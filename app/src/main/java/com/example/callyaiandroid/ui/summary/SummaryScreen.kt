package com.example.callyaiandroid.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: SummaryViewModel,
    token: String,
    showSnack: (String) -> Unit
) {
    val st by vm.st.collectAsState()
    var editingItem by remember { mutableStateOf<FoodLogItem?>(null) }

    LaunchedEffect(token) { vm.loadAll(token) }
    LaunchedEffect(st.error) { st.error?.let(showSnack) }
    LaunchedEffect(st.message) {
        st.message?.let {
            showSnack(it)
            vm.clearMessage()
        }
    }

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val selectedDate = st.selectedDate
        val formattedDate = remember(selectedDate) {
            selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        var showPicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochMilli()
        )

        LaunchedEffect(st.selectedDate) {
            datePickerState.selectedDateMillis = st.selectedDate.toEpochMilli()
        }

        if (showPicker) {
            DatePickerDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                vm.selectDate(millis.toLocalDate())
                            }
                            showPicker = false
                        }
                    ) {
                        Text("Patvirtinti")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) {
                        Text("Atšaukti")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Text(
            "Suvestinė",
            style = MaterialTheme.typography.titleLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { vm.selectDate(selectedDate.minusDays(1)) }) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Ankstesnė diena"
                )
            }

            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { showPicker = true }
            ) {
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(onClick = { vm.selectDate(selectedDate.plusDays(1)) }) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Kita diena"
                )
            }
        }
        Text(
            "Dienos kalorijų suma: ${st.overallTotal} kcal",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))
        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
                    val selectedGroup = st.selectedGroup
                    if (selectedGroup == null || selectedGroup.items.isEmpty()) {
                        item("empty-state") {
                            Text(
                                "Pasirinktai dienai įrašų nėra.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {

                        items(
                            items = selectedGroup.items,
                            key = { it.id }
                        ) { item ->
                            ElevatedCard {
                                    Column(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                item.consumedAt.format(timeFormatter),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Text(
                                            "Gramai: ${item.grams} g",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Kiekis: ${item.quantity}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Kalorijos: ${item.portionCalories} kcal",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Iš viso: ${item.totalCalories} kcal",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { editingItem = item }) {
                                                Text("Redaguoti")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                editingItem?.let { item ->
                    var gramsText by remember(item.id) { mutableStateOf(item.grams.toString()) }
                    var quantityText by remember(item.id) { mutableStateOf(item.quantity.toString()) }
                    var gramsError by remember(item.id) { mutableStateOf(false) }
                    var quantityError by remember(item.id) { mutableStateOf(false) }
                    var submitted by remember(item.id) { mutableStateOf(false) }
                    val updating = st.updatingItemId == item.id

                    LaunchedEffect(st.updatingItemId) {
                        if (submitted && st.updatingItemId != item.id) {
                            editingItem = null
                            submitted = false
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { if (!updating) editingItem = null },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val g = gramsText.toIntOrNull()
                                    val q = quantityText.toIntOrNull()
                                    gramsError = g == null || g <= 0
                                    quantityError = q == null || q <= 0
                                    if (!gramsError && !quantityError) {
                                        submitted = true
                                        vm.updateItem(token, item.id, g!!, q!!)
                                    }
                                },
                                enabled = !updating
                            ) {
                                Text(if (updating) "Saugoma..." else "Išsaugoti")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    if (!updating) {
                                        editingItem = null
                                        submitted = false
                                    }
                                }
                            ) { Text("Atšaukti") }
                        },
                        title = { Text("Redaguoti įrašą") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = gramsText,
                                    onValueChange = {
                                        gramsText = it.filter { ch -> ch.isDigit() }
                                        if (gramsError) gramsError = false
                                    },
                                    label = { Text("Gramų kiekis") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = gramsError,
                                    supportingText = if (gramsError) {
                                        { Text("Įveskite teigiamą skaičių") }
                                    } else null
                                )
                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = {
                                        quantityText = it.filter { ch -> ch.isDigit() }
                                        if (quantityError) quantityError = false
                                    },
                                    label = { Text("Porcijų kiekis") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = quantityError,
                                    supportingText = if (quantityError) {
                                        { Text("Įveskite teigiamą skaičių") }
                                    } else null
                                )
                            }
                        }
                    )
                }
            }

            private fun LocalDate.toEpochMilli(zoneId: ZoneId = ZoneId.systemDefault()): Long =
                this.atStartOfDay(zoneId).toInstant().toEpochMilli()

            private fun Long.toLocalDate(): LocalDate =
                Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()