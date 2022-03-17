package com.pointlessapps.rt_editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pointlessapps.rt_editor.model.RichTextValue

@Composable
fun RichTextEditor(
	value: RichTextValue,
	onValueChange: (RichTextValue) -> Unit,
	modifier: Modifier = Modifier,
	textFieldStyle: RichTextFieldStyle = defaultRichTextFieldStyle()
) {
	RichTextField(
		modifier = modifier,
		value = value.value,
		styledValue = value.styledValue,
		styleMapper = value.styleMapper,
		onValueChange = {
			val newValue = value.copy()
			if (newValue.updatedValueAndStyles(it)) {
				onValueChange(newValue)
			}
		},
		textFieldStyle = textFieldStyle
	)
}
