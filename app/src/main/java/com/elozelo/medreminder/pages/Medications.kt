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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.DosageUnit
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import com.elozelo.medreminder.ui.components.AddMedicationDialog
import com.elozelo.medreminder.ui.components.formatDate
import com.elozelo.medreminder.ui.theme.MedReminderTheme
import com.elozelo.medreminder.viewmodel.MedicationViewModel
import java.util.*

@Composable
fun MedicationsPage(
    viewModel: MedicationViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues()
) {
    val medications by viewModel.medications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }
    var showEmpty by remember { mutableStateOf(false) }

    // Filtrowanie i sortowanie leków
    val filteredMedications = remember(medications, showEmpty) {
        if (showEmpty) {
            // Puste - leki ze stanem 0, sortowane alfabetycznie
            medications.filter { it.remainingQuantity == 0 }
                .sortedBy { it.name.lowercase() }
        } else {
            // Aktualne - leki z zapasem, sortowane po najbliższej przyszłej godzinie przypomnienia
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            medications.filter { it.remainingQuantity > 0 }
                .sortedBy { med ->
                    if (med.reminderTimes.isEmpty()) {
                        return@sortedBy Int.MAX_VALUE
                    }

                    // Jeśli jest lastTakenTime, znajdź następną godzinę po niej
                    // W przeciwnym razie znajdź następną godzinę po aktualnym czasie
                    val lastTakenMinutes = med.lastTakenTime?.let { timeString ->
                        val parts = timeString.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: return@let null
                            val minute = parts[1].toIntOrNull() ?: return@let null
                            hour * 60 + minute
                        } else null
                    }

                    val referenceTime = lastTakenMinutes ?: currentTimeInMinutes

                    // Znajdź najbliższą przyszłą godzinę
                    val nextReminderTime = med.reminderTimes
                        .mapNotNull { timeString ->
                            val parts = timeString.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                                hour * 60 + minute
                            } else null
                        }
                        .filter { it > referenceTime } // Tylko godziny po ostatnim wzięciu/aktualnym czasie
                        .minOrNull()

                    // Jeśli jest przyszła godzina dzisiaj, użyj jej
                    // Jeśli nie, użyj pierwszej godziny z jutrzejszego dnia (dodaj 24h)
                    nextReminderTime ?: (med.reminderTimes
                        .mapNotNull { timeString ->
                            val parts = timeString.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                                hour * 60 + minute + (24 * 60) // Dodaj 24h
                            } else null
                        }
                        .minOrNull() ?: Int.MAX_VALUE)
                }
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
                    text = stringResource(R.string.medications_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Przycisk Puste/Aktualne
                OutlinedButton(
                    onClick = { showEmpty = !showEmpty },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (showEmpty) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (showEmpty) Icons.Default.Inventory2 else Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showEmpty) stringResource(R.string.medications_current) else stringResource(R.string.medications_empty))
                }
            }

            if (isLoading && medications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMedications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showEmpty) stringResource(R.string.medications_no_empty) else stringResource(R.string.medications_no_added),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMedications) { medication ->
                        MedicationItem(
                            medication = medication,
                            onDelete = { viewModel.deleteMedication(medication.id) },
                            onClick = { medicationToEdit = medication },
                            onMarkTaken = { viewModel.markMedicationTaken(medication) },
                            onRefill = { viewModel.refillMedication(medication) },
                            onToggleNotifications = { med ->
                                val updatedMedication = if (med.reminderTimes.isNotEmpty()) {
                                    med.copy(reminderTimes = emptyList())
                                } else {
                                    med.copy(reminderTimes = listOf("08:00"))
                                }
                                viewModel.updateMedication(updatedMedication)
                            },
                            isEmpty = showEmpty
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
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.medications_add), tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { medication ->
                viewModel.addMedication(medication)
                showAddDialog = false
            }
        )
    }

    medicationToEdit?.let { medication ->
        AddMedicationDialog(
            medication = medication,
            onDismiss = { medicationToEdit = null },
            onConfirm = { updatedMedication ->
                viewModel.updateMedication(updatedMedication)
                medicationToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationItem(
    medication: Medication,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkTaken: () -> Unit = {},
    onRefill: () -> Unit = {},
    onToggleNotifications: (Medication) -> Unit = {},
    isEmpty: Boolean = false
) {
    val context = LocalContext.current
    val percentageRemaining = if (medication.initialQuantity > 0) {
        (medication.remainingQuantity.toFloat() / medication.initialQuantity.toFloat()) * 100
    } else 100f
    val isLowStock = percentageRemaining < 15f && medication.remainingQuantity > 0

    // Kolor paska bocznego zależny od stanu
    val accentColor = when {
        isLowStock -> MaterialTheme.colorScheme.error
        isEmpty -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    // Stan rozwinięcia karty
    var isExpanded by remember { mutableStateOf(false) }

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
                    .background(accentColor)
                    .align(Alignment.CenterVertically)
                    .then(
                        if (isExpanded) Modifier.fillMaxHeight()
                        else Modifier.height(56.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                // Nagłówek - klikalny do zwijania/rozwijania
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
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isLowStock) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Ikony po prawej stronie
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ikona powiadomień (klikalny dzwonek)
                        var showDisableDialog by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                if (medication.reminderTimes.isNotEmpty()) {
                                    showDisableDialog = true
                                } else {
                                    onToggleNotifications(medication)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (medication.reminderTimes.isNotEmpty()) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (medication.reminderTimes.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Ikona strzałki
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        // Dialog potwierdzenia
                        if (showDisableDialog) {
                            AlertDialog(
                                onDismissRequest = { showDisableDialog = false },
                                icon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                title = { Text(stringResource(R.string.medication_notifications_disable_title)) },
                                text = { Text(stringResource(R.string.medication_notifications_disable_message, medication.name)) },
                                confirmButton = {
                                    Button(onClick = {
                                        onToggleNotifications(medication)
                                        showDisableDialog = false
                                    }) {
                                        Text(stringResource(R.string.medication_notifications_disable_confirm))
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showDisableDialog = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                    }
                }

                // Sekcja środkowa - zwijalna, klikalna do edycji
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
                            // Lewa kolumna - informacje
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Dawka
                                InfoRow(
                                    icon = Icons.Default.Medication,
                                    text = "${medication.quantity} ${medication.dosage.getLocalizedName(context)}",
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Stan magazynowy
                                if (medication.initialQuantity > 0) {
                                    InfoRow(
                                        icon = Icons.Default.Inventory,
                                        text = "${medication.remainingQuantity}/${medication.initialQuantity}",
                                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Częstotliwość
                                InfoRow(
                                    icon = Icons.Default.EventRepeat,
                                    text = medication.frequency.getLocalizedName(context),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Data końcowa
                                medication.endDate?.let {
                                    InfoRow(
                                        icon = Icons.Default.CalendarToday,
                                        text = "${stringResource(R.string.medications_to)} ${formatDate(it)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Godziny przypomnień
                                if (medication.reminderTimes.isNotEmpty()) {
                                    InfoRow(
                                        icon = Icons.Default.Alarm,
                                        text = medication.reminderTimes.joinToString(", "),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Prawa strona - notatki
                            if (!medication.notes.isNullOrBlank()) {
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
                                            Text("${stringResource(R.string.notes_title)} - ${medication.name}")
                                        },
                                        text = {
                                            Text(
                                                text = medication.notes,
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
                            if (isEmpty) {
                                Button(
                                    onClick = onRefill,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.medications_refill))
                                }
                            } else {
                                Button(
                                    onClick = onMarkTaken,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.medications_mark_taken))
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Dialog potwierdzenia usunięcia
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
                                    Text(stringResource(R.string.medication_delete_title))
                                },
                                text = {
                                    Text(stringResource(R.string.medication_delete_message, medication.name))
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDelete()
                                            showDeleteDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(stringResource(R.string.medication_delete_confirm))
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                                        Text(stringResource(R.string.cancel))
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

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationItemPreview() {
    MedReminderTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            MedicationItem(
                medication = Medication(
                    name = "Witamina D",
                    dosage = DosageUnit.PILLS,
                    quantity = 1,
                    frequency = frequencyUnit.DAILY,
                    endDate = Date(),
                    reminderTimes = listOf("09:00", "21:00")
                ),
                onDelete = {},
                onClick = {}
            )
            MedicationItem(
                medication = Medication(
                    name = "Paracetamol",
                    dosage = DosageUnit.PILLS,
                    quantity = 2,
                    frequency = frequencyUnit.WEEKLY,
                    endDate = null,
                    reminderTimes = emptyList()
                ),
                onDelete = {},
                onClick = {}
            )
        }
    }
}
