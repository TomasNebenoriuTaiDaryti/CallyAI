package com.example.callyaiandroid.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SummaryScreen(
    vm: SummaryViewModel,
    token: String,
    showSnack: (String) -> Unit
) {
    val st by vm.st.collectAsState()

    LaunchedEffect(token) { vm.loadAll(token) }
    LaunchedEffect(st.error) { st.error?.let(showSnack) }

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Suvestinė",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Iš viso: ${st.overallTotal} kcal",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            st.groups.forEach { group ->
                item(key = "header-${group.date}") {
                    val d = group.date
                    Text(
                        "${"%02d".format(d.monthValue)}/${"%02d".format(d.dayOfMonth)}  —  ${group.dayTotal} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(
                    items = group.items,
                    key = { it.id }
                ) { item ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            val t = item.consumedAt.toLocalTime()
                            Text(
                                "${item.name}  (${ "%02d".format(t.hour) }:${ "%02d".format(t.minute) })",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Kiekis: ${item.quantity}")
                            Text("Kalorijos: ${item.calories} × ${item.quantity} = ${item.total} kcal")
                        }
                    }
                }
                item(key = "space-${group.date}") { Spacer(Modifier.height(4.dp)) }
            }
        }
    }
}
