package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "status_saver_language_prefs"
    private const val KEY_LANG = "selected_language"

    private lateinit var prefs: SharedPreferences
    private val _currentLanguage = MutableStateFlow("az")
    val currentLanguage: StateFlow<String> = _currentLanguage

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANG, null)
        if (savedLang != null) {
            _currentLanguage.value = savedLang
        } else {
            // Detect system locale
            val systemLocale = Locale.getDefault().language
            val defaultLang = when (systemLocale) {
                "ru" -> "ru"
                "tr" -> "tr"
                "az" -> "az"
                else -> "en" // Default to English
            }
            _currentLanguage.value = defaultLang
            prefs.edit().putString(KEY_LANG, defaultLang).apply()
        }
    }

    fun setLanguage(lang: String) {
        if (lang in listOf("az", "en", "ru", "tr")) {
            _currentLanguage.value = lang
            prefs.edit().putString(KEY_LANG, lang).apply()
        }
    }

    /**
     * Translates a string key or returns a value based on the current active language.
     */
    fun translate(
        az: String,
        en: String,
        ru: String,
        tr: String
    ): String {
        return when (_currentLanguage.value) {
            "en" -> en
            "ru" -> ru
            "tr" -> tr
            else -> az
        }
    }
}

/**
 * Extension/helper to access the reactive translation dynamically inside Composables.
 */
@Composable
fun t(
    az: String,
    en: String,
    ru: String,
    tr: String
): String {
    // Collect the language state and read its value to trigger recomposition when changed
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    return when (currentLang) {
        "en" -> en
        "ru" -> ru
        "tr" -> tr
        else -> az
    }
}
