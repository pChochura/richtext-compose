package com.pointlessapps.rt_editor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange

internal fun AnnotatedString.copy(
	text: String = this.text,
	spanStyles: List<AnnotatedString.Range<SpanStyle>> = this.spanStyles,
	paragraphStyles: List<AnnotatedString.Range<ParagraphStyle>> = this.paragraphStyles
) = AnnotatedString(
	text = text,
	spanStyles = spanStyles,
	paragraphStyles = paragraphStyles
)

internal fun Int.coerceStartOfParagraph(text: String): Int {
	val previousNewLineCharacterIndex = text.substring(0, this)
		.lastIndexOf(System.lineSeparator())

	if (previousNewLineCharacterIndex == -1) {
		return 0
	}

	return previousNewLineCharacterIndex + System.lineSeparator().length
}

internal fun Int.coerceEndOfParagraph(text: String): Int {
	val nextNewLineCharacterIndex = text.substring(this)
		.indexOf(System.lineSeparator())

	if (nextNewLineCharacterIndex == -1) {
		return text.lastIndex
	}

	return this + nextNewLineCharacterIndex - System.lineSeparator().length + 1
}

internal fun TextRange.coerceNotReversed() = if (start < end) {
	this
} else {
	TextRange(end, start)
}

internal fun String.startsWith(prefixes: Set<String>) =
	prefixes.any { this.startsWith(it) }
