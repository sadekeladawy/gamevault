package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryRed

/**
 * Beautiful, native Jetpack Compose Markdown renderer for Gemini AI responses.
 *
 * CRITICAL RULE:
 * Absolutely NO raw Markdown asterisks (* or **) are ever rendered literally.
 * - Bold (**text**) is rendered as bold typography.
 * - Headings (### Title) are rendered as structured heading blocks with spacing.
 * - Bullet lists (* item, - item) are rendered with clean bullet markers.
 * - Numbered lists (1. item) are rendered with styled numbers.
 * - Stray asterisks during streaming are cleaned out automatically.
 */
@Composable
fun AiMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    if (markdown.isBlank()) return

    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed,
                            fontSize = when (block.level) {
                                1 -> 17.sp
                                2 -> 16.sp
                                else -> 15.sp
                            }
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .background(PrimaryRed, CircleShape)
                        )
                        Text(
                            text = block.annotatedText,
                            color = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed,
                            fontSize = fontSize,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = block.annotatedText,
                            color = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.annotatedText,
                        color = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val annotatedText: AnnotatedString) : MarkdownBlock()
    data class NumberedItem(val number: String, val annotatedText: AnnotatedString) : MarkdownBlock()
    data class Paragraph(val annotatedText: AnnotatedString) : MarkdownBlock()
}

/**
 * Parses raw text into structured Markdown blocks and strips all literal formatting symbols.
 */
private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    // Sanitize excessive emojis and stars
    val cleanedText = rawText
        .replace(Regex("[⭐★☆✨🌟]"), "")
        .replace(Regex("Rating\\s*:\\s*[⭐★☆✨🌟]+"), "Rating: ")
        .trim()

    val lines = cleanedText.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotBlank()) {
            val content = paragraphBuffer.toString().trim()
            if (content.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(buildFormattedInlineString(content)))
            }
            paragraphBuffer.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        // Heading: #, ##, ###
        val headingMatch = Regex("^(#{1,3})\\s+(.+)$").find(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val titleText = headingMatch.groupValues[2].replace("**", "").replace("*", "").trim()
            blocks.add(MarkdownBlock.Heading(level, titleText))
            continue
        }

        // Bullet item: *, -, •, +
        val bulletMatch = Regex("^([*\\-•+])\\s+(.+)$").find(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            val itemText = bulletMatch.groupValues[2]
            blocks.add(MarkdownBlock.BulletItem(buildFormattedInlineString(itemText)))
            continue
        }

        // Numbered item: 1. or 1)
        val numberedMatch = Regex("^(\\d+)[.)]\\s+(.+)$").find(trimmed)
        if (numberedMatch != null) {
            flushParagraph()
            val num = numberedMatch.groupValues[1]
            val itemText = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, buildFormattedInlineString(itemText)))
            continue
        }

        // Regular text line
        if (paragraphBuffer.isNotEmpty()) {
            paragraphBuffer.append(" ")
        }
        paragraphBuffer.append(trimmed)
    }

    flushParagraph()
    return blocks
}

/**
 * Builds an AnnotatedString from inline markdown (bolding, italics, code)
 * and strips ALL raw asterisks from the text.
 */
fun buildFormattedInlineString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Check for **bold**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldText = text.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldText)
                    pop()
                    i = end + 2
                    continue
                } else {
                    // Unclosed ** during streaming: skip the ** characters
                    val remaining = text.substring(i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(remaining)
                    pop()
                    break
                }
            }

            // Check for `inline code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeText = text.substring(i + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.White.copy(alpha = 0.08f)
                        )
                    )
                    append(codeText)
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Check for *italic* (single asterisk)
            if (text[i] == '*' && (i + 1 == len || text[i + 1] != '*')) {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    val italicText = text.substring(i + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicText)
                    pop()
                    i = end + 1
                    continue
                } else {
                    // Stray single asterisk: skip it completely!
                    i++
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}
