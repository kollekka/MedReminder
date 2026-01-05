package com.elozelo.medreminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.elozelo.medreminder.ui.theme.MedReminderTheme
import com.elozelo.medreminder.utils.LanguagePreferences
import com.elozelo.medreminder.utils.languageDataStore
import com.elozelo.medreminder.viewmodel.LanguageViewModel
import com.elozelo.medreminder.viewmodel.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var themeViewModel: ThemeViewModel
    private lateinit var languageViewModel: LanguageViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MedReminder", "Uprawnienie do powiadomień przyznane")
        } else {
            Log.d("MedReminder", "Uprawnienie do powiadomień odrzucone")
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocaleContext(newBase))
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inicjalizacja ViewModels
        themeViewModel = ViewModelProvider(this)[ThemeViewModel::class.java]
        languageViewModel = ViewModelProvider(this)[LanguageViewModel::class.java]

        // Sprawdź i poproś o uprawnienia do powiadomień (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MedReminder", "Uprawnienie do powiadomień już przyznane")
                }
                else -> {
                    // Poproś o uprawnienie
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Automatyczne logowanie anonimowe jeśli użytkownik nie jest zalogowany
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { authResult ->
                    Log.d("MedReminder", "Zalogowano anonimowo: ${authResult.user?.uid}")
                }
                .addOnFailureListener { exception ->
                    Log.e("MedReminder", "Błąd logowania: ${exception.message}")
                }
        } else {
            Log.d("MedReminder", "Użytkownik już zalogowany: ${auth.currentUser?.uid}")
        }

        enableEdgeToEdge()
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val currentLanguage by languageViewModel.currentLanguage.collectAsState()

            // Zastosuj język
            updateLocale(currentLanguage)

            MedReminderTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    App(
                        themeViewModel = themeViewModel,
                        languageViewModel = languageViewModel
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        config.fontScale = 1.0f

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    @Suppress("DEPRECATION")
    private fun updateLocaleContext(context: Context): Context {
        // Odczytaj język synchronicznie z DataStore
        val languageCode = runBlocking {
            try {
                context.languageDataStore.data.first()[LanguagePreferences.LANGUAGE_KEY]
                    ?: LanguagePreferences.POLISH
            } catch (e: Exception) {
                Log.e("MedReminder", "Błąd odczytu języka: ${e.message}")
                LanguagePreferences.POLISH
            }
        }

        Log.d("MedReminder", "Ustawianie języka w attachBaseContext: $languageCode")

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        config.fontScale = 1.0f

        return context.createConfigurationContext(config)
    }
}

