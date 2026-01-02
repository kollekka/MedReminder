package com.elozelo.medreminder

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elozelo.medreminder.pages.*
import com.elozelo.medreminder.viewmodel.LanguageViewModel
import com.elozelo.medreminder.viewmodel.ThemeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun App(
    themeViewModel: ThemeViewModel = viewModel(),
    languageViewModel: LanguageViewModel = viewModel()
) {
    val selectedRoute = remember { mutableStateOf(NavRoutes.Home.route) }
    // Obserwuj język aby wymusić rekompozycję po zmianie
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()

    // Użyj key(currentLanguage) aby wymusić pełną rekompozycję UI po zmianie języka
    androidx.compose.runtime.key(currentLanguage) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { AppTitle() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            content = { paddingValues ->
                when(selectedRoute.value) {
                    NavRoutes.Home.route -> HomePage(
                        paddingValues = paddingValues,
                        onNavigateToMedications = { selectedRoute.value = NavRoutes.Medications.route },
                        onNavigateToAppointments = { selectedRoute.value = NavRoutes.Reminders.route }
                    )
                    NavRoutes.Medications.route -> MedicationsPage(paddingValues = paddingValues)
                    NavRoutes.Reminders.route -> AppointmentPage(paddingValues = paddingValues)
                    NavRoutes.Settings.route -> SettingsPage(
                        themeViewModel = themeViewModel,
                        languageViewModel = languageViewModel,
                        paddingValues = paddingValues
                    )
                }
            },
            bottomBar = {
                // NavBar będzie teraz rekompozowany gdy currentLanguage się zmieni
                NavBar(
                    selectedRoute = selectedRoute.value,
                    onChange = { selectedRoute.value = it }
                )
            }
        )
    }
}

@Composable
private fun AppTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Text("MedReminder",
            modifier = Modifier
                .padding(end = 16.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            style = typography.headlineLarge
        )
    }
}