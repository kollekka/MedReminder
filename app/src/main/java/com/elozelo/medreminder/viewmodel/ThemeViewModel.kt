package com.elozelo.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.elozelo.medreminder.utils.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    private val _isDarkMode = MutableStateFlow(themePreferences.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        themePreferences.isDarkMode = newValue
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        themePreferences.isDarkMode = enabled
    }
}

