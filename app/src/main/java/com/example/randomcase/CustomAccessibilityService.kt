package com.example.randomcase

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.Toast
import kotlin.random.Random

class CustomAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var isButtonAdded = false
    private var emojiMap: Map<String, String> = emptyMap()
    private val leetMapping = mapOf(
        'a' to '@', 'e' to '3', 'i' to '1',
        'o' to '0', 't' to '7', 's' to '5'
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        emojiMap = loadEmojiMapFromCsv("emojis.csv")

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
                                Math.abs(deltaY) > CLICK_ACTION_THRESHOLD) {
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
                                deltaY < CLICK_ACTION_THRESHOLD) {
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
            .setItems(arrayOf("rAnDoM cAsE", "UPPERCASE", "lowercase", "Emojify", "Leet")) { _, which ->
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
            rootNode.recycle()
        }
    }

    private fun findEditableNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodesRecursive(root, nodes)
        return nodes
    }

    private fun findEditableNodesRecursive(node: AccessibilityNodeInfo, nodes: MutableList<AccessibilityNodeInfo>) {
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

        if (start >= 0 && end > start) {
            val selectedText = text.substring(start, end)
            val transformedText = when (transformationType) {
                0 -> randomizeCase(selectedText)
                1 -> selectedText.uppercase()
                2 -> selectedText.lowercase()
                3 -> emojifyText(selectedText)
                4 -> leetCase(selectedText)
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