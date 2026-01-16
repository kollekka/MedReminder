package com.elozelo.medreminder.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Sekcja powitalna
        WelcomeSection()

        Spacer(modifier = Modifier.height(20.dp))

        // Dashboard Grid - szybki podgląd statystyk
        DashboardGrid(
            medications = medications,
            appointments = appointments,
            onNavigateToMedications = onNavigateToMedications,
            onNavigateToAppointments = onNavigateToAppointments
        )

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
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    // Powitanie zależne od pory dnia
    val greeting = when {
        hour < 6 -> stringResource(R.string.home_greeting_night)
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 18 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }

    // Ikona zależna od pory dnia
    val icon = when {
        hour < 6 -> Icons.Default.NightsStay
        hour < 12 -> Icons.Default.WbSunny
        hour < 18 -> Icons.Default.WbSunny
        else -> Icons.Default.NightsStay
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona w kółku
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DashboardGrid(
    medications: List<Medication>,
    appointments: List<Appointment>,
    onNavigateToMedications: () -> Unit,
    onNavigateToAppointments: () -> Unit
) {
    val todayMedications = getMedicationsForTodaySorted(medications)
    val upcomingAppointments = appointments.filter { it.dateTime > System.currentTimeMillis() && !it.completed }
    val lowStockMedications = medications.filter { it.remainingQuantity <= 5 && it.remainingQuantity > 0 }
    val allTaken = areAllTodayMedicationsTaken(medications)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pierwszy rząd
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leki dzisiaj
            DashboardCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_medications_today),
                value = if (allTaken) "✓" else todayMedications.size.toString(),
                icon = Icons.Default.Medication,
                color = if (allTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                onClick = onNavigateToMedications
            )

            // Nadchodzące wizyty
            DashboardCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_upcoming_appointments),
                value = upcomingAppointments.size.toString(),
                icon = Icons.Default.Event,
                color = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToAppointments
            )
        }

        // Drugi rząd
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Aktywne leki
            DashboardCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_active_medications),
                value = medications.filter { it.remainingQuantity > 0 }.size.toString(),
                icon = Icons.Default.Inventory,
                color = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToMedications
            )

            // Kończące się leki
            DashboardCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_low_stock_medications),
                value = lowStockMedications.size.toString(),
                icon = Icons.Default.Warning,
                color = if (lowStockMedications.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                onClick = onNavigateToMedications
            )
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolorowa ikona
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
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
    val hasAnyMedications = medications.any { it.remainingQuantity > 0 }

    SectionHeader(
        title = stringResource(R.string.home_medications_today),
        showViewAll = hasAnyMedications,
        onViewAllClick = onNavigateToMedications
    )

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
private fun SectionHeader(
    title: String,
    showViewAll: Boolean,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        if (showViewAll) {
            TextButton(onClick = onViewAllClick) {
                Text(stringResource(R.string.home_view_all))
            }
        }
    }
}

@Composable
private fun MedicationTodayCard(medication: Medication, medicationViewModel: MedicationViewModel) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAlreadyTakenWarningDialog by remember { mutableStateOf(false) }

    // Sprawdź czy lek był już dzisiaj wzięty
    val calendar = Calendar.getInstance()
    val today = String.format(
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val alreadyTakenToday = medication.lastTakenDate == today && medication.dailyTakenCount > 0

    Card(
        modifier = Modifier.width(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row {
            // Niebieski pasek boczny
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${medication.quantity} ${medication.dosage.getLocalizedName(androidx.compose.ui.platform.LocalContext.current)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (medication.reminderTimes.isNotEmpty()) {
                    val reminderTimeStatus = getCurrentReminderTimeWithStatus(medication)
                    if (reminderTimeStatus != null) {
                        val (nextTime, isPast) = reminderTimeStatus
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.home_next_dose)}: $nextTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (alreadyTakenToday) {
                            showAlreadyTakenWarningDialog = true
                        } else {
                            showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.home_take_medication),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    // Dialog potwierdzający wzięcie leku (standardowy)
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.home_confirm_take_medication),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(onClick = { showConfirmDialog = false }) {
                        Text(stringResource(R.string.home_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            medicationViewModel.markMedicationTaken(medication)
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_yes))
                    }
                }
            }
        )
    }

    // Dialog ostrzegawczy - lek już dzisiaj wzięty
    if (showAlreadyTakenWarningDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyTakenWarningDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    stringResource(R.string.home_already_taken_warning_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.home_already_taken_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(onClick = { showAlreadyTakenWarningDialog = false }) {
                        Text(stringResource(R.string.home_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            medicationViewModel.markMedicationTaken(medication)
                            showAlreadyTakenWarningDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_yes))
                    }
                }
            }
        )
    }
}

