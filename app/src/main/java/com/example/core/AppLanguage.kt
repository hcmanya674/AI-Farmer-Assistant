package com.example.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val shortBadge: String,
    val nativeLabel: String,
    val geminiLanguageName: String
) {
    ENGLISH("en", "EN", "English", "English"),
    KANNADA("kn", "ಕ", "ಕನ್ನಡ", "Kannada (ಕನ್ನಡ)"),
    HINDI("hi", "हि", "हिन्दी", "Hindi (हिन्दी)"),
    MARATHI("mr", "म", "मराठी", "Marathi (मराठी)");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

fun Context.localizedContext(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.code)
    val config = Configuration(resources.configuration).apply {
        setLocale(locale)
    }
    return createConfigurationContext(config)
}
