package com.pointlessapps.rt_editor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.pointlessapps.rt_editor.mappers.StyleMapper
import com.pointlessapps.rt_editor.utils.RichTextValueSnapshot

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

        fun fromSnapshot(
            snapshot: RichTextValueSnapshot,
            styleMapper: StyleMapper = StyleMapper(),
        ): RichTextValue = RichTextValueImpl(styleMapper).apply {
            restoreFromSnapshot(snapshot)
        }
    }
}
