package com.example.callyaiandroid.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    vm: AddFoodViewModel,
    token: String,
    showSnack: (String) -> Unit = {}
) {
    val st by vm.st.collectAsState()

    var query by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Paieška
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Maisto pavadinimas (pvz., 'apple')") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.search(token, query) }) { Text("Ieškoti") }
            Button(
                onClick = { vm.addLastResultToCart() },
                enabled = st.result != null
            ) { Text("Į krepšelį") }
        }

        st.result?.let { r ->
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.name ?: "Maistas", style = MaterialTheme.typography.titleMedium)
                    Text("${r.calories} kcal ${r.unit ?: "per 100 g"}", style = MaterialTheme.typography.bodyMedium)
                    r.source?.let { Text("šaltinis: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        // Krepšelis
        if (st.cart.isNotEmpty()) {
            Text("Krepšelis", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(st.cart) { index, item ->
                    ElevatedCard {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.caloriesPer100g} kcal per 100 g",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // gramų redagavimas
                            OutlinedTextField(
                                value = item.grams.toString(),
                                onValueChange = { vm.setGrams(index, it) },
                                label = { Text("g") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(88.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            // Kiekis +/- (jei qty==1 ir spaudžiam -, ištrins)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.decQty(index) }) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "minus")
                                }
                                Text(
                                    text = "${item.qty}",
                                    modifier = Modifier.width(24.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { vm.incQty(index) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = "plus")
                                }
                            }
                            Spacer(Modifier.width(12.dp))

                            // Bendra kcal (už grams * qty)
                            Text("${item.totalKcal} kcal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Data / laikas + suvestinė + Išsaugoti
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDate = true }) {
                    val d = st.consumedAt.toLocalDate()
                    Text("Data: ${d.year}-${"%02d".format(d.monthValue)}-${"%02d".format(d.dayOfMonth)}")
                }
                Button(onClick = { showTime = true }) {
                    val t = st.consumedAt.toLocalTime()
                    Text("Laikas: ${"%02d".format(t.hour)}:${"%02d".format(t.minute)}")
                }
            }

            Text(
                "Iš viso: ${st.cartTotal} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = { vm.save(token) },
                enabled = st.cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Išsaugoti") }
        }
    }

    // Date picker
    if (showDate) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        vm.setDate(date)
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // Time picker (Material3)
    if (showTime) {
        val initial = st.consumedAt.toLocalTime()
        val tp = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTime(LocalTime.of(tp.hour, tp.minute))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = tp) }
        )
    }

    if (st.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }

    // Žinutės
    LaunchedEffect(st.message) {
        st.message?.let { showSnack(it) }
    }
}
