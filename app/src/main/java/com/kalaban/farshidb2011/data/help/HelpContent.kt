package com.kalaban.farshidb2011.data.help

import android.content.Context
import com.google.gson.Gson

data class HelpSection(
    val heading: String? = null,
    val paragraphs: List<String>? = null,
    val steps: List<String>? = null,
    val tips: List<String>? = null,
)

data class HelpArticle(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val keywords: List<String>? = null,
    val sections: List<HelpSection>? = null,
) {
    /** Lowercased searchable text: title, summary, keywords and every section line. */
    val searchText: String by lazy {
        buildString {
            append(title).append(' ').append(summary).append(' ')
            keywords?.forEach { append(it).append(' ') }
            sections?.forEach { s ->
                append(s.heading.orEmpty()).append(' ')
                (s.paragraphs.orEmpty() + s.steps.orEmpty() + s.tips.orEmpty()).forEach { append(it).append(' ') }
            }
        }.lowercase()
    }
}

private data class HelpDocument(val articles: List<HelpArticle>? = null)

/** In-app guide loaded from `assets/help/help_<lang>.json`, so content can change without touching screens. */
object HelpContent {
    private val cache = HashMap<String, List<HelpArticle>>()

    @Synchronized
    fun articles(context: Context, language: String): List<HelpArticle> {
        val lang = if (language.startsWith("en")) "en" else "fa"
        return cache.getOrPut(lang) {
            runCatching {
                context.assets.open("help/help_$lang.json").bufferedReader(Charsets.UTF_8).use { reader ->
                    Gson().fromJson(reader, HelpDocument::class.java)?.articles.orEmpty()
                }
            }.getOrDefault(emptyList())
        }
    }

    fun search(articles: List<HelpArticle>, query: String): List<HelpArticle> {
        val terms = query.lowercase().split(' ').filter { it.isNotBlank() }
        if (terms.isEmpty()) return articles
        return articles.filter { a -> terms.all { a.searchText.contains(it) } }
    }
}
