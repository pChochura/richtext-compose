package com.pointlessapps.rt_editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

interface Style {

    val tag: String

    object ClearFormat : Style {
        override val tag = "ClearFormat"
    }

    interface TextStyle : Style {
        val spanStyle: SpanStyle
    }

    interface ParagraphStyle : Style {
        val paragraphStyle: androidx.compose.ui.text.ParagraphStyle
    }

    object OrderedList : ParagraphStyle {
        override val paragraphStyle: androidx.compose.ui.text.ParagraphStyle get() = TODO("Not yet implemented")
        override val tag = "OrderedList"
    }

    object UnorderedList : ParagraphStyle {
        override val paragraphStyle: androidx.compose.ui.text.ParagraphStyle get() = TODO("Not yet implemented")
        override val tag = "UnorderedList"
    }

    object AlignLeft : ParagraphStyle {
        override val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Left)
        override val tag = "AlignLeft"
    }

    object AlignCenter : ParagraphStyle {
        override val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        override val tag = "AlignCenter"
    }

    object AlignRight : ParagraphStyle {
        override val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Right)
        override val tag = "AlignRight"
    }

    object Bold : TextStyle {
        override val spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
        override val tag = "Bold"
    }

    object Underline : TextStyle {
        override val spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
        override val tag = "Underline"
    }

    object Italic : TextStyle {
        override val spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
        override val tag = "Italic"
    }

    object Strikethrough : TextStyle {
        override val spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
        override val tag = "Strikethrough"
    }

    class TextColor(color: Color) : TextStyle {
        override val spanStyle = SpanStyle(color = color)
        override val tag = "$TAG/$color"

        companion object {
            const val TAG = "TextColor"
        }
    }

    class TextSize(fraction: Float = DEFAULT_VALUE) : TextStyle {
        @OptIn(ExperimentalUnitApi::class)
        override val spanStyle = SpanStyle(fontSize = TextUnit(fraction, TextUnitType.Em))
        override val tag = "$TAG/$fraction"

        var fraction: Float? = fraction.coerceIn(
            minimumValue = MIN_VALUE.toFloat(),
            maximumValue = MAX_VALUE.toFloat()
        )

        companion object {
            const val DEFAULT_VALUE = 1f
            const val MIN_VALUE = 0.5
            const val MAX_VALUE = 2.0
            const val TAG = "TextSize"
        }
    }

    companion object {
        val DEFAULT_MAPPER: Map<String, (String) -> Style> = mapOf(
            ClearFormat.tag to { ClearFormat },
            OrderedList.tag to { OrderedList },
            UnorderedList.tag to { UnorderedList },
            AlignLeft.tag to { AlignLeft },
            AlignCenter.tag to { AlignCenter },
            AlignRight.tag to { AlignRight },
            Bold.tag to { Bold },
            Underline.tag to { Underline },
            Italic.tag to { Italic },
            Strikethrough.tag to { Strikethrough },
            TextColor.TAG to { TextColor(Color(it.toLongOrNull() ?: 0L)) },
            TextSize.TAG to { TextSize(it.toFloatOrNull() ?: 1f) },
        )
    }
}

val Style.spanStyle: SpanStyle? get() = (this as? Style.TextStyle)?.spanStyle
val Style.paragraphStyle: ParagraphStyle? get() = (this as? Style.ParagraphStyle)?.paragraphStyle

fun String.toStyle(styleMapper: Map<String, (String) -> Style>): Style {
    val items = split('/')
    val name = items.getOrNull(0)
    val arg = items.getOrNull(1) ?: ""
    val builder = styleMapper[name] ?: throw IllegalArgumentException("Unrecognized tag: $this")
    return builder(arg)
}
