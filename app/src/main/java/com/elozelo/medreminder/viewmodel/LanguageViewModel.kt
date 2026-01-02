package com.elozelo.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elozelo.medreminder.utils.LanguagePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    private val languagePreferences = LanguagePreferences(application)

    val currentLanguage: StateFlow<String> = languagePreferences.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LanguagePreferences.POLISH
        )

    fun setLanguage(language: String) {
        viewModelScope.launch {
            languagePreferences.setLanguage(language)
        }
    }
}

