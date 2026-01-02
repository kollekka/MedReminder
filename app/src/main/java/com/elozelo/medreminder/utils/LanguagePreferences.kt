package com.elozelo.medreminder.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_settings")

class LanguagePreferences(private val context: Context) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        const val POLISH = "pl"
        const val ENGLISH = "en"
    }

    val languageFlow: Flow<String> = context.languageDataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: POLISH
        }

    suspend fun setLanguage(language: String) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
}

