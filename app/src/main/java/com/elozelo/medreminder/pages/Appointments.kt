package com.elozelo.medreminder.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.Appointment
import com.elozelo.medreminder.ui.components.AddAppointmentDialog
import com.elozelo.medreminder.ui.components.formatDate
import com.elozelo.medreminder.ui.theme.MedReminderTheme
import com.elozelo.medreminder.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun AppointmentPage(
    viewModel: AppointmentViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues()
) {
    val appointments by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    // Filtrowanie i sortowanie wizyt
    val filteredAppointments = remember(appointments, showHistory) {
        if (showHistory) {
            // Historia - wizyt odbyte, sortowane od najnowszej
            appointments.filter { it.completed }
                .sortedByDescending { it.dateTime }
        } else {
            // Aktualne - nieodbyte, sortowane chronologicznie od najbliższej
            appointments.filter { !it.completed }
                .sortedBy { it.dateTime }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.appointments_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Przycisk Historia/Aktualne
                OutlinedButton(
                    onClick = { showHistory = !showHistory },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (showHistory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.History else Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showHistory) stringResource(R.string.appointments_upcoming) else stringResource(R.string.appointments_history))
                }
            }

            if (isLoading && appointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredAppointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showHistory) "No completed appointments." else "No scheduled appointments.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredAppointments) { appointment ->
                        AppointmentItem(
                            appointment = appointment,
                            onDelete = { viewModel.deleteAppointment(appointment.id) },
                            onClick = { appointmentToEdit = appointment },
                            onMarkCompleted = { viewModel.markAppointmentCompleted(appointment) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 5.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.appointments_add), tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { appointment ->
                viewModel.addAppointment(appointment)
                showAddDialog = false
            }
        )
    }

    appointmentToEdit?.let { appointment ->
        AddAppointmentDialog(
            appointment = appointment,
            onDismiss = { appointmentToEdit = null },
            onConfirm = { updatedAppointment ->
                viewModel.updateAppointment(updatedAppointment)
                appointmentToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentItem(
    appointment: Appointment,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkCompleted: () -> Unit = {}
) {
    val daysUntil = TimeUnit.MILLISECONDS.toDays(appointment.dateTime - System.currentTimeMillis())
    val isUrgent = daysUntil < 3 && daysUntil >= 0
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    // Stan rozwinięcia karty
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = appointment.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 120.dp)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUrgent) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Wizyta się zbliża!",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Ikona powiadomień
                    Icon(
                        imageVector = if (appointment.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = "Status przypomnienia",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )

                    // Ikona strzałki
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Sekcja środkowa - zwijalna, klikalna do edycji
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Lewa kolumna - informacje
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Data wizyty", tint = iconColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = formatDate(appointment.dateTime), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Text(text = formatTimeOnly(appointment.dateTime), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }

                        if (appointment.location.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Lokalizacja", tint = iconColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = appointment.location, fontSize = 18.sp)
                            }
                        }
                    }

                    // Prawa strona - ikona i notatki
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "Lekarz",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(120.dp)
                        )

                        if (appointment.notes.isNotEmpty()) {
                            var showNotesDialog by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showNotesDialog = true },
                                modifier = Modifier.width(120.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.notes_title), fontSize = 12.sp)
                            }

                            if (showNotesDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNotesDialog = false },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Notes, contentDescription = null)
                                            Text("${stringResource(R.string.notes_title)} - ${appointment.name}")
                                        }
                                    },
                                    text = { Text(text = appointment.notes, style = MaterialTheme.typography.bodyLarge) },
                                    confirmButton = {
                                        TextButton(onClick = { showNotesDialog = false }) { Text(stringResource(R.string.close)) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Przyciski akcji - stopka
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkCompleted,
                    modifier = Modifier.weight(1f),
                    enabled = !appointment.completed,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (appointment.completed) stringResource(R.string.appointments_completed) else stringResource(R.string.appointments_mark_as_done),
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(0.8f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun formatTimeOnly(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Preview(showBackground = true, name = "Urgent Appointment Preview")
@Composable
private fun AppointmentItemPreviewUrgent() {
    MedReminderTheme {
        AppointmentItem(
            appointment = Appointment(
                name = "Kontrola kardiologiczna",
                location = "Szpital Miejski, Pokój 203",
                dateTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
                reminderEnabled = true,
                notes = ""
            ),
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Standard Appointment Preview")
@Composable
private fun AppointmentItemPreviewStandard() {
    MedReminderTheme {
        AppointmentItem(
            appointment = Appointment(
                name = "Wizyta u dentysty",
                location = "Stomatologia uśmiech",
                dateTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10),
                reminderEnabled = false,
                notes = ""
            ),
            onDelete = {},
            onClick = {}
        )
    }
}
