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
fun RegisterScreen(
    vm: AuthViewModel,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    showSnack: (String) -> Unit
) {
    val st by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("2000") }

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
                Text("Registracija", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Vardas") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("El. paštas") }, singleLine = true,
                    isError = st.emailError != null,
                    supportingText = { st.emailError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it },
                    label = { Text("Slaptažodis") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = st.passError != null,
                    supportingText = { st.passError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass2, onValueChange = { pass2 = it },
                    label = { Text("Pakartokite slaptažodį") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kcal, onValueChange = { kcal = it.filter(Char::isDigit) },
                    label = { Text("Dienos tikslas (kcal)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (pass != pass2) {
                            vm.setError("Slaptažodžiai nesutampa")
                        } else {
                            vm.register(name, email, pass, pass2, kcal.toIntOrNull() ?: 2000)
                        }
                    },
                    enabled = !st.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (st.loading) "Kuriama..." else "Registruotis") }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBackToLogin) { Text("Jau turite paskyrą? Prisijungti") }
            }
        }
    }
}
