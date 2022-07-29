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
    return when (val previousNewLineCharacterIndex = text.substring(0, this).lastIndexOf(lineSeparator())) {
        -1 -> 0
        else -> previousNewLineCharacterIndex + lineSeparator().length
    }
}

internal fun Int.coerceEndOfParagraph(text: String): Int {
    return when (val nextNewLineCharacterIndex = text.substring(this).indexOf(lineSeparator())) {
        -1 -> text.length
        else -> this + nextNewLineCharacterIndex + 1
    }
}

internal fun TextRange.coerceParagraph(text: String): TextRange = TextRange(
    start = start.coerceStartOfParagraph(text),
    end = end.coerceEndOfParagraph(text)
)

internal fun TextRange.coerceNotReversed(): TextRange = if (start <= end) {
    this
} else {
    TextRange(end, start)
}

internal fun String.startsWith(prefixes: Iterable<String>): Boolean =
    prefixes.any { this.startsWith(it) }
