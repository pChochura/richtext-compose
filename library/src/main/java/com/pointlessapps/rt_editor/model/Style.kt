package com.pointlessapps.rt_editor.model

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color

interface Style {

	object ClearFormat : Style

	object OrderedList : Style
	object UnorderedList : Style
	object AlignLeft : Style
	object AlignCenter : Style
	object AlignRight : Style

	object Bold : Style
	object Underline : Style
	object Italic : Style
	object Strikethrough : Style

	class TextColor(val color: Color? = null) : Style

	class TextSize(
		@FloatRange(from = MIN_VALUE, to = MAX_VALUE)
		fraction: Float = DEFAULT_VALUE
	) : Style {

		@FloatRange(from = MIN_VALUE, to = MAX_VALUE)
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
