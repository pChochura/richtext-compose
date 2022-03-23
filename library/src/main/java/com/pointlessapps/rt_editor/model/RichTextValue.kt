package com.pointlessapps.rt_editor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pointlessapps.rt_editor.mappers.StyleMapper
import com.pointlessapps.rt_editor.model.Style.ClearFormat
import com.pointlessapps.rt_editor.utils.*
import kotlin.math.max
import kotlin.math.min

internal typealias StyleRange<T> = AnnotatedStringBuilder.MutableRange<T>

abstract class RichTextValue {

    /**
     * Returns styles that are used inside the current selection (or composition)
     */
    abstract val currentStyles: Set<Style>
    abstract val isUndoAvailable: Boolean
    abstract val isRedoAvailable: Boolean
    abstract val value: TextFieldValue
    internal abstract val styledValue: AnnotatedString
    internal abstract val styleMapper: StyleMapper

    abstract fun insertStyle(style: Style): RichTextValue
    abstract fun clearStyles(vararg styles: Style): RichTextValue

    abstract fun undo(): RichTextValue
    abstract fun redo(): RichTextValue

    /**
     * Retrieves the last snapshot of the RichTextValue
     */
    abstract fun getLastSnapshot(): RichTextValueSnapshot

    internal abstract fun updatedValueAndStyles(value: TextFieldValue): Boolean
    internal abstract fun copy(): RichTextValue

    override fun toString() = this.value.toString()

    companion object {
        // Indicates minimum length difference to add a new snapshot to the history stack
        internal const val MIN_LENGTH_DIFFERENCE = 10

        fun get(styleMapper: StyleMapper = StyleMapper()): RichTextValue =
            RichTextValueImpl(styleMapper)
    }
}

internal class RichTextValueImpl(override val styleMapper: StyleMapper) : RichTextValue() {

    private val annotatedStringBuilder = AnnotatedStringBuilder()
    private var selection: TextRange = TextRange.Zero
    private var composition: TextRange? = null

    private var historyOffset: Int = 0
    private val historySnapshots = mutableListOf(
        RichTextValueSnapshot.fromAnnotatedStringBuilder(
            annotatedStringBuilder,
            selectionPosition = selection.start
        )
    )
    private val currentSnapshot: RichTextValueSnapshot?
        get() = historySnapshots.elementAtOrNull(
            historySnapshots.lastIndex - historyOffset
        )

    override val isUndoAvailable: Boolean
        get() = historySnapshots.isNotEmpty() && historyOffset < historySnapshots.lastIndex

    override val isRedoAvailable: Boolean
        get() = historyOffset > 0

    override val value: TextFieldValue
        get() = TextFieldValue(
            annotatedString = AnnotatedString(annotatedStringBuilder.text),
            selection = selection,
            composition = composition
        )

    override val styledValue: AnnotatedString
        get() = annotatedStringBuilder.toAnnotatedString()

    private val currentSelection: TextRange
        get() = (composition ?: selection).coerceNotReversed()

    override val currentStyles: Set<Style>
        get() = filterCurrentStyles(annotatedStringBuilder.spanStyles)
            .map { styleMapper.fromTag(it.tag) }.toSet() +
                filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
                    .map { styleMapper.fromTag(it.tag) }.toSet()

    private fun clearRedoStack() {
        // If offset in the history is not 0 clear possible "redo" states
        repeat(historyOffset) {
            historySnapshots.removeLastOrNull()
        }
        historyOffset = 0
    }

    private fun updateHistoryIfNecessary() {
        currentSnapshot?.run {
            // Add a snapshot when the style is added, but not enough text was changed to be saved
            if (text != annotatedStringBuilder.text) {
                updateHistory()
            }
        }
    }

    private fun updateHistory() {
        clearRedoStack()

        historySnapshots.add(
            RichTextValueSnapshot.fromAnnotatedStringBuilder(
                annotatedStringBuilder = annotatedStringBuilder,
                selectionPosition = selection.start
            )
        )
    }

