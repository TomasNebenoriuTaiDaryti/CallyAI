package com.example.callyaiandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

sealed class Dest(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Suvestine : Dest("suvestine","Suvestinė", Icons.Outlined.History)
    data object Prideti   : Dest("prideti","Pridėti",   Icons.Outlined.Add)
    data object Profilis  : Dest("profilis","Profilis", Icons.Outlined.Edit)
}
val bottomDests = listOf(Dest.Suvestine, Dest.Prideti, Dest.Profilis)

@Composable
fun AppScaffold(content: @Composable (NavHostController) -> Unit) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val current by nav.currentBackStackEntryAsState()
                val currentRoute = current?.destination?.route
                bottomDests.forEach { d ->
                    NavigationBarItem(
                        selected = currentRoute == d.route,
                        onClick = {
                            nav.navigate(d.route) {
                                launchSingleTop = true
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.title) },
                        label = { Text(d.title) }
                    )
                }
            }
        }
    ) { _ -> content(nav) }
}