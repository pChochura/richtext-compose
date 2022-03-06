package com.pointlessapps.rt_editor.model

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color

interface Style {

	object ClearFormat : Style

	interface TextStyle : Style
	interface ParagraphStyle : Style

	object OrderedList : ParagraphStyle
	object UnorderedList : ParagraphStyle
	object AlignLeft : ParagraphStyle
	object AlignCenter : ParagraphStyle
	object AlignRight : ParagraphStyle

	object Bold : TextStyle
	object Underline : TextStyle
	object Italic : TextStyle
	object Strikethrough : TextStyle

	class TextColor(val color: Color? = null) : TextStyle

	class TextSize(
		@FloatRange(from = MIN_VALUE, to = MAX_VALUE)
		fraction: Float = DEFAULT_VALUE
	) : TextStyle {

		var fraction: Float? = fraction.coerceIn(
			minimumValue = MIN_VALUE.toFloat(),
			maximumValue = MAX_VALUE.toFloat()
		)

		companion object {
			const val DEFAULT_VALUE = 1f
			const val MIN_VALUE = 0.5
			const val MAX_VALUE = 2.0
		}
	}
}
