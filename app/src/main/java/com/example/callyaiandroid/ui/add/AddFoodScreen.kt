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
import com.example.callyaiandroid.ui.add.AddFoodMode.CAMERA
import com.example.callyaiandroid.ui.add.AddFoodMode.MANUAL
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsBottomHeight

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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pridėti maistą", style = MaterialTheme.typography.titleLarge)

        SingleChoiceSegmentedButtonRow {
            listOf(MANUAL, CAMERA).forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = st.mode == mode,
                    onClick = { vm.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                ) {
                    Text(if (mode == MANUAL) "Įvesti ranka" else "Fotografuoti")
                }
            }
        }

        when (st.mode) {
            MANUAL -> ManualEntrySection(
                query = query,
                onQueryChange = { query = it },
                state = st,
                onSearch = { vm.search(token, query) },
                onAddLastResult = vm::addLastResultToCart,
                onSetGrams = vm::setGrams,
                onIncQty = vm::incQty,
                onDecQty = vm::decQty,
                onSave = { vm.save(token) },
                openDatePicker = { showDate = true },
                openTimePicker = { showTime = true }
            )

            CAMERA -> CameraEntrySection(
                state = st,
                onStartCapture = vm::startPhotoCapture,
                onSetGrams = vm::setPhotoDraftGrams,
                onIncQty = vm::incPhotoDraftQty,
                onDecQty = vm::decPhotoDraftQty,
                onDiscard = vm::clearPhotoDraft,
                onImport = vm::importPhotoDraftToCart
            )
        }
        Spacer(Modifier.height(40.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars.add(WindowInsets.ime)))
    }

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

    LaunchedEffect(st.message) {
        st.message?.let { showSnack(it) }
    }
}

@Composable
private fun ManualEntrySection(
    query: String,
    onQueryChange: (String) -> Unit,
    state: AddFoodState,
    onSearch: () -> Unit,
    onAddLastResult: () -> Unit,
    onSetGrams: (Int, String) -> Unit,
    onIncQty: (Int) -> Unit,
    onDecQty: (Int) -> Unit,
    onSave: () -> Unit,
    openDatePicker: () -> Unit,
    openTimePicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Maisto pavadinimas (pvz., 'apple')") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSearch) { Text("Ieškoti") }
            Button(onClick = onAddLastResult, enabled = state.result != null) { Text("Į krepšelį") }
        }

        state.result?.let { r ->
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.name ?: "Maistas", style = MaterialTheme.typography.titleMedium)
                    Text("${r.calories} kcal ${r.unit ?: "per 100 g"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "B: ${r.protein.formatAsGrams()} g  R: ${r.fat.formatAsGrams()} g  A: ${r.carbs.formatAsGrams()} g",
                        style = MaterialTheme.typography.bodySmall
                    )
                    r.source?.let { Text("šaltinis: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        if (state.cart.isNotEmpty()) {
            Text("Krepšelis", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.cart) { index, item ->
                    CartItemRow(
                        item = item,
                        onSetGrams = { onSetGrams(index, it) },
                        onIncQty = { onIncQty(index) },
                        onDecQty = { onDecQty(index) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = openDatePicker) {
                    val d = state.consumedAt.toLocalDate()
                    Text("Data: ${d.year}-${"%02d".format(d.monthValue)}-${"%02d".format(d.dayOfMonth)}")
                }
                Button(onClick = openTimePicker) {
                    val t = state.consumedAt.toLocalTime()
                    Text("Laikas: ${"%02d".format(t.hour)}:${"%02d".format(t.minute)}")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Iš viso: ${state.cartTotal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val totalProtein = state.cart.sumOf { it.totalProtein }
                val totalFat = state.cart.sumOf { it.totalFat }
                val totalCarbs = state.cart.sumOf { it.totalCarbs }
                Text(
                    "Baltymai: ${totalProtein.formatAsGrams()} g  Riebalai: ${totalFat.formatAsGrams()} g  Angliavandeniai: ${totalCarbs.formatAsGrams()} g",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onSave,
                enabled = state.cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Išsaugoti") }
        }
    }
}

@Composable
private fun CameraEntrySection(
    state: AddFoodState,
    onStartCapture: () -> Unit,
    onSetGrams: (Int, String) -> Unit,
    onIncQty: (Int) -> Unit,
    onDecQty: (Int) -> Unit,
    onDiscard: () -> Unit,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Nufotografuokite produktą ir sistema atpažins maistą. Prieš įkeldami galėsite pakoreguoti duomenis.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(onClick = onStartCapture, enabled = !state.photoInProgress) {
            Text(if (state.photoInProgress) "Apdorojama..." else "Fotografuoti")
        }

        if (state.photoInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (state.photoDraft.isEmpty()) {
            Text(
                "Kol kas nėra nuskaitytų produktų.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text("Atpažinti produktai", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.photoDraft) { index, item ->
                    CartItemRow(
                        item = item,
                        onSetGrams = { onSetGrams(index, it) },
                        onIncQty = { onIncQty(index) },
                        onDecQty = { onDecQty(index) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDiscard) { Text("Išvalyti") }
                Button(onClick = onImport) { Text("Pridėti į krepšelį") }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onSetGrams: (String) -> Unit,
    onIncQty: () -> Unit,
    onDecQty: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${item.caloriesPer100g} kcal per 100 g",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "B: ${item.perServingProtein.formatAsGrams()} g  R: ${item.perServingFat.formatAsGrams()} g  A: ${item.perServingCarbs.formatAsGrams()} g",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = item.grams.toString(),
                    onValueChange = onSetGrams,
                    label = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp)
                )
                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecQty) {
                        Icon(Icons.Outlined.Remove, contentDescription = "minus")
                    }
                    Text(
                        text = "${item.qty}",
                        modifier = Modifier.width(24.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onIncQty) {
                        Icon(Icons.Outlined.Add, contentDescription = "plus")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${item.totalKcal} kcal", fontWeight = FontWeight.Bold)
                Text(
                    "B:${item.totalProtein.formatAsGrams()} g R:${item.totalFat.formatAsGrams()} g A:${item.totalCarbs.formatAsGrams()} g",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun Double.formatAsGrams(): String = String.format("%.1f", this)
