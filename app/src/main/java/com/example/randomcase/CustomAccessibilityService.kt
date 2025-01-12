// TextSelectionAccessibilityService.kt
package com.example.randomcase

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlin.random.Random

class CustomAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var isDialogShowing = false
    private var emojiMap: Map<String, String> = emptyMap()
    private var emojis:List<Emoji> = emptyList()
    private var leetMapping = mapOf(
        'a' to '@',
        'e' to '3',
        'i' to '1',
        'o' to '0',
        't' to '7',
        's' to '5'
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        emojiMap = loadEmojiMapFromCsv("emojis.csv")
        emojis= loadEmojiMapFromJson("emoji.json")
    }

    private fun loadEmojiMapFromCsv(fileName: String): Map<String, String> {
        val map = mutableMapOf<String, String>()

        try {
            val inputStream = assets.open(fileName)
            inputStream.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line -> // Skip header
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val emoji = parts[0].trim()
                        val name = parts[1].trim()
                        name.split(" ").forEach { keyword ->
                            map[keyword.lowercase()] = emoji
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EmojiLoader", "Error loading emoji CSV", e)
        }

        return map
    }

    private fun loadEmojiMapFromJson(fileName: String):List<Emoji>{
        try {
            val inputStream = assets.open(fileName)
            val json = inputStream.bufferedReader().toString()
            return parseEmojis(json)
        } catch (e: Exception) {
            Log.e("EmojiLoader", "Error loading emoji CSV", e)
        }
        return emptyList()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val source = event.source ?: return

            // Validate: Ensure it's a proper selection with non-empty text
            val fullText = source.text?.toString() ?: return
            val selectionStart = source.textSelectionStart
            val selectionEnd = source.textSelectionEnd
            val selectedText = fullText.substring(selectionStart,selectionEnd)

            if (selectedText.isNotEmpty() && !isDialogShowing) {
                // Show dialog only if text is selected
                showTransformDialog(source, selectedText)
            }
        }
    }

    private fun showTransformDialog(nodeInfo: AccessibilityNodeInfo, text: String) {
        isDialogShowing = true

        val dialog = AlertDialog.Builder(this)
            .setTitle("Transform Text")
            .setItems(arrayOf("rAnDoM cAsE", "UPPERCASE", "lowercase", "Emojify","Leet")) { dialogInterface, which ->
                val transformedText = when (which) {
                    0 -> randomizeCase(text) // Random case transformation
                    1 -> text.uppercase()    // Convert to uppercase
                    2 -> text.lowercase()    // Convert to lowercase
                    // 3 -> emojify(text,emojis)   // Emojify text (new feature)
                    3 -> emojifyText(text)   // Emojify text (new feature)
                    4 -> leetCase(text)
                    else -> text
                }

                // Apply transformed text to the input field
                updateInputField(nodeInfo, transformedText)
                dialogInterface.dismiss()
                dialogInterface.cancel()
            }
            .setOnCancelListener {
                isDialogShowing = false
            }
            .create()

        dialog.window?.let { window ->
            window.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            window.attributes?.apply {
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error showing dialog", e)
            isDialogShowing = false
        }
    }


    private fun updateInputField(nodeInfo: AccessibilityNodeInfo, transformedText: String) {
        if (nodeInfo.isEditable) {
            // Retrieve the current text
            val currentText = nodeInfo.text?.toString() ?: ""
            val selectionStart = nodeInfo.textSelectionStart
            val selectionEnd = nodeInfo.textSelectionEnd

            if (selectionStart != -1 && selectionEnd != -1 && selectionStart < selectionEnd) {
                // Update only the selected portion of the text
                val updatedText = currentText.substring(0, selectionStart) +
                        transformedText +
                        currentText.substring(selectionEnd)

                // Apply the updated text back to the input field
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
                }
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                Toast.makeText(this, "Selected text updated in the input field", Toast.LENGTH_SHORT).show()
            } else {
                // No valid selection, fallback to copying to clipboard
                Toast.makeText(this, "No valid text selection found", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Fall back to copying to clipboard
            copyToClipboard(transformedText)
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        isDialogShowing = false
    }


    private fun randomizeCase(text: String): String {
        return text.map { char ->
            if (Random.nextBoolean()) char.uppercase() else char.lowercase()
        }.joinToString("")
    }

    private fun leetCase(text: String): String {
        return text.map { char ->
            if (leetMapping[char] != null) leetMapping[char] else char
        }.joinToString("")
    }

    private fun emojifyText(input: String): String {
        val words = input.split(" ").toMutableList()
        for (i in words.indices) {
            val word = words[i].lowercase().trim()
            if (emojiMap.containsKey(word)) {
                if (emojiMap[word] != null) {
                    words[i] = "${words[i]} ${emojiMap[word]}"
                }
            }
        }
        return words.joinToString(" ")
    }


    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("transformed_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Transformed text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {
        isDialogShowing = false
    }
}