package com.pointlessapps.rt_editor.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.pointlessapps.rt_editor.model.RichTextValue
import com.pointlessapps.rt_editor.transformations.UnorderedListTransformation
import com.pointlessapps.rt_editor.transformations.combinedTransformations

@Composable
fun RichText(
    value: RichTextValue,
    modifier: Modifier = Modifier,
    textStyle: RichTextStyle = defaultRichTextStyle(),
) {
    Text(
        modifier = modifier,
        text = combinedTransformations(
            styledValue = value.styledValue,
            VisualTransformation.None,
            UnorderedListTransformation()
        ).filter(value.styledValue).text,
        style = textStyle.textStyle.copy(
            color = textStyle.textColor,
        )
    )
}

@Composable
fun defaultRichTextStyle() = RichTextStyle(
    textStyle = MaterialTheme.typography.body1,
    textColor = MaterialTheme.colors.onPrimary,
)

data class RichTextStyle(
    val textStyle: TextStyle,
    val textColor: Color,
)
