package com.pointlessapps.rt_editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pointlessapps.rt_editor.model.RichTextValue

@Composable
fun RichTextEditor(
    value: RichTextValue,
    modifier: Modifier = Modifier,
    textFieldStyle: RichTextFieldStyle = defaultRichTextFieldStyle(),
    onValueChange: (RichTextValue) -> Unit,
) {
    RichTextField(
        value = value.value,
        styledValue = value.styledValue,
        modifier = modifier,
        textFieldStyle = textFieldStyle
    ) {
        val newValue = value.copy()
        if (newValue.updatedValueAndStyles(it)) {
            onValueChange(newValue)
        }
    }
}
