package com.elozelo.medreminder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit { putBoolean(KEY_DARK_MODE, value) }
        }
}
