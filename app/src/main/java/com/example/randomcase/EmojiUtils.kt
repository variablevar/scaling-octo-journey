package com.example.randomcase
import org.json.JSONArray
import org.json.JSONObject

data class Emoji(
    val name: String,
    val keywords: List<String>,
    val char: String,
    val category: String
)

fun emojify(input: String, emojis: List<Emoji>): String {

    var emojifiedString = input

    // Iterate over each emoji and its keywords
    for (emoji in emojis) {
        for (keyword in emoji.keywords) {
            // Replace each keyword with the corresponding emoji character
            emojifiedString = emojifiedString.replace(keyword, emoji.char, ignoreCase = true)
        }
    }

    return emojifiedString
}

fun parseEmojis(json: String): List<Emoji> {
    val jsonArray = JSONArray(json)
    val emojis = mutableListOf<Emoji>()

    for (i in 0 until jsonArray.length()) {
        val emojiJson = jsonArray.getJSONObject(i)
        val name = emojiJson.getString("name")
        val keywords = emojiJson.getJSONArray("keywords").let {
            List(it.length()) { i -> it.getString(i) }
        }
        val char = emojiJson.getString("char")
        val category = emojiJson.getString("category")

        emojis.add(Emoji(name, keywords, char, category))
    }

    return emojis
}
