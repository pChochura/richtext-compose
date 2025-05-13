package com.pointlessapps.rt_editor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import kotlin.math.max
import kotlin.math.min

internal class AnnotatedStringBuilder {

    private val _text = StringBuilder()
    internal var text: String
        get() = _text.toString()
        set(value) {
            _text.clear()
            _text.append(value)
        }

    private val _spanStyles: MutableList<MutableRange<SpanStyle>> = mutableListOf()
    internal val spanStyles: List<MutableRange<SpanStyle>>
        get() = _spanStyles

    private val _paragraphStyles: MutableList<MutableRange<ParagraphStyle>> = mutableListOf()
    internal val paragraphStyles: List<MutableRange<ParagraphStyle>>
        get() = _paragraphStyles

    fun addSpan(spanStyle: MutableRange<SpanStyle>) {
        _spanStyles.add(spanStyle)
        collapseStyles(_spanStyles)
    }

    fun addSpans(spanStyles: List<MutableRange<SpanStyle>>) {
        _spanStyles.addAll(spanStyles)
        collapseStyles(_spanStyles)
    }

    fun addParagraph(paragraphStyle: MutableRange<ParagraphStyle>) {
        _paragraphStyles.add(paragraphStyle)
    }

    fun addParagraphs(paragraphStyles: List<MutableRange<ParagraphStyle>>) {
        _paragraphStyles.addAll(paragraphStyles)
    }

    fun removeSpans(spanStyles: List<MutableRange<SpanStyle>>) {
        _spanStyles.removeAll {
            spanStyles.any { span -> span.equalsStructurally(it) }
        }
    }

    fun removeParagraphs(paragraphStyles: List<MutableRange<ParagraphStyle>>) {
        _paragraphStyles.removeAll {
            paragraphStyles.any { paragraph -> paragraph.equalsStructurally(it) }
        }
    }

    fun updateStyles(
        previousSelection: TextRange,
        currentValue: String,
        onCollapsedParagraphsCallback: () -> Unit,
        onEscapeParagraphCallback: (String) -> Unit
    ): Boolean {
        val lengthDifference = currentValue.length - text.length
        if (lengthDifference == 0) {
            // Text was not changed at all; leave styles untouched
            return false
        }

        val updatedSpans = updateStyles(_spanStyles, previousSelection, lengthDifference)

        val currentStartingParagraph = _paragraphStyles.find { it.start == previousSelection.end }
        val currentEndingParagraph = _paragraphStyles.find { it.end == previousSelection.end }
        val previousEndingParagraph = _paragraphStyles.find { it.end == previousSelection.end }

        // A user deleted a newline character and tried to merge two paragraphs
        val updatedParagraphs = when {
            previousSelection.collapsed && lengthDifference == -1 &&
                    currentStartingParagraph != null && previousEndingParagraph != null -> {
                // Collapse current paragraph
                _paragraphStyles.remove(currentStartingParagraph)
                previousEndingParagraph.end = currentStartingParagraph.end
                onCollapsedParagraphsCallback()

                true
            }
            previousSelection.collapsed && lengthDifference == 1 && currentEndingParagraph != null &&
                    currentValue.substring(
                        currentEndingParagraph.start,
                        currentEndingParagraph.end + 1
                    ).endsWith(lineSeparator().repeat(2)) -> {
                currentEndingParagraph.end -= 1

                onEscapeParagraphCallback(
                    currentValue.substring(0, currentEndingParagraph.start) +
                            currentValue.substring(
                                currentEndingParagraph.start,
                                currentEndingParagraph.end + 1
                            ) + currentValue.substring(
                        currentEndingParagraph.end + 2
                    )
                )

                true
            }
            else -> updateStyles(_paragraphStyles, previousSelection, lengthDifference)
        }

        return updatedSpans || updatedParagraphs
    }

