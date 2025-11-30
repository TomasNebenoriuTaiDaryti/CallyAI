package com.example.callyaiandroid.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.notifications.CalorieNotificationScheduler
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.rememberUpdatedState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    prefs: Prefs,
    token: String,
    onLoggedOut: () -> Unit,
    showSnack: (String) -> Unit
) {
    val st by vm.st.collectAsState()
    val theme by prefs.themeFlow.collectAsState(initial = "light")
    val dark = theme == "dark"
    val notificationsEnabled by prefs.notificationsEnabledFlow.collectAsState(initial = false)
    val notificationIntervalHours by prefs.notificationIntervalHoursFlow.collectAsState(initial = 3)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasNotificationPermission by remember { mutableStateOf(CalorieNotificationScheduler.hasPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = CalorieNotificationScheduler.hasPermission(context)
        scope.launch {
            if (granted) {
                prefs.setNotificationsEnabled(true)
                CalorieNotificationScheduler.schedule(context, notificationIntervalHours)
                showSnack("Kalorijų priminimai įjungti")
            } else {
                prefs.setNotificationsEnabled(false)
                showSnack("Priminimams reikia leisti pranešimus")
            }
        }
    }

    LaunchedEffect(Unit) {
        hasNotificationPermission = CalorieNotificationScheduler.hasPermission(context)
    }

    val latestInterval by rememberUpdatedState(notificationIntervalHours)

    LaunchedEffect(notificationsEnabled, hasNotificationPermission, latestInterval) {
        if (notificationsEnabled && hasNotificationPermission) {
            CalorieNotificationScheduler.schedule(context, latestInterval)
        }
    }

    LaunchedEffect(token) { vm.load(token) }

    var confirmLogout by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    if (st.user == null && st.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val user = st.user ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nepavyko įkelti profilio") }
        return
    }

    var name by remember(user) { mutableStateOf(user.name) }
    var email by remember(user) { mutableStateOf(user.email) }
    var kcal by remember(user) { mutableStateOf((user.dailyCalories ?: 2000).toString()) }
    var goal by remember(user) { mutableStateOf("maintain") }
    var gender by remember(user) { mutableStateOf("female") }
    var activityLevel by remember(user) { mutableStateOf("none") }
    var weightInput by remember(user) { mutableStateOf("") }
    var heightInput by remember(user) { mutableStateOf("") }

    LaunchedEffect(st.message) {
        st.message?.let { showSnack(it) }
    }

    LaunchedEffect(st.calcCalories) {
        st.calcCalories?.let { calculated ->
            kcal = calculated.toString()
        }
    }

    val scrollState = rememberScrollState()
    val notificationOn = notificationsEnabled && hasNotificationPermission
    val frequencyOptions = listOf(1, 3, 6, 12, 24)

    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding().imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profilis", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val initial = remember(user.name) {
                    user.name.trim().takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
                }
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initial,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Vardas") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("El. paštas") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Redaguoti")
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Tamsi tema") },
                    supportingContent = { Text(if (dark) "Įjungta" else "Išjungta") },
                    trailingContent = {
                        Switch(checked = dark, onCheckedChange = { wantDark ->
                            scope.launch { prefs.setTheme(if (wantDark) "dark" else "light") }
                            showSnack(if (wantDark) "Įjungta tamsi tema" else "Išjungta tamsi tema")
                        })
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Kalorijų priminimai") },
                    supportingContent = { Text(if (notificationOn) "Įjungta" else "Išjungta") },
                    trailingContent = {
                        Switch(
                            checked = notificationOn,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (CalorieNotificationScheduler.hasPermission(context)) {
                                        hasNotificationPermission = true
                                        scope.launch { prefs.setNotificationsEnabled(true) }
                                        CalorieNotificationScheduler.schedule(context, notificationIntervalHours)
                                        showSnack("Kalorijų priminimai įjungti")
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            hasNotificationPermission = true
                                            scope.launch { prefs.setNotificationsEnabled(true) }
                                            CalorieNotificationScheduler.schedule(context, notificationIntervalHours)
                                            showSnack("Kalorijų priminimai įjungti")
                                        }
                                    }
                                } else {
                                    scope.launch { prefs.setNotificationsEnabled(false) }
                                    CalorieNotificationScheduler.cancel(context)
                                    showSnack("Kalorijų priminimai išjungti")
                                }
                            }
                        )
                    }
                )
                Divider()
                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = if (notificationIntervalHours == 1) "Kas valandą" else "Kas ${notificationIntervalHours} val."
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    ListItem(
                        modifier = Modifier.menuAnchor(),
                        headlineContent = { Text("Priminimų dažnis") },
                        supportingContent = { Text(selectedLabel) },
                        trailingContent = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        overlineContent = { Text("Pranešimai siunčiami tik iki kol pasiekiamas dienos tikslas") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        frequencyOptions.forEach { hours ->
                            DropdownMenuItem(
                                text = { Text(if (hours == 1) "Kas valandą" else "Kas $hours val.") },
                                onClick = {
                                    expanded = false
                                    scope.launch { prefs.setNotificationIntervalHours(hours) }
                                    if (notificationOn) {
                                        CalorieNotificationScheduler.schedule(context, hours)
                                        showSnack("Dažnis atnaujintas")
                                    }
                                }
                            )
                        }
                    }
                }
                Divider()
                ListItem(
                    headlineContent = { Text("Dienos tikslas (kcal)") },
                    supportingContent = {
                        OutlinedTextField(
                            value = kcal,
                            onValueChange = { kcal = it.filter(Char::isDigit) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Apskaičiuokite dienos kalorijas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Pasirinkite tikslą ir įveskite savo dabartinį svorį bei ūgį. Dirbtinis intelektas pasiūlys dienos kalorijų tikslą.",
                    style = MaterialTheme.typography.bodySmall
                )

                val options = listOf(
                    "lose" to "Mažinti svorį",
                    "maintain" to "Išlaikyti svorį",
                    "gain" to "Priaugti svorio"
                )

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { (value, label) ->
                        FilterChip(
                            selected = goal == value,
                            onClick = { goal = value },
                            label = { Text(label) },
                            leadingIcon = if (goal == value) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Text(
                    "Lytis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                val genderOptions = listOf(
                    "female" to "Moteris",
                    "male" to "Vyras"
                )

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genderOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = gender == value,
                            onClick = { gender = value },
                            label = { Text(label) },
                            leadingIcon = if (gender == value) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Text(
                    "Aktyvumo lygis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                val activityOptions = listOf(
                    "none" to "Be sporto",
                    "moderate" to "Vidutinis aktyvumas (1-2 k./sav.)",
                    "active" to "Aktyvus sportas (3-5 k./sav.)",
                    "very_active" to "Labai aktyvus (6+ k./sav.)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activityOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = activityLevel == value,
                            onClick = { activityLevel = value },
                            label = { Text(label) },
                            leadingIcon = if (activityLevel == value) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { raw ->
                        val sanitized = raw.replace(',', '.')
                        if (sanitized.count { it == '.' } <= 1) {
                            weightInput = sanitized.filter { it.isDigit() || it == '.' }
                        }
                    },
                    label = { Text("Svoris (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { raw ->
                        val sanitized = raw.replace(',', '.')
                        if (sanitized.count { it == '.' } <= 1) {
                            heightInput = sanitized.filter { it.isDigit() || it == '.' }
                        }
                    },
                    label = { Text("Ūgis (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val weight = weightInput.toDoubleOrNull()
                        if (weight == null || weight <= 0) {
                            showSnack("Įveskite teisingą svorį")
                            return@Button
                        }
                        val height = heightInput.toDoubleOrNull()
                        if (height == null || height <= 0) {
                            showSnack("Įveskite teisingą ūgį")
                            return@Button
                        }
                        vm.calculateDailyCalories(token, goal, weight, height, gender, activityLevel)
                    },
                    enabled = !st.calcLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (st.calcLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Skaičiuojama...")
                    } else {
                        Text("Apskaičiuoti kalorijas")
                    }
                }

                st.calcCalories?.let { target ->
                    Text(
                        "Rekomenduojamas tikslas: $target kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("Versija") }, supportingContent = { Text("0.0.1-Beta") })
                Divider()
                ListItem(headlineContent = { Text("Platforma") }, supportingContent = { Text("Android") })
            }
        }

        Button(
            onClick = {
                if (name.isBlank()) { showSnack("Laukas privalomas"); return@Button }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showSnack("Neteisingas el. paštas"); return@Button
                }
                val updated = user.copy(
                    name = name,
                    email = email,
                    dailyCalories = kcal.toIntOrNull() ?: 2000
                )
                scope.launch {
                    try {
                        vm.save(token, updated)
                        isEditing = false
                    } catch (e: HttpException) {
                        val msg = e.response()?.errorBody()?.string()?.let { json ->
                            try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                        } ?: "Nepavyko atnaujinti"
                        showSnack(msg)
                    } catch (_: Exception) {
                        showSnack("Nepavyko atnaujinti")
                    }
                }
            },
            enabled = !st.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (st.loading) "Saugoma..." else "Išsaugoti") }

        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Atsijungti")
        }
        Spacer(Modifier.height(48.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars.add(WindowInsets.ime)))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    vm.logout(token) {
                        onLoggedOut()
                    }
                }) { Text("Taip") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Ne") } },
            title = { Text("Patvirtinimas") },
            text = { Text("Ar tikrai atsijungti?") }
        )
    }
}
