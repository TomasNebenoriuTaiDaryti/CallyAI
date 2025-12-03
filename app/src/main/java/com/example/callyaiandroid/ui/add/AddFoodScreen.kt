package com.example.callyaiandroid.ui.add

import androidx.compose.foundation.layout.*
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.callyaiandroid.ui.add.AddFoodMode.CAMERA
import com.example.callyaiandroid.ui.add.AddFoodMode.MANUAL
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                //onStartCapture = vm::startPhotoCapture,
                onImageCaptured = { vm.analyzePhoto(token, it) },
                onPhotoError = vm::onPhotoError,
                onSetGrams = vm::setPhotoDraftGrams,
                onIncQty = vm::incPhotoDraftQty,
                onDecQty = vm::decPhotoDraftQty,
                onDiscard = vm::clearPhotoDraft,
                onSave = { vm.savePhotoDraft(token) },
                openDatePicker = { showDate = true },
                openTimePicker = { showTime = true }
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
    //onStartCapture: () -> Unit,
    onImageCaptured: (ByteArray) -> Unit,
    onPhotoError: (String) -> Unit,
    onSetGrams: (Int, String) -> Unit,
    onIncQty: (Int) -> Unit,
    onDecQty: (Int) -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    openDatePicker: () -> Unit,
    openTimePicker: () -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { bitmap.prepareForUpload().toJpegByteArray() }
                if (bytes != null && bytes.isNotEmpty()) {
                    onImageCaptured(bytes)
                } else {
                    onPhotoError("Nepavyko apdoroti nuotraukos")
                }
            }
        } else if (!state.photoInProgress) {
            onPhotoError("Nuotrauka nebuvo padaryta")
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { uri.toCompressedBytes(context) }
                if (bytes != null && bytes.isNotEmpty()) {
                    onImageCaptured(bytes)
                } else {
                    onPhotoError("Nepavyko nuskaityti nuotraukos")
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingAction?.invoke()
        } else {
            onPhotoError("Kameros leidimas atmestas")
        }
        pendingAction = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Nufotografuokite produktą ir sistema atpažins maistą. Prieš įkeldami galėsite pakoreguoti duomenis.",
            style = MaterialTheme.typography.bodyMedium
        )

        /*Button(onClick = onStartCapture, enabled = !state.photoInProgress) {
            Text(if (state.photoInProgress) "Apdorojama..." else "Fotografuoti")
        }*/
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (state.photoInProgress) return@Button
                    val permission = Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        takePhotoLauncher.launch(null)
                    } else {
                        pendingAction = { takePhotoLauncher.launch(null) }
                        permissionLauncher.launch(permission)
                    }
                },
                enabled = !state.photoInProgress
            ) {
                Text(if (state.photoInProgress) "Apdorojama..." else "Nufotografuoti")
            }

            OutlinedButton(
                onClick = {
                    if (state.photoInProgress) return@OutlinedButton
                    pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !state.photoInProgress
            ) {
                Text("Pasirinkti nuotrauką")
            }
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
            Text("Atpažintas produktas", style = MaterialTheme.typography.titleMedium)
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
                    val totalKcal = state.photoDraft.sumOf { it.totalKcal }
                    Text(
                        "Iš viso: $totalKcal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val totalProtein = state.photoDraft.sumOf { it.totalProtein }
                    val totalFat = state.photoDraft.sumOf { it.totalFat }
                    val totalCarbs = state.photoDraft.sumOf { it.totalCarbs }
                    Text(
                        "Baltymai: ${totalProtein.formatAsGrams()} g  Riebalai: ${totalFat.formatAsGrams()} g  Angliavandeniai: ${totalCarbs.formatAsGrams()} g",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDiscard) { Text("Išvalyti") }
                    Button(onClick = onSave) { Text("Išsaugoti") }
                }
            }
        }
    }
}

private fun Bitmap.prepareForUpload(maxDimension: Int = 1024): Bitmap {
    val maxSide = maxOf(width, height)
    if (maxSide <= maxDimension) return this
    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.toJpegByteArray(): ByteArray? {
    return ByteArrayOutputStream().use { stream ->
        if (!compress(Bitmap.CompressFormat.JPEG, 90, stream)) return null
        stream.toByteArray()
    }
}

private fun Uri.toCompressedBytes(context: android.content.Context): ByteArray? {
    return context.contentResolver.openInputStream(this)?.use { input ->
        val original = BitmapFactory.decodeStream(input) ?: return null
        original.prepareForUpload().toJpegByteArray()
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
