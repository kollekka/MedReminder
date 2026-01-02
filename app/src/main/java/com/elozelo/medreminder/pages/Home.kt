package com.elozelo.medreminder.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.Appointment
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.ui.theme.MedReminderTheme
import com.elozelo.medreminder.viewmodel.AppointmentViewModel
import com.elozelo.medreminder.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomePage(
    paddingValues: PaddingValues = PaddingValues(),
    medicationViewModel: MedicationViewModel = viewModel(),
    appointmentViewModel: AppointmentViewModel = viewModel(),
    onNavigateToMedications: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {}
) {
    val medications by medicationViewModel.medications.collectAsState()
    val appointments by appointmentViewModel.appointments.collectAsState()
    val isLoadingMeds by medicationViewModel.isLoading.collectAsState()
    val isLoadingAppts by appointmentViewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        WelcomeSection()


        Spacer(modifier = Modifier.height(24.dp))

        TodayMedicationsSection(
            medications,
            isLoadingMeds,
            medicationViewModel,
            onNavigateToMedications
        )

        Spacer(modifier = Modifier.height(24.dp))

        UpcomingAppointmentsSection(appointments, isLoadingAppts, onNavigateToAppointments)

        Spacer(modifier = Modifier.height(24.dp))

        LowStockMedicationsSection(medications)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardElevation(defaultElevation = 4.dp).let {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.home_overview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
private fun TodayMedicationsSection(
    medications: List<Medication>,
    isLoading: Boolean,
    medicationViewModel: MedicationViewModel,
    onNavigateToMedications: () -> Unit
) {
    val todayMedications = getMedicationsForTodaySorted(medications)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_medications_today),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (todayMedications.isNotEmpty()) {
            TextButton(onClick = onNavigateToMedications) {
                Text(stringResource(R.string.home_view_all))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (todayMedications.isEmpty() && areAllTodayMedicationsTaken(medications)) {
        // Wszystkie leki zostały wzięte
        EmptyStateCard(
            icon = Icons.Default.CheckCircle,
            message = stringResource(R.string.home_all_medications_taken),
            color = MaterialTheme.colorScheme.primary
        )
    } else if (todayMedications.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Default.CheckCircle,
            message = stringResource(R.string.home_no_medications_today),
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(todayMedications) { medication ->
                MedicationTodayCard(medication, medicationViewModel)
            }
        }
    }
}

@Composable
private fun MedicationTodayCard(medication: Medication, medicationViewModel: MedicationViewModel) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.width(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Healing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${medication.quantity} ${medication.dosage.getLocalizedName(androidx.compose.ui.platform.LocalContext.current)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (medication.reminderTimes.isNotEmpty()) {
                val nextTime = getNextReminderTime(medication)
                if (nextTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.home_next_dose)}: $nextTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.home_take_medication))
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(medication.name) },
            text = { Text(stringResource(R.string.home_confirm_take_medication)) },
            confirmButton = {
                TextButton(onClick = {
                    medicationViewModel.markMedicationTaken(medication)
                    showConfirmDialog = false
                }) {
                    Text(stringResource(R.string.home_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.home_no))
                }
            }
        )
    }
}

