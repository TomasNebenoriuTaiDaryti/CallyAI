package com.example.callyaiandroid.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddFoodScreen(
    vm: AddFoodViewModel,
    token: String,
    showSnack: (String) -> Unit
) {
    val st by vm.st.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(st.message) { st.message?.let { showSnack(it) } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pridėti maistą", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Maisto pavadinimas (pvz., \"apple\")") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { vm.search(token, query) },
            enabled = !st.loading && query.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (st.loading) "Ieškoma..." else "Ieškoti") }

        st.result?.let { res ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(res.name ?: query, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${res.calories} kcal",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    res.unit?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    res.source?.let { Text("Šaltinis: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }

    if (st.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}
