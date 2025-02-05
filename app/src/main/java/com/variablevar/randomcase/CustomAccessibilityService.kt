package com.variablevar.randomcase

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.Toast
import java.io.BufferedReader
import kotlin.random.Random
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class CustomAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var isButtonAdded = false
    private var emojiMap: Map<String, String> = emptyMap()
    private var listEmojis: List<Emoji> = emptyList()
    private val leetMapping = mapOf(
        'a' to 'a',
        'b' to 'ß',
        'c' to 'c',
        'd' to 'Ð',
        'e' to '3',
        'f' to 'ƒ',
        'g' to '9',
        'h' to 'h',
        'i' to '¡',
        'j' to 'ʝ',
        'k' to 'k',
        'l' to '1',
        'm' to 'm',
        'n' to 'n',
        'o' to '0',
        'p' to 'p',
        'q' to 'q',
        'r' to 'r',
        's' to '5',
        't' to '†',
        'u' to 'บ',
        'v' to '√',
        'w' to 'w',
        'x' to 'x',
        'y' to '¥',
        'z' to '2'
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        emojiMap = loadEmojiMapFromCsv("emojis.csv")
        listEmojis = loadEmojis(this)

        createFloatingButton()
    }

    private fun createFloatingButton() {
        if (isButtonAdded) return

        val layoutParams = WindowManager.LayoutParams(
            150, // Fixed width
            150, // Fixed height
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val imageView = ImageView(this).apply {
            setImageDrawable(packageManager.getApplicationIcon(packageName))

            setOnClickListener {
                showTransformDialog()
            }

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var startClickTime = 0L
                private val MAX_CLICK_DURATION = 200L
                private val CLICK_ACTION_THRESHOLD = 5

                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startClickTime = System.currentTimeMillis()
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()

                            if (Math.abs(deltaX) > CLICK_ACTION_THRESHOLD ||
                                Math.abs(deltaY) > CLICK_ACTION_THRESHOLD
                            ) {
                                layoutParams.x = initialX + deltaX
                                layoutParams.y = initialY + deltaY
                                windowManager?.updateViewLayout(view, layoutParams)
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            val clickDuration = System.currentTimeMillis() - startClickTime
                            val deltaX = Math.abs(event.rawX - initialTouchX)
                            val deltaY = Math.abs(event.rawY - initialTouchY)

                            if (clickDuration < MAX_CLICK_DURATION &&
                                deltaX < CLICK_ACTION_THRESHOLD &&
                                deltaY < CLICK_ACTION_THRESHOLD
                            ) {
                                view.performClick()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        windowManager?.addView(imageView, layoutParams)
        floatingButton = imageView
        isButtonAdded = true
    }

    private fun showTransformDialog() {
        // Find the editable node with selection
        val nodeWithSelection = findNodeWithSelection()

        if (nodeWithSelection == null) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Transform Text")
            .setItems(
                arrayOf(
                    "rAnDoM cAsE",
                    "UPPERCASE",
                    "lowercase",
                    "Emojify",
                    "Leet",
                    "0b0",
                    "0x0",
                    "Base64",
                )
            ) { _, which ->
                transformSelectedText(nodeWithSelection, which)
            }
            .create()
            .apply {
                window?.apply {
                    setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                    attributes?.apply {
                        format = PixelFormat.TRANSLUCENT
                        gravity = Gravity.CENTER
                        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    }
                }
                show()
            }
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

    private fun findNodeWithSelection(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null

        return try {
            findEditableNodes(rootNode).find { node ->
                node.isEditable &&
                        node.textSelectionStart != -1 &&
                        node.textSelectionEnd != -1 &&
                        node.textSelectionStart != node.textSelectionEnd
            }
        } finally {
            // rootNode.recycle()
        }
    }

    private fun findEditableNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodesRecursive(root, nodes)
        return nodes
    }

    private fun findEditableNodesRecursive(
        node: AccessibilityNodeInfo,
        nodes: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isEditable) {
            nodes.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findEditableNodesRecursive(child, nodes)
            }
        }
    }

    private fun transformSelectedText(node: AccessibilityNodeInfo, transformationType: Int) {
        val text = node.text?.toString() ?: return
        val start = node.textSelectionStart
        val end = node.textSelectionEnd

        if (start in 0..<end) {
            val selectedText = text.substring(start, end)
            val transformedText = when (transformationType) {
                0 -> randomizeCase(selectedText)
                1 -> selectedText.uppercase()
                2 -> selectedText.lowercase()
                3 -> emojifyTextV2(selectedText)
                4 -> leetCase(selectedText)
                5 -> convertToBase2(selectedText)
                6 -> convertToBase16(selectedText)
                7 -> convertToBase64(selectedText)
                else -> selectedText
            }

            val newText = text.substring(0, start) + transformedText + text.substring(end)
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
    }

    private fun convertToBase2(selectedText: String): String? {
        return selectedText.toByteArray().joinToString(" ") {
            it.toUByte().toString(2).padStart(8, '0')
        }
    }

    private fun convertToBase16(selectedText: String): String? {
        return selectedText.toByteArray().joinToString(" ") {
            it.toUByte().toString(16).uppercase().padStart(2, '0')
        }
    }

    private fun convertToBase64(selectedText: String): String? {
        return Base64.encodeToString(selectedText.toByteArray(), Base64.DEFAULT).trim()
    }

    private fun randomizeCase(text: String): String {
        return text.map { if (Random.nextBoolean()) it.uppercase() else it.lowercase() }
            .joinToString("")
    }

    private fun leetCase(text: String): String {
        return text.map { leetMapping[it.lowercaseChar()] ?: it }.joinToString("")
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

    private fun emojifyTextV2(input: String): String {
        val rawText = input.split(" ")
        val processedText = rawText.map { word ->
            var replacedWord = word
            for (model in listEmojis) {
                if (model.keywords.contains(word)) {
                    replacedWord = model.char
                    break // Stop checking once a match is found
                }
            }
            if (word == replacedWord) {
                word
            } else {
                "$word $replacedWord"
            }
        }
        return processedText.joinToString(" ")
    }

    private fun loadEmojis(context: Context): List<Emoji> {
        val jsonString = loadJsonFromAssets(context, "emoji.json")
        return parseEmojisFromJson(jsonString)
    }

    private fun loadJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use(BufferedReader::readText)
    }

    private fun parseEmojisFromJson(jsonString: String): List<Emoji> {
        val listType = object : TypeToken<List<Emoji>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle events as we're using the floating button
    }

    override fun onInterrupt() {
        removeFloatingButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
    }

    private fun removeFloatingButton() {
        if (floatingButton != null && isButtonAdded) {
            windowManager?.removeView(floatingButton)
            floatingButton = null
            isButtonAdded = false
        }
    }
}