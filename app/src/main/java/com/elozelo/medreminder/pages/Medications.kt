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
                        if (showEmpty) "Brak pustych leków." else "Brak dodanych leków.",
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
            Icon(Icons.Default.Add, contentDescription = "Dodaj lek", tint = MaterialTheme.colorScheme.onPrimary)
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
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val percentageRemaining = if (medication.initialQuantity > 0) {
        (medication.remainingQuantity.toFloat() / medication.initialQuantity.toFloat()) * 100
    } else 100f
    val isLowStock = percentageRemaining < 15f && medication.remainingQuantity > 0

    // Stan rozwinięcia karty
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column {
            // Nagłówek - klikalny do zwijania/rozwijania
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = medication.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.CenterStart).padding(end = 120.dp)
                )

                // Ikony po prawej stronie
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ostrzeżenie o niskim stanie
                    if (isLowStock) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Lek się kończy!",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Ikona powiadomień (klikalny dzwonek)
                    var showDisableDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            if (medication.reminderTimes.isNotEmpty()) {
                                showDisableDialog = true
                            } else {
                                onToggleNotifications(medication)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (medication.reminderTimes.isNotEmpty()) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = if (medication.reminderTimes.isNotEmpty()) "Wyłącz powiadomienia" else "Włącz powiadomienia",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Ikona strzałki
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )

                    // Dialog potwierdzenia
                    if (showDisableDialog) {
                        AlertDialog(
                            onDismissRequest = { showDisableDialog = false },
                            icon = {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            title = { Text("Wyłączyć powiadomienia?") },
                            text = { Text("Czy na pewno chcesz wyłączyć powiadomienia dla leku \"${medication.name}\"?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onToggleNotifications(medication)
                                    showDisableDialog = false
                                }) {
                                    Text("Tak, wyłącz")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDisableDialog = false }) {
                                    Text("Anuluj")
                                }
                            }
                        )
                    }
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vaccines, contentDescription = "Dawka", tint = iconColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "${medication.quantity} ${medication.dosage.getLocalizedName(context)}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }

                        if (medication.initialQuantity > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory, contentDescription = "Stan zapasów", tint = if (isLowStock) MaterialTheme.colorScheme.error else iconColor, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "${medication.remainingQuantity}/${medication.initialQuantity} ${medication.dosage.getLocalizedName(context)}", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (isLowStock) MaterialTheme.colorScheme.error else Color.Unspecified)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventRepeat, contentDescription = "Częstotliwość", tint = iconColor, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = medication.frequency.getLocalizedName(context), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                        }

                        medication.endDate?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Data zakończenia", tint = iconColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "${stringResource(R.string.medications_to)} ${formatDate(it)}", fontSize = 15.sp)
                            }
                        }

                        if (medication.reminderTimes.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, contentDescription = "Godziny przypomnień", tint = iconColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = medication.reminderTimes.joinToString(", "), fontSize = 15.sp)
                            }
                        }
                    }

                    // Prawa strona - ikona i notatki
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (medication.dosage) {
                                DosageUnit.PILLS, DosageUnit.TABLETS -> Icons.Default.Medication
                                DosageUnit.CAPSULES -> Icons.Default.MedicalServices
                                DosageUnit.ML -> Icons.Default.WaterDrop
                                else -> Icons.Default.Medication
                            },
                            contentDescription = medication.dosage.getLocalizedName(context),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(120.dp)
                        )

                        if (!medication.notes.isNullOrBlank()) {
                            var showNotesDialog by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showNotesDialog = true },
                                modifier = Modifier.width(120.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Notatki", fontSize = 12.sp)
                            }

                            if (showNotesDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNotesDialog = false },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Notes, contentDescription = null)
                                            Text("Notatki - ${medication.name}")
                                        }
                                    },
                                    text = { Text(text = medication.notes, style = MaterialTheme.typography.bodyLarge) },
                                    confirmButton = {
                                        TextButton(onClick = { showNotesDialog = false }) { Text("Zamknij") }
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
                if (isEmpty) {
                    OutlinedButton(
                        onClick = onRefill,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Uzupełnij", fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onMarkTaken,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Wzięto", fontSize = 16.sp)
                    }
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