@Composable
private fun UpcomingAppointmentsSection(appointments: List<Appointment>, isLoading: Boolean, onNavigateToAppointments: () -> Unit) {
    val upcomingAppointments = appointments
        .filter { it.dateTime > System.currentTimeMillis() }
        .sortedBy { it.dateTime }
        .take(3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_upcoming_appointments),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (upcomingAppointments.isNotEmpty()) {
            TextButton(onClick = onNavigateToAppointments) {
                Text(stringResource(R.string.home_view_all))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (upcomingAppointments.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Default.Event,
            message = stringResource(R.string.home_no_appointments),
            color = MaterialTheme.colorScheme.secondary
        )
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            upcomingAppointments.forEach { appointment ->
                AppointmentCard(appointment)
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: Appointment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatAppointmentDate(appointment.dateTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (appointment.location.isNotEmpty()) {
                    Text(
                        text = appointment.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = getDaysUntilAppointment(appointment.dateTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LowStockMedicationsSection(medications: List<Medication>) {
    val lowStockMedications = medications.filter { it.remainingQuantity <= 5 }

    if (lowStockMedications.isNotEmpty()) {
        Text(
            text = stringResource(R.string.home_low_stock_medications),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lowStockMedications) { medication ->
                LowStockCard(medication)
            }
        }
    }
}

@Composable
private fun LowStockCard(medication: Medication) {
    Card(
        modifier = Modifier.width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stringResource(R.string.home_remaining)}: ${medication.remainingQuantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Funkcje pomocnicze
private fun getMedicationsForToday(medications: List<Medication>): List<Medication> {
    return medications.filter { medication ->
        medication.reminderEnabled && medication.reminderTimes.isNotEmpty()
    }
}

private fun getMedicationsForTodaySorted(medications: List<Medication>): List<Medication> {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTimeInMinutes = currentHour * 60 + currentMinute

    // Pobierz dzisiejszą datę
    val today = String.format(
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    return medications.filter { medication ->
        medication.reminderEnabled &&
        medication.reminderTimes.isNotEmpty() &&
        medication.remainingQuantity > 0 &&
        // Sprawdź czy nie przekroczono dziennego limitu dawek
        (medication.lastTakenDate != today || medication.dailyTakenCount < medication.reminderTimes.size)
    }.sortedBy { medication ->
        // Sortuj według najbliższej godziny przypomnienia
        medication.reminderTimes
            .mapNotNull { timeString ->
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val timeInMinutes = hour * 60 + minute

                    // Jeśli to dzisiaj i mamy lastTakenTime, szukaj następnej godziny po niej
                    if (medication.lastTakenDate == today && medication.lastTakenTime != null) {
                        val lastParts = medication.lastTakenTime.split(":")
                        if (lastParts.size == 2) {
                            val lastHour = lastParts[0].toIntOrNull() ?: 0
                            val lastMinute = lastParts[1].toIntOrNull() ?: 0
                            val lastTimeInMinutes = lastHour * 60 + lastMinute
                            if (timeInMinutes >= lastTimeInMinutes) timeInMinutes else null
                        } else timeInMinutes
                    } else {
                        // Nowy dzień lub brak lastTakenTime - pokaż przyszłe lub aktualne godziny
                        if (timeInMinutes >= currentTimeInMinutes - 30) timeInMinutes else null
                    }
                } else null
            }
            .minOrNull() ?: Int.MAX_VALUE
    }
}

private fun getNextReminderTime(medication: Medication): String? {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTimeInMinutes = currentHour * 60 + currentMinute

    // Pobierz dzisiejszą datę
    val today = String.format(
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    return medication.reminderTimes
        .mapNotNull { timeString ->
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                val timeInMinutes = hour * 60 + minute

                // Jeśli to dzisiaj i mamy lastTakenTime, zwróć następną godzinę po ostatnim wzięciu
                if (medication.lastTakenDate == today && medication.lastTakenTime != null) {
                    val lastParts = medication.lastTakenTime.split(":")
                    if (lastParts.size == 2) {
                        val lastHour = lastParts[0].toIntOrNull() ?: 0
                        val lastMinute = lastParts[1].toIntOrNull() ?: 0
                        val lastTimeInMinutes = lastHour * 60 + lastMinute
                        if (timeInMinutes >= lastTimeInMinutes) {
                            timeString to timeInMinutes
                        } else null
                    } else timeString to timeInMinutes
                } else {
                    // Nowy dzień lub brak lastTakenTime - zwróć najbliższą przyszłą godzinę
                    if (timeInMinutes >= currentTimeInMinutes - 30) {
                        timeString to timeInMinutes
                    } else null
                }
            } else null
        }
        .minByOrNull { it.second }
        ?.first
}

private fun areAllTodayMedicationsTaken(medications: List<Medication>): Boolean {
    val calendar = Calendar.getInstance()
    val today = String.format(
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Znajdź wszystkie leki które powinny być wzięte dzisiaj
    val todayMedications = medications.filter { medication ->
        medication.reminderEnabled &&
        medication.reminderTimes.isNotEmpty() &&
        medication.remainingQuantity > 0
    }

    // Jeśli nie ma żadnych leków na dzisiaj, zwróć false
    if (todayMedications.isEmpty()) {
        return false
    }

    // Sprawdź czy wszystkie leki na dzisiaj zostały wzięte
    return todayMedications.all { medication ->
        medication.lastTakenDate == today &&
        medication.dailyTakenCount >= medication.reminderTimes.size
    }
}

private fun formatAppointmentDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Composable
private fun getDaysUntilAppointment(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffInMillis = timestamp - now
    val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

    return when (diffInDays) {
        0 -> stringResource(R.string.home_today)
        1 -> stringResource(R.string.home_tomorrow)
        else -> "$diffInDays ${stringResource(R.string.home_days)}"
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePagePreview() {
    MedReminderTheme {
        HomePage()
    }
}