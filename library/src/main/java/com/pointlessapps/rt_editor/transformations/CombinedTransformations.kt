package com.pointlessapps.rt_editor.transformations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal fun combinedTransformations(
	styledValue: AnnotatedString,
	vararg transformations: VisualTransformation
) = VisualTransformation {
	val offsetMappings = mutableListOf<OffsetMapping>()
	val output = transformations.fold(styledValue) { value, transformation ->
		val output = transformation.filter(value)
		offsetMappings.add(output.offsetMapping)
		output.text
	}

	return@VisualTransformation TransformedText(output, object : OffsetMapping {
		override fun originalToTransformed(offset: Int) =
			offsetMappings.fold(offset) { acc, offsetMapping ->
				offsetMapping.originalToTransformed(acc)
			}

		override fun transformedToOriginal(offset: Int) =
			offsetMappings.fold(offset) { acc, offsetMapping ->
				offsetMapping.transformedToOriginal(acc)
			}
	})
}