    private fun <T> updateStyles(
        styles: MutableList<MutableRange<T>>,
        previousSelection: TextRange,
        lengthDifference: Int
    ): Boolean {
        val removedIndexes = mutableSetOf<Int>()

        var updated = false
        val prevStart = previousSelection.min
        val prevEnd = previousSelection.max
        styles.forEachIndexed { index, style ->
            val updateStart = prevEnd <= style.start
            val updateEnd = prevEnd <= style.end

            if (previousSelection.collapsed && (updateStart || updateEnd)) {
                if (updateStart) style.start += lengthDifference
                if (updateEnd) style.end += lengthDifference

                updated = true
            } else if (prevStart <= style.start && prevEnd >= style.end) {
                // Example: som|e *Long* t|ext
                style.start = 0
                style.end = 0

                updated = true
            } else if (prevStart < style.start && prevEnd <= style.end && prevEnd > style.start) {
                // Example: som|e *Tex|t*
                style.start = max(0, style.start - (style.start - prevStart))
                style.end = min(_text.length, style.end - (prevEnd - prevStart))

                updated = true
            } else if (prevStart >= style.start && prevStart < style.end && prevEnd > style.end) {
                // Example: some *Lo|ng* te|xt
                style.end = prevStart

                updated = true
            } else if (prevStart < style.start && prevEnd <= style.start) {
                // Example: |som|e *Long* text
                style.start = max(0, style.start - (prevEnd - prevStart))
                style.end = min(_text.length, style.end - (prevEnd - prevStart))

                updated = true
            }

            if (style.end <= style.start) {
                removedIndexes.add(index)
            }
        }

        removedIndexes.reversed().forEach { styles.removeAt(it) }

        return updated
    }

    private fun <T> collapseStyles(styles: MutableList<MutableRange<T>>) {
        val startRangeMap = mutableMapOf<Int, Int>()
        val endRangeMap = mutableMapOf<Int, Int>()
        val removedIndexes = mutableSetOf<Int>()

        styles.forEachIndexed { index, range ->
            startRangeMap[range.start] = index
            endRangeMap[range.end] = index
        }

        styles.forEachIndexed { index, range ->
            if (removedIndexes.contains(index)) {
                return@forEachIndexed
            }

            var start = range.start
            var end = range.end

            if (end <= start) {
                removedIndexes.add(index)
                return@forEachIndexed
            }

            startRangeMap[range.end]?.let { otherRangeIndex ->
                if (styles[otherRangeIndex].tag == range.tag) {
                    end = styles[otherRangeIndex].end

                    // Remove collapsed values
                    startRangeMap.remove(range.end)
                    endRangeMap.remove(range.end)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            endRangeMap[range.start]?.let { otherRangeIndex ->
                if (styles[otherRangeIndex].tag == range.tag) {
                    start = styles[otherRangeIndex].start

                    // Remove collapsed values
                    startRangeMap.remove(range.start)
                    endRangeMap.remove(range.start)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            range.start = max(0, start)
            range.end = min(_text.length, end)
        }

        removedIndexes.reversed().forEach { styles.removeAt(it) }
    }

    fun splitStyles(position: Int) {
        val spansToAdd = mutableListOf<MutableRange<SpanStyle>>()
        _spanStyles.forEach {
            if (position in it.start..it.end) {
                spansToAdd.add(it.copy(start = position))
                it.end = position - 1
            }
        }
        _spanStyles.addAll(spansToAdd)
        collapseStyles(_spanStyles)

        val paragraphsToAdd = mutableListOf<MutableRange<ParagraphStyle>>()
        _paragraphStyles.forEach {
            if (position in it.start..it.end) {
                paragraphsToAdd.add(it.copy(start = position))
                it.end = position - 1
            }
        }
        _paragraphStyles.addAll(paragraphsToAdd)
        collapseStyles(_paragraphStyles)
    }

    fun toAnnotatedString() = AnnotatedString(
        text = text,
        spanStyles = spanStyles.map { it.toRange() },
        paragraphStyles = paragraphStyles.map { it.toRange() }.sortedBy { it.end }
    )

    fun update(annotatedStringBuilder: AnnotatedStringBuilder) {
        text = annotatedStringBuilder.text
        _spanStyles.clear()
        _spanStyles.addAll(annotatedStringBuilder._spanStyles)
        _paragraphStyles.clear()
        _paragraphStyles.addAll(annotatedStringBuilder._paragraphStyles)
    }

    internal data class MutableRange<T>(
        val item: T,
        var start: Int,
        var end: Int,
        val tag: String
    ) {
        fun toRange() = AnnotatedString.Range(item = item, start = start, end = end, tag = tag)

        fun equalsStructurally(other: MutableRange<T>) =
            item == other.item && tag == other.tag &&
                    start == other.start && end == other.end

        companion object {
            fun <T> fromRange(range: AnnotatedString.Range<T>) = MutableRange(
                item = range.item,
                start = range.start,
                end = range.end,
                tag = range.tag
            )
        }
    }
}
