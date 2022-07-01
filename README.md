# Jetpack Compose Rich Text Editor

[![](https://jitpack.io/v/pChochura/richtext-compose.svg)](https://jitpack.io/#pChochura/richtext-compose)

I've been looking for a library that is able to deliver an editable component which can render rich
text in real time. The main issue with libraries I found was that they were using WebView and
Javascript under the hood. I wanted something compatible with Jetpack Compose.

So the only solution was to create my own library.

# Table of contents
 - [Installation](#installation)
 - [Usage](#usage)
    - [Available Styles](#available-styles)
    - [Custom styling](#custom-styling)
    - [Serialization](#serialization)

# Installation

1. Add a link to the Jitpack repository

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

2. Include link do the library (change the version to the current one)

```groovy
implementation "com.github.pChochura:richtext-compose:$version"
```

# Usage

Insert function call to a Composable:

```kotlin
var value by remember { mutableStateOf(RichTextValue.get()) }

RichTextEditor(
    modifier = Modifier,
    value = value,
    onValueChange = { value = it },
    textFieldStyle = defaultRichTextFieldStyle().copy(
        placeholder = "My rich text editor in action",
        textColor = MaterialTheme.colors.onPrimary,
        placeholderColor = MaterialTheme.colors.secondaryVariant,
    )
)

// If you want to render a static text use `RichText` instead
RichText(
   modifier = Modifier,
   value = value,
   textStyle = defaultRichTextStyle().copy(
      textColor = MaterialTheme.colors.onPrimary,
   )
)
```

You can easily stylize the TextField that is under the hood by providing values for
the `textFieldStyle`. Default ones are as follows:

```kotlin
@Composable
fun defaultRichTextFieldStyle() = RichTextFieldStyle(
     keyboardOptions = KeyboardOptions(
         capitalization = KeyboardCapitalization.Sentences,
     ),
     placeholder = EMPTY_STRING,
     textStyle = MaterialTheme.typography.body1,
     textColor = MaterialTheme.colors.onPrimary,
     placeholderColor = MaterialTheme.colors.secondaryVariant,
     cursorColor = MaterialTheme.colors.secondary,
 )
```

To insert or clear styles you can use methods provided by the `RichTextValue` object:

```kotlin
abstract class RichTextValue {
    /**
     * Returns styles that are used inside the current selection (or composition)
     */
    abstract val currentStyles: Set<Style>
    abstract val isUndoAvailable: Boolean
    abstract val isRedoAvailable: Boolean

    abstract fun insertStyle(style: Style): RichTextValue
    abstract fun clearStyles(vararg styles: Style): RichTextValue

    abstract fun undo(): RichTextValue
    abstract fun redo(): RichTextValue
}
```

Every method that manipulates the text inside the TextField returns a copy of the object to be
passed to a state.

## Available styles

1. Bold
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.Bold)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.Bold)
   ```

2. Underline
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.Underline)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.Underline)
   ```

3. Italic
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.Italic)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.Italic)
   ```

4. Strikethrough
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.Strikethrough)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.Strikethrough)
   ```

5. Align Left
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.AlignLeft)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.AlignLeft)
   ```

6. Align Center
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.AlignCenter)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.AlignCenter)
   ```

7. Align Right
   ```kotlin
   // Inserting style
   value = value.insertStyle(Style.AlignRight)

   // Checking if the style is used inside the current selection
   val isInCurrentSelection = value.currentStyles.contains(Style.AlignRight)
   ```

8. Text Size
   ```kotlin
   // We're deleting all of the text size styles from the selection to avoid multiple multiplications of the size
   value = value.clearStyles(Style.TextColor())

   // Inserting style
   // You have to pass size as a parameter. It accepts values between 0.5f and 2.0f
   // Which means that the text size will be multiplied by the provided value
   value = value.insertStyle(Style.TextSize(textSize))

   // Checking if the style is used inside the current selection
   // Here we're using `filterIsInstance` to check if there are any of the text size styles
   val isInCurrentSelection = value.currentStyles.filterIsInstance<Style.TextSize>().isNotEmpty()
   ```

9. Text Color
   ```kotlin
   // We're deleting all of the text color styles from the selection to avoid having more than one color on the same portion of the text (the last one would be displayed either way)
   value = value.clearStyles(Style.TextColor())

   // Inserting style
   // You have to pass color as a parameter
   value = value.insertStyle(Style.TextColor(color))

   // Checking if the style is used inside the current selection
   // Here we're using `filterIsInstance` to check if there are any of the text color styles
   val isInCurrentSelection = value.value.currentStyles.filterIsInstance<Style.TextColor>().isNotEmpty()
   ```

## Custom styling

If you want to create your own styles you're free to do so. You would have to create a class that
extends `StyleMapper` and implement the styling there for the styles that you would have created.

```kotlin
// If you want to create a paragraph style you have to extend `ParagraphStyle` interface!
object CustomParagraphStyle : Style
object CustomStyle : Style

class CustomStyleMapper : StyleMapper() {

    override fun fromTag(tag: String): Style =
        runCatching { super.fromTag(tag) }.getOrNull() ?: when (tag) {
            // It is necessary to ensure undo/redo actions work correctly
            "${CustomStyle.javaClass.simpleName}/" -> CustomStyle
            "${CustomParagraphStyle.javaClass.simpleName}/" -> CustomParagraphStyle
            else -> throw IllegalArgumentException()
        }

    override fun toSpanStyle(style: Style): SpanStyle? = super.toSpanStyle(style) ?: when (style) {
        // Here we're customizing the behavior of the style
        is CustomStyle -> SpanStyle(
            color = Color.Red,
            fontWeight = FontWeight.Bold,
        )
        else -> null
    }

    override fun toParagraphStyle(style: Style): ParagraphStyle? =
        super.toParagraphStyle(style) ?: when (style) {
            is CustomParagraphStyle -> ParagraphStyle(
                textAlign = TextAlign.Justify,
                textIndent = TextIndent(firstLine = 12.sp)
            )
            else -> null
        }
}
```

And then you would have to pass an instance of the class you created as a parameter to
the `RichTextValue` class:

```kotlin
var value by remember {
    mutableStateOf(
        RichTextValue.get(
            styleMapper = CustomStyleMapper()
        )
    )
}
```

## Serialization

If you want to save the contents of the `RichTextValue` you can use `RichTextValueSnapshot` as described below:

```kotlin
var value by remember { mutableStateOf(RichTextValue.get()) }
RichTextEditor(
    value = value,
    onValueChange = { value = it },
)

// Get the last snapshot of the content
val snapshot: RichTextValueSnapshot = value.getLastSnapshot()

// Here you can save the snapshot to the database

// Get the content value back. It will be rendered the same way as with the initial value
var contentFromSnapshot by remember { mutableStateOf(RichTextValue.fromSnapshot(snapshot)) }
RichTextEditor(
    value = contentFromSnapshot,
    onValueChange = { contentFromSnapshot = it },
)
```