@Composable
private fun UpcomingAppointmentsSection(appointments: List<Appointment>, isLoading: Boolean, onNavigateToAppointments: () -> Unit) {
    val upcomingAppointments = appointments
        .filter { it.dateTime > System.currentTimeMillis() && !it.completed }
        .sortedBy { it.dateTime }
        .take(3)

    SectionHeader(
        title = stringResource(R.string.home_upcoming_appointments),
        showViewAll = true,
        onViewAllClick = onNavigateToAppointments
    )

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
    val daysUntil = ((appointment.dateTime - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val isUrgent = daysUntil < 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row {
            // Kolorowy pasek boczny
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(
                        if (isUrgent) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikona
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatAppointmentDate(appointment.dateTime),
                        style = MaterialTheme.typography.bodySmall,
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

                // Dni do wizyty
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getDaysUntilAppointment(appointment.dateTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isUrgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockMedicationsSection(medications: List<Medication>) {
    val lowStockMedications = medications.filter { it.remainingQuantity <= 5 && it.remainingQuantity > 0 }

    if (lowStockMedications.isNotEmpty()) {
        Text(
            text = stringResource(R.string.home_low_stock_medications),
            style = MaterialTheme.typography.titleLarge,
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
        modifier = Modifier.width(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row {
            // Czerwony pasek boczny
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.error)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${stringResource(R.string.home_remaining)}: ${medication.remainingQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
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
    return getCurrentReminderTimeWithStatus(medication)?.first
}

/**
 * Zwraca parę (godzina przypomnienia, czy czas minął)
 * Jeśli czas minął, zwraca najbliższą nieobsłużoną godzinę z flagą isPast=true
 */
private fun getCurrentReminderTimeWithStatus(medication: Medication): Pair<String, Boolean>? {
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

    // Pobierz wszystkie godziny z informacją o czasie
    val timesWithInfo = medication.reminderTimes
        .mapNotNull { timeString ->
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                val timeInMinutes = hour * 60 + minute
                Triple(timeString, timeInMinutes, timeInMinutes < currentTimeInMinutes)
            } else null
        }
        .sortedBy { it.second }

    // Jeśli to dzisiaj i mamy lastTakenTime, szukaj następnej godziny po ostatnim wzięciu
    if (medication.lastTakenDate == today && medication.lastTakenTime != null) {
        val lastParts = medication.lastTakenTime.split(":")
        if (lastParts.size == 2) {
            val lastHour = lastParts[0].toIntOrNull() ?: 0
            val lastMinute = lastParts[1].toIntOrNull() ?: 0
            val lastTimeInMinutes = lastHour * 60 + lastMinute

            // Znajdź następną godzinę po ostatnim wzięciu
            val nextTime = timesWithInfo.find { it.second > lastTimeInMinutes }
            if (nextTime != null) {
                return Pair(nextTime.first, nextTime.third)
            }
        }
    } else {
        // Nowy dzień lub brak lastTakenTime
        // Najpierw szukaj przyszłych godzin
        val futureTime = timesWithInfo.find { !it.third }
        if (futureTime != null) {
            return Pair(futureTime.first, false)
        }

        // Jeśli nie ma przyszłych, zwróć pierwszą godzinę jako przeszłą
        val pastTime = timesWithInfo.firstOrNull()
        if (pastTime != null) {
            return Pair(pastTime.first, true)
        }
    }

    return null
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