    private fun restoreFromHistory() {
        currentSnapshot?.run {
            annotatedStringBuilder.update(toAnnotatedStringBuilder(styleMapper))
            selection = TextRange(selectionPosition)
            composition = null
        }
    }

    private fun <T> filterCurrentStyles(styles: List<StyleRange<T>>) = styles.filter {
        !currentSelection.collapsed && currentSelection.intersects(TextRange(it.start, it.end))
    }

    private fun getCurrentSpanStyles(style: Style?) =
        filterCurrentStyles(annotatedStringBuilder.spanStyles)
            .filter { style == null || it.tag == styleMapper.toTag(style) }

    private fun getCurrentParagraphStyles(style: Style?) =
        filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
            .filter { style == null || it.tag == styleMapper.toTag(style) }

    private fun <T> removeStyleFromSelection(
        styles: List<StyleRange<T>>,
        selection: TextRange = currentSelection,
    ): Pair<List<StyleRange<T>>, List<StyleRange<T>>> {
        if (styles.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        val stylesToAdd = mutableListOf<StyleRange<T>>()
        val stylesToRemove = mutableListOf<StyleRange<T>>()
        val start = selection.start
        val end = selection.end
        styles.forEach {
            if (it.start <= start && it.end >= end) {
                // Split into two styles
                stylesToRemove.add(it)

                val styleBeforeSelection = it.copy(end = start)
                val styleAfterSelection = it.copy(start = end)
                if (styleBeforeSelection.start < styleBeforeSelection.end) {
                    stylesToAdd.add(it.copy(end = start))
                }
                if (styleAfterSelection.start < styleAfterSelection.end) {
                    stylesToAdd.add(it.copy(start = end))
                }

                return@forEach
            }

            if (it.start >= start && it.end <= end) {
                // Remove this style completely
                stylesToRemove.add(it)

                return@forEach
            }

            if (it.start >= start) {
                // Move style before the selection
                stylesToRemove.add(it)
                stylesToAdd.add(it.copy(start = end))

                return@forEach
            }

            if (it.end <= end) {
                // Move style after the selection
                stylesToRemove.add(it)
                stylesToAdd.add(it.copy(end = start))

                return@forEach
            }
        }

        return stylesToAdd to stylesToRemove
    }

    private fun insertStyleInternal(style: Style): RichTextValue {
        if (currentSelection.collapsed && style == ClearFormat) {
            updateHistoryIfNecessary()
            annotatedStringBuilder.splitStyles(currentSelection.end)
            updateHistory()

            return this
        }

        val (spansToAdd, spansToRemove) = removeStyleFromSelection(
            getCurrentSpanStyles(style.takeUnless { it == ClearFormat })
        )
        val (_, paragraphsToRemove) = removeStyleFromSelection(
            getCurrentParagraphStyles(style.takeUnless {
                it == ClearFormat || it is Style.ParagraphStyle
            }), // Always remove all paragraphs; they cannot overlap
            currentSelection.coerceParagraph(annotatedStringBuilder.text) // Select whole paragraph
        )

        val changedStyles = spansToAdd.isNotEmpty() || spansToRemove.isNotEmpty() ||
                paragraphsToRemove.isNotEmpty()

        if (changedStyles) {
            updateHistoryIfNecessary()

            annotatedStringBuilder.addSpans(*spansToAdd.toTypedArray())
            annotatedStringBuilder.removeSpans(*spansToRemove.toTypedArray())

            if (paragraphsToRemove.isNotEmpty()) {
                annotatedStringBuilder.removeParagraphs(*paragraphsToRemove.toTypedArray())
            }

            updateHistory()

            if (paragraphsToRemove.isEmpty() || paragraphsToRemove.any {
                    it.tag == styleMapper.toTag(style)
                }
            ) {
                return this
            }
        } else if (style == ClearFormat || (composition == null && selection.collapsed)) {
            return this
        }

        updateHistoryIfNecessary()

        val spanStyle = styleMapper.toSpanStyle(style)?.let {
            StyleRange(
                item = it,
                start = currentSelection.start,
                end = currentSelection.end,
                tag = styleMapper.toTag(style)
            )
        }

        val paragraphStyle = styleMapper.toParagraphStyle(style)?.let {
            var startOfTheParagraph = currentSelection.start
                .coerceStartOfParagraph(annotatedStringBuilder.text)
            var endOfTheParagraph = currentSelection.end
                .coerceEndOfParagraph(annotatedStringBuilder.text)

            val removedParagraph = paragraphsToRemove.singleOrNull()
            if (removedParagraph != null) {
                startOfTheParagraph = min(removedParagraph.start, startOfTheParagraph)
                endOfTheParagraph = max(removedParagraph.end, endOfTheParagraph)
            }

            StyleRange(
                item = it,
                start = startOfTheParagraph,
                end = endOfTheParagraph,
                tag = styleMapper.toTag(style)
            )
        }

        spanStyle?.let { annotatedStringBuilder.addSpans(it) }
        paragraphStyle?.let { annotatedStringBuilder.addParagraphs(it) }

        updateHistory()

        return this
    }

    override fun insertStyle(style: Style) = this.copy().insertStyleInternal(style)

    private fun clearStylesInternal(vararg styles: Style): RichTextValue {
        val tags = styles.map { styleMapper.toTag(it, simple = true) }.toSet()
        val spanStylesByType = filterCurrentStyles(annotatedStringBuilder.spanStyles)
            .filter { it.tag.startsWith(tags) }
        val paragraphStylesByType = filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
            .filter { it.tag.startsWith(tags) }

        annotatedStringBuilder.removeSpans(*spanStylesByType.toTypedArray())
        annotatedStringBuilder.removeParagraphs(*paragraphStylesByType.toTypedArray())

        return this
    }

    override fun clearStyles(vararg styles: Style) = this.copy().clearStylesInternal(*styles)

    private fun undoInternal(): RichTextValue {
        updateHistoryIfNecessary()
        historyOffset += 1
        restoreFromHistory()

        return this
    }

    override fun undo() = this.copy().undoInternal()

    private fun redoInternal(): RichTextValue {
        historyOffset -= 1
        restoreFromHistory()

        return this
    }

    override fun redo() = this.copy().redoInternal()

    override fun updatedValueAndStyles(value: TextFieldValue): Boolean {
        var updateText = true
        val updatedStyles = annotatedStringBuilder.updateStyles(
            previousSelection = selection,
            currentValue = value.text,
            onCollapsedParagraphsCallback = { updateText = false },
            onEscapeParagraphCallback = {
                updateText = false
                annotatedStringBuilder.text = it
            }
        )

        if (updatedStyles || annotatedStringBuilder.text != value.text ||
            selection != value.selection || composition != value.composition
        ) {
            if (updateText) {
                annotatedStringBuilder.text = value.text
                selection = value.selection
                composition = value.composition
            }

            currentSnapshot?.run {
                if (value.text.length - text.length >= MIN_LENGTH_DIFFERENCE) {
                    updateHistory()
                } else if (value.text != text) {
                    clearRedoStack()
                }
            }

            return true
        }

        return false
    }

    override fun getLastSnapshot(): RichTextValueSnapshot {
        val snapshot = currentSnapshot
        if (snapshot != null) {
            return snapshot
        }

        updateHistory()
        return requireNotNull(currentSnapshot)
    }

    override fun copy() = RichTextValueImpl(styleMapper).apply {
        annotatedStringBuilder.update(this@RichTextValueImpl.annotatedStringBuilder)
        selection = this@RichTextValueImpl.selection
        composition = this@RichTextValueImpl.composition
        historyOffset = this@RichTextValueImpl.historyOffset
        historySnapshots.clear()
        historySnapshots.addAll(this@RichTextValueImpl.historySnapshots)
    }
}
