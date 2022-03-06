package com.pointlessapps.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.pointlessapps.rt_editor.mappers.StyleMapper
import com.pointlessapps.rt_editor.model.RichTextValue
import com.pointlessapps.rt_editor.model.Style
import com.pointlessapps.rt_editor.ui.RichTextEditor
import com.pointlessapps.rt_editor.ui.defaultRichTextFieldStyle
import kotlin.random.Random

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Surface(modifier = Modifier.fillMaxSize()) {
				var value by remember {
					mutableStateOf(
						RichTextValue.get(
							// Optional parameter; leave it blank if you want to use provided styles
							// But if you want to customize the user experience you're free to do that
							// by providing a custom StyleMapper
							styleMapper = CustomStyleMapper()
						)
					)
				}

				RichTextEditor(
					modifier = Modifier
						.fillMaxSize()
						.padding(16.dp),
					value = value,
					onValueChange = { value = it },
					textFieldStyle = defaultRichTextFieldStyle().copy(
						textColor = Color.Black,
						placeholderColor = Color.LightGray,
						placeholder = "My rich text editor in action"
					)
				)

				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.BottomCenter
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.wrapContentHeight()
							.background(Color.DarkGray)
							.horizontalScroll(rememberScrollState()),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.Center
					) {
						// Button for a custom style
						IconButton(onClick = {
							value = value.insertStyle(BoldRedStyle)
						}) {
							Icon(
								modifier = Modifier.size(24.dp),
								painter = painterResource(id = R.drawable.icon_bold),
								tint = if (value.currentStyles.contains(BoldRedStyle)) {
									Color.Red
								} else {
									Color.Red.copy(alpha = 0.3f)
								},
								contentDescription = null
							)
						}

						EditorAction(
							iconRes = R.drawable.icon_bold,
							active = value.currentStyles.contains(Style.Bold)
						) {
							value = value.insertStyle(Style.Bold)
						}
						EditorAction(
							iconRes = R.drawable.icon_underline,
							active = value.currentStyles.contains(Style.Underline)
						) {
							value = value.insertStyle(Style.Underline)
						}
						EditorAction(
							iconRes = R.drawable.icon_italic,
							active = value.currentStyles.contains(Style.Italic)
						) {
							value = value.insertStyle(Style.Italic)
						}
						EditorAction(
							iconRes = R.drawable.icon_strikethrough,
							active = value.currentStyles.contains(Style.Strikethrough)
						) {
							value = value.insertStyle(Style.Strikethrough)
						}
						EditorAction(
							iconRes = R.drawable.icon_align_left,
							active = value.currentStyles.contains(Style.AlignLeft)
						) {
							value = value.insertStyle(Style.AlignLeft)
						}
						EditorAction(
							iconRes = R.drawable.icon_align_center,
							active = value.currentStyles.contains(Style.AlignCenter)
						) {
							value = value.insertStyle(Style.AlignCenter)
						}
						EditorAction(
							iconRes = R.drawable.icon_align_right,
							active = value.currentStyles.contains(Style.AlignRight)
						) {
							value = value.insertStyle(Style.AlignRight)
						}
						EditorAction(
							iconRes = R.drawable.icon_text_size,
							active = value.currentStyles
								.filterIsInstance<Style.TextSize>()
								.isNotEmpty()
						) {
							// Remove all styles in selected region that changes the text size
							value = value.clearStyles(Style.TextSize())

							// Here you would show a dialog of some sorts and allow user to pick
							// a specific text size. I'm gonna use a random one between 50% and 200%

							value = value.insertStyle(
								Style.TextSize(
									(Random.nextFloat() *
											(Style.TextSize.MAX_VALUE - Style.TextSize.MIN_VALUE) +
											Style.TextSize.MIN_VALUE).toFloat()
								)
							)
						}
						EditorAction(
							iconRes = R.drawable.icon_circle,
							active = value.currentStyles
								.filterIsInstance<Style.TextColor>()
								.isNotEmpty()
						) {
							// Remove all styles in selected region that changes the text color
							value = value.clearStyles(Style.TextColor())

							// Here you would show a dialog of some sorts and allow user to pick
							// a specific color. I'm gonna use a random one

							value = value.insertStyle(
								Style.TextColor(Random.nextInt(360).hueToColor())
							)
						}
						EditorAction(R.drawable.icon_format_clear, active = true) {
							value = value.insertStyle(Style.ClearFormat)
						}
						EditorAction(
							iconRes = R.drawable.icon_undo,
							active = value.isUndoAvailable
						) {
							value = value.undo()
						}
						EditorAction(
							iconRes = R.drawable.icon_redo,
							active = value.isRedoAvailable
						) {
							value = value.redo()
						}
					}
				}
			}
		}
	}

	@Composable
	private fun EditorAction(
		@DrawableRes iconRes: Int,
		active: Boolean,
		onClick: () -> Unit,
	) {
		IconButton(onClick = onClick) {
			Icon(
				modifier = Modifier.size(24.dp),
				painter = painterResource(id = iconRes),
				tint = if (active) Color.White else Color.Black,
				contentDescription = null
			)
		}
	}

	private fun Int.hueToColor(saturation: Float = 1f, value: Float = 0.5f): Color = Color(
		ColorUtils.HSLToColor(floatArrayOf(this.toFloat(), saturation, value))
	)
}

object BoldRedStyle : Style

class CustomStyleMapper : StyleMapper() {

	override fun fromTag(tag: String) =
		runCatching { super.fromTag(tag) }.getOrNull() ?: when (tag) {
			"${BoldRedStyle.javaClass.simpleName}/" -> BoldRedStyle
			else -> throw IllegalArgumentException()
		}

	override fun toSpanStyle(style: Style) = super.toSpanStyle(style) ?: when (style) {
		is BoldRedStyle -> SpanStyle(
			color = Color.Red,
			fontWeight = FontWeight.Bold,
		)
		else -> null
	}
}
