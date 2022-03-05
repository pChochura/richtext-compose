package com.pointlessapps.rt_editor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange

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

	fun addSpans(vararg spanStyle: MutableRange<SpanStyle>) {
		_spanStyles.addAll(spanStyle)
		collapseStyles(_spanStyles)
	}

	fun addParagraphs(vararg paragraphStyle: MutableRange<ParagraphStyle>) {
		_paragraphStyles.addAll(paragraphStyle)
		collapseStyles(_paragraphStyles)
	}

	fun removeSpans(vararg spanStyle: MutableRange<SpanStyle>) {
		_spanStyles.removeAll {
			spanStyle.any { span -> span.equalsStructurally(it) }
		}
	}

	fun removeParagraphs(vararg paragraphStyle: MutableRange<ParagraphStyle>) {
		_paragraphStyles.removeAll {
			paragraphStyle.any { paragraph -> paragraph.equalsStructurally(it) }
		}
	}

	fun updateStyles(previousSelection: TextRange, currentValue: String): Boolean {
		val lengthDifference = currentValue.length - text.length
		if (lengthDifference == 0) {
			// Text was not changed at all; leave styles untouched
			return false
		}

		return updateStyles(_spanStyles, previousSelection, lengthDifference) ||
				updateStyles(_paragraphStyles, previousSelection, lengthDifference)
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
			val updateStart = style.start > prevEnd
			val updateEnd = style.end >= prevEnd

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
				style.start -= style.start - prevStart
				style.end -= prevEnd - prevStart

				updated = true
			} else if (prevStart >= style.start && prevStart < style.end && prevEnd > style.end) {
				// Example: some *Lo|ng* te|xt
				style.end = prevStart

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
			if (startRangeMap.containsKey(range.end)) {
				val otherRangeIndex = requireNotNull(startRangeMap[range.end])
				if (styles[otherRangeIndex].tag == range.tag) {
					end = styles[otherRangeIndex].end

					// Remove collapsed values
					startRangeMap.remove(range.end)
					endRangeMap.remove(range.end)
					removedIndexes.add(otherRangeIndex)
				}
			}

			if (endRangeMap.containsKey(range.start)) {
				val otherRangeIndex = requireNotNull(endRangeMap[range.start])
				if (styles[otherRangeIndex].tag == range.tag) {
					start = styles[otherRangeIndex].start

					// Remove collapsed values
					startRangeMap.remove(range.start)
					endRangeMap.remove(range.start)
					removedIndexes.add(otherRangeIndex)
				}
			}

			range.start = start
			range.end = end
		}

		removedIndexes.reversed().forEach { styles.removeAt(it) }
	}

	fun toAnnotatedString() = AnnotatedString(
		text = text,
		spanStyles = spanStyles.map { it.toRange() },
		paragraphStyles = paragraphStyles.map { it.toRange() }
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
		fun toRange() = Range(item = item, start = start, end = end, tag = tag)

		fun equalsStructurally(other: MutableRange<T>) =
			item == other.item && tag == other.tag &&
					start == other.start && end == other.end
	}
}
