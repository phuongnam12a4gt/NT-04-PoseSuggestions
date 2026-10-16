package com.ppnnttt.posesuggestions

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    private const val PREFS = "app_settings"
    private const val LANGUAGE_KEY = "language"
    const val DEFAULT_LANGUAGE = "vi"

    fun applySavedLanguage(context: Context) {
        setLanguage(context, getLanguage(context), persist = false)
    }

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(context: Context, languageTag: String, persist: Boolean = true) {
        if (persist) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(LANGUAGE_KEY, languageTag).apply()
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val manager = context.applicationContext.getSystemService(android.app.LocaleManager::class.java)
            val locales = android.os.LocaleList.forLanguageTags(languageTag)
            if (manager.applicationLocales != locales) manager.applicationLocales = locales
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        }
    }
}
