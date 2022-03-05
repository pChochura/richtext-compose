package com.pointlessapps.rt_editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText

private const val EMPTY_STRING = ""

@Composable
internal fun RichTextField(
	value: TextFieldValue,
	styledValue: AnnotatedString,
	onValueChange: (TextFieldValue) -> Unit,
	modifier: Modifier = Modifier,
	textFieldModel: RTTextFieldModel = defaultRTTextFieldModel()
) {
	Box(modifier = modifier) {
		if (value.text.isEmpty()) {
			Text(
				modifier = Modifier.fillMaxSize(),
				text = textFieldModel.placeholder,
				style = textFieldModel.textStyle.copy(
					color = textFieldModel.placeholderColor
				)
			)
		}
		BasicTextField(
			modifier = Modifier.fillMaxSize(),
			value = value,
			onValueChange = onValueChange,
			keyboardOptions = textFieldModel.keyboardOptions,
			visualTransformation = { TransformedText(styledValue, OffsetMapping.Identity) },
			textStyle = textFieldModel.textStyle.copy(
				textFieldModel.textColor
			),
			cursorBrush = SolidColor(textFieldModel.cursorColor)
		)
	}
}

@Composable
fun defaultRTTextFieldModel() = RTTextFieldModel(
	keyboardOptions = KeyboardOptions(
		capitalization = KeyboardCapitalization.Sentences,
	),
	placeholder = EMPTY_STRING,
	textStyle = MaterialTheme.typography.body1,
	textColor = MaterialTheme.colors.onPrimary,
	placeholderColor = MaterialTheme.colors.secondaryVariant,
	cursorColor = MaterialTheme.colors.secondary,
)

data class RTTextFieldModel(
	val keyboardOptions: KeyboardOptions,
	val placeholder: String,
	val textStyle: TextStyle,
	val textColor: Color,
	val placeholderColor: Color,
	val cursorColor: Color
)
