package com.pointlessapps.rt_editor.utils

import com.pointlessapps.rt_editor.model.Style
import com.pointlessapps.rt_editor.model.paragraphStyle
import com.pointlessapps.rt_editor.model.spanStyle
import com.pointlessapps.rt_editor.model.toStyle
import kotlinx.serialization.Serializable

/**
 * A helper class that lets you serialize the [RichTextValue]
 */
@Serializable
data class RichTextValueSnapshot(
    val text: String = "",
    val spanStyles: List<RichTextValueSpanSnapshot> = emptyList(),
    val paragraphStyles: List<RichTextValueSpanSnapshot> = emptyList(),
    val selectionPosition: Int = -1,
) {

    internal fun toAnnotatedStringBuilder(styleMapper: Map<String, (String) -> Style>): AnnotatedStringBuilder {
        val spans = this.spanStyles.mapNotNull {
            val item = it.tag.toStyle(styleMapper).spanStyle ?: return@mapNotNull null
            AnnotatedStringBuilder.MutableRange(item, it.start, it.end, it.tag)
        }

        val paragraphs = this.paragraphStyles.mapNotNull {
            val item = it.tag.toStyle(styleMapper).paragraphStyle ?: return@mapNotNull null
            AnnotatedStringBuilder.MutableRange(item, it.start, it.end, it.tag)
        }

        return AnnotatedStringBuilder().apply {
            text = this@RichTextValueSnapshot.text
            addSpans(spans)
            addParagraphs(paragraphs)
        }
    }

    @Serializable
    data class RichTextValueSpanSnapshot(
        val start: Int,
        val end: Int,
        val tag: String,
    )

    companion object {
        internal fun fromAnnotatedStringBuilder(
            annotatedStringBuilder: AnnotatedStringBuilder,
            selectionPosition: Int,
        ) = RichTextValueSnapshot(
            text = annotatedStringBuilder.text,
            spanStyles = annotatedStringBuilder.spanStyles.map {
                it.toRichTextValueSpanSnapshot()
            },
            paragraphStyles = annotatedStringBuilder.paragraphStyles.map {
                it.toRichTextValueSpanSnapshot()
            },
            selectionPosition = selectionPosition
        )
    }
}

private fun <T> AnnotatedStringBuilder.MutableRange<T>.toRichTextValueSpanSnapshot() =
    RichTextValueSnapshot.RichTextValueSpanSnapshot(start = start, end = end, tag = tag)
