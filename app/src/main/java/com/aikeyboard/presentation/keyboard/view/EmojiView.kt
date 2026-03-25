package com.aikeyboard.presentation.keyboard.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.aikeyboard.core.extension.dpToPx

/**
 * Emoji view for emoji input
 * 
 * Displays a grid of commonly used emojis.
 */
class EmojiView(
    context: Context,
    private val onEmojiClick: (String) -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(
            context.dpToPx(8),
            context.dpToPx(8),
            context.dpToPx(8),
            context.dpToPx(8)
        )
        gravity = Gravity.CENTER
        buildEmojiGrid()
    }

    /**
     * Build the emoji grid
     */
    private fun buildEmojiGrid() {
        val emojiRows = listOf(
            listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊"),
            listOf("❤️", "🔥", "✨", "👍", "👎", "👏", "🎉", "💯", "🙌", "💪"),
            listOf("🥰", "😍", "🤩", "😘", "😎", "🤔", "😢", "😭", "😤", "🤗"),
            listOf("🙏", "👋", "🤝", "✌️", "🤞", "👌", "✋", "👏", "🤲", "👐")
        )

        emojiRows.forEach { row ->
            addView(createEmojiRow(row))
        }
    }

    /**
     * Create a row of emojis
     */
    private fun createEmojiRow(emojis: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            emojis.forEach { emoji ->
                addView(createEmojiButton(emoji))
            }
        }
    }

    /**
     * Create a single emoji button
     */
    private fun createEmojiButton(emoji: String): TextView {
        return TextView(context).apply {
            text = emoji
            textSize = 24f
            setPadding(
                context.dpToPx(8),
                context.dpToPx(8),
                context.dpToPx(8),
                context.dpToPx(8)
            )
            setOnClickListener { onEmojiClick(emoji) }
        }
    }

    companion object {
        /**
         * Create an emoji view
         */
        fun create(context: Context, onEmojiClick: (String) -> Unit): EmojiView {
            return EmojiView(context, onEmojiClick)
        }
    }
}
