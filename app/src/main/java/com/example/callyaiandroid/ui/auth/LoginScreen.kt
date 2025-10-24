package com.example.callyaiandroid.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.callyaiandroid.auth.AuthViewModel

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    dark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    showToast: (String) -> Unit,
    showSnack: (String) -> Unit
) {
    val st by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    LaunchedEffect(st.generalError) {
        st.generalError?.let { msg ->
            showSnack(msg)
            vm.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.92f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Prisijungimas",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tamsi", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = dark, onCheckedChange = onToggleDark)
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("El. paštas") },
                    isError = st.emailError != null,
                    supportingText = { st.emailError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Slaptažodis") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = st.passError != null,
                    supportingText = { st.passError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { vm.login(email, pass) },
                    enabled = !st.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (st.loading) "Jungiama..." else "Prisijungti")
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRegister) { Text("Neturite paskyros? Registruotis") }
            }
        }
    }
}
