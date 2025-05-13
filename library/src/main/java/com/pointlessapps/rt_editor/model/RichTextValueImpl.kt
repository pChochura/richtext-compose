package com.pointlessapps.rt_editor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pointlessapps.rt_editor.utils.*
import kotlin.math.max
import kotlin.math.min

internal typealias StyleRange<T> = AnnotatedStringBuilder.MutableRange<T>

internal class RichTextValueImpl(private val styleMapper: Map<String, (String) -> Style>) : RichTextValue() {

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
            .map { it.tag.toStyle(styleMapper) }.toSet() +
                filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
                    .map { it.tag.toStyle(styleMapper) }.toSet()

    private fun clearRedoStack() {
        // If offset in the history is not 0 clear possible "redo" states
        repeat(historyOffset) {
            historySnapshots.removeLastOrNull()
        }
        historyOffset = 0
    }

    private fun updateHistoryIfNecessary() {
        currentSnapshot?.run {
            // Add a snapshot manually when not enough text was changed to be saved
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
        currentSnapshot?.let(::restoreFromSnapshot)
    }

    fun restoreFromSnapshot(snapshot: RichTextValueSnapshot) {
        annotatedStringBuilder.update(snapshot.toAnnotatedStringBuilder(styleMapper))
        selection = TextRange(snapshot.selectionPosition)
        composition = null
    }

    private fun <T> filterCurrentStyles(styles: List<StyleRange<T>>) = styles.filter {
        !currentSelection.collapsed && currentSelection.intersects(TextRange(it.start, it.end))
    }

    private fun getCurrentSpanStyles(style: Style?) =
        filterCurrentStyles(annotatedStringBuilder.spanStyles)
            .filter { style == null || it.tag == style.tag }

    private fun getCurrentParagraphStyles(style: Style?) =
        filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
            .filter { style == null || it.tag == style.tag }

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
        if (currentSelection.collapsed && style == Style.ClearFormat) {
            updateHistoryIfNecessary()
            annotatedStringBuilder.splitStyles(currentSelection.end)
            updateHistory()

            return this
        }

        val (spansToAdd, spansToRemove) = removeStyleFromSelection(
            getCurrentSpanStyles(style.takeUnless { it == Style.ClearFormat })
        )
        val (_, paragraphsToRemove) = removeStyleFromSelection(
            getCurrentParagraphStyles(style.takeUnless {
                it == Style.ClearFormat || it is Style.ParagraphStyle
            }), // Always remove all paragraphs; they cannot overlap
            currentSelection.coerceParagraph(annotatedStringBuilder.text) // Select whole paragraph
        )

        val changedStyles = spansToAdd.isNotEmpty() || spansToRemove.isNotEmpty() ||
                paragraphsToRemove.isNotEmpty()

        if (changedStyles) {
            updateHistoryIfNecessary()

            annotatedStringBuilder.addSpans(spansToAdd)
            annotatedStringBuilder.removeSpans(spansToRemove)

            if (paragraphsToRemove.isNotEmpty()) {
                annotatedStringBuilder.removeParagraphs(paragraphsToRemove)
            }

            updateHistory()

            if (paragraphsToRemove.isEmpty() || paragraphsToRemove.any { it.tag == style.tag }) {
                return this
            }
        } else if (style == Style.ClearFormat || (composition == null && selection.collapsed)) {
            return this
        }

        updateHistoryIfNecessary()

        val spanStyle = style.spanStyle?.let {
            StyleRange(
                item = it,
                start = currentSelection.start,
                end = currentSelection.end,
                tag = style.tag
            )
        }

        val paragraphStyle = style.paragraphStyle?.let {
            var startOfTheParagraph = currentSelection.start.coerceStartOfParagraph(annotatedStringBuilder.text)
            var endOfTheParagraph = currentSelection.end.coerceEndOfParagraph(annotatedStringBuilder.text)

            val removedParagraph = paragraphsToRemove.singleOrNull()
            if (removedParagraph != null) {
                startOfTheParagraph = min(removedParagraph.start, startOfTheParagraph)
                endOfTheParagraph = max(removedParagraph.end, endOfTheParagraph)
            }

            StyleRange(
                item = it,
                start = startOfTheParagraph,
                end = endOfTheParagraph,
                tag = style.tag,
            )
        }

        spanStyle?.let { annotatedStringBuilder.addSpan(it) }
        paragraphStyle?.let { annotatedStringBuilder.addParagraph(it) }

        updateHistory()

        return this
    }

    override fun insertStyle(style: Style) = this.copy().insertStyleInternal(style)

    private fun clearStylesInternal(vararg styles: Style): RichTextValue {
        val tags = styles.map { it.tag }.toSet()
        val spanStylesByType = filterCurrentStyles(annotatedStringBuilder.spanStyles)
            .filter { it.tag.startsWith(tags) }
        val paragraphStylesByType = filterCurrentStyles(annotatedStringBuilder.paragraphStyles)
            .filter { it.tag.startsWith(tags) }

        annotatedStringBuilder.removeSpans(spanStylesByType)
        annotatedStringBuilder.removeParagraphs(paragraphStylesByType)

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
        updateHistoryIfNecessary()
        return currentSnapshot ?: run {
            updateHistory()
            requireNotNull(currentSnapshot)
        }
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
