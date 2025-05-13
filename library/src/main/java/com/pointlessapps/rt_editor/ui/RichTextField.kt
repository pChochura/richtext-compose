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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.pointlessapps.rt_editor.transformations.UnorderedListTransformation
import com.pointlessapps.rt_editor.transformations.combinedTransformations

@Composable
internal fun RichTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    styledValue: AnnotatedString,
    modifier: Modifier = Modifier,
    textFieldStyle: RichTextFieldStyle = defaultRichTextFieldStyle(),
) {
    Box(modifier = modifier) {
        if (value.text.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxSize(),
                text = textFieldStyle.placeholder,
                style = textFieldStyle.textStyle.copy(
                    color = textFieldStyle.placeholderColor,
                ),
            )
        }
        BasicTextField(
            modifier = Modifier.fillMaxSize(),
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = textFieldStyle.keyboardOptions,
            visualTransformation = combinedTransformations(
                styledValue = styledValue,
                VisualTransformation.None,
                UnorderedListTransformation(),
            ),
            textStyle = textFieldStyle.textStyle.copy(
                color = textFieldStyle.textColor,
            ),
            cursorBrush = SolidColor(textFieldStyle.cursorColor),
        )
    }
}

@Composable
fun defaultRichTextFieldStyle() = RichTextFieldStyle(
    keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
    ),
    placeholder = "",
    textStyle = MaterialTheme.typography.body1,
    textColor = MaterialTheme.colors.onPrimary,
    placeholderColor = MaterialTheme.colors.secondaryVariant,
    cursorColor = MaterialTheme.colors.secondary,
)

data class RichTextFieldStyle(
    val keyboardOptions: KeyboardOptions,
    val placeholder: String,
    val textStyle: TextStyle,
    val textColor: Color,
    val placeholderColor: Color,
    val cursorColor: Color
)
