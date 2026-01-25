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
import androidx.compose.ui.text.style.TextAlign
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
    paddingValues: PaddingValues = PaddingValues(),
    expandedAppointmentId: String? = null,
    onAppointmentExpanded: () -> Unit = {}
) {
    val appointments by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }


    val filteredAppointments = remember(appointments, showHistory, searchQuery) {
        val baseList = if (showHistory) {
            appointments.filter { it.completed }
                .sortedByDescending { it.dateTime }
        } else {
            appointments.filter { !it.completed }
                .sortedBy { it.dateTime }
        }


        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_appointments)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && appointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredAppointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            searchQuery.isNotEmpty() -> stringResource(R.string.search_no_results)
                            showHistory -> stringResource(R.string.appointments_no_completed)
                            else -> stringResource(R.string.appointments_no_scheduled)
                        },
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
                        val shouldBeExpanded = expandedAppointmentId == appointment.id
                        AppointmentItem(
                            appointment = appointment,
                            onDelete = { viewModel.deleteAppointment(appointment.id) },
                            onClick = { appointmentToEdit = appointment },
                            onMarkCompleted = { viewModel.markAppointmentCompleted(appointment) },
                            initiallyExpanded = shouldBeExpanded,
                            onExpandedChanged = {
                                if (shouldBeExpanded) {
                                    onAppointmentExpanded()
                                }
                            }
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
    onMarkCompleted: () -> Unit = {},
    initiallyExpanded: Boolean = false,
    onExpandedChanged: () -> Unit = {}
) {
    val daysUntil = remember(appointment.dateTime) {
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val appointmentCalendar = Calendar.getInstance().apply {
            timeInMillis = appointment.dateTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (appointmentCalendar.timeInMillis - todayCalendar.timeInMillis) / (1000 * 60 * 60 * 24)
    }
    val isUrgent = daysUntil < 3 && daysUntil >= 0
    val isPast = appointment.dateTime < System.currentTimeMillis()

    val accentColor = when {
        appointment.completed -> MaterialTheme.colorScheme.outline
        isUrgent -> MaterialTheme.colorScheme.tertiary
        isPast -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }


    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    LaunchedEffect(initiallyExpanded) {
        if (initiallyExpanded) {
            onExpandedChanged()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(accentColor)
                    .align(Alignment.CenterVertically)
                    .then(
                        if (isExpanded) Modifier.fillMaxHeight()
                        else Modifier.height(56.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = appointment.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isUrgent && !appointment.completed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (appointment.completed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (appointment.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (appointment.reminderEnabled)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = formatDate(appointment.dateTime),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatTimeOnly(appointment.dateTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (appointment.location.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = appointment.location,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!appointment.completed && !isPast) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isUrgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = when {
                                                daysUntil == 0L -> stringResource(R.string.home_today)
                                                daysUntil == 1L -> stringResource(R.string.home_tomorrow)
                                                else -> "$daysUntil ${stringResource(R.string.home_days)}"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUrgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (appointment.notes.isNotEmpty()) {
                                var showNotesDialog by remember { mutableStateOf(false) }

                                OutlinedButton(
                                    onClick = { showNotesDialog = true },
                                    modifier = Modifier.align(Alignment.Top),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.labelMedium)
                                }

                                if (showNotesDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showNotesDialog = false },
                                        title = {
                                            Text("${stringResource(R.string.notes_title)} - ${appointment.name}")
                                        },
                                        text = {
                                            Text(
                                                text = appointment.notes,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showNotesDialog = false }) {
                                                Text(stringResource(R.string.close))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Przyciski akcji - stopka
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onMarkCompleted,
                                modifier = Modifier.weight(1f),
                                enabled = !appointment.completed,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (appointment.completed) stringResource(R.string.appointments_completed)
                                    else stringResource(R.string.appointments_mark_as_done)
                                )
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                icon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.appointment_delete_title),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.appointment_delete_message, appointment.name),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        OutlinedButton(onClick = { showDeleteDialog = false }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onDelete()
                                                showDeleteDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text(stringResource(R.string.appointment_delete_confirm))
                                        }
                                    }
                                }
                            )
                        }
                    }
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
