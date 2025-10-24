package com.example.callyaiandroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.callyaiandroid.auth.AuthViewModel
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.ui.AppScaffold
import com.example.callyaiandroid.ui.Dest
import com.example.callyaiandroid.ui.auth.LoginScreen
import com.example.callyaiandroid.ui.auth.RegisterScreen
import com.example.callyaiandroid.ui.profile.ProfileScreen
import com.example.callyaiandroid.ui.profile.ProfileViewModel
import com.example.callyaiandroid.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs(this)
        val authVm = AuthViewModel(prefs)

        setContent {
            val scope = rememberCoroutineScope()

            val snackHost = remember { SnackbarHostState() }
            val showSnack: (String) -> Unit = { msg ->
                scope.launch { snackHost.showSnackbar(msg) }
            }

            val theme by prefs.themeFlow.collectAsState(initial = "light")
            val dark = theme == "dark"
            val setDark: (Boolean) -> Unit = { wantDark ->
                scope.launch { prefs.setTheme(if (wantDark) "dark" else "light") }
            }

            val token by prefs.tokenFlow.collectAsState(initial = null)
            var loggedIn by remember { mutableStateOf(false) }
            LaunchedEffect(token) { loggedIn = !token.isNullOrBlank() }

            var authScreen by remember { mutableStateOf("login") }

            LaunchedEffect(token) {
                if (!token.isNullOrBlank()) {
                    if (authScreen == "register") showSnack("Paskyra sukurta")
                    else showSnack("Sėkmingai prisijungta")
                }
            }

            AppTheme(darkTheme = dark) {
                Scaffold(snackbarHost = { SnackbarHost(snackHost) }) { padding ->

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {

                        if (!loggedIn) {
                            if (authScreen == "login") {
                                LoginScreen(
                                    vm = authVm,
                                    dark = dark,
                                    onToggleDark = setDark,
                                    onSuccess = { },
                                    onRegister = { authScreen = "register" },
                                    showToast = { msg ->
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    showSnack = showSnack
                                )
                            } else {
                                RegisterScreen(
                                    vm = authVm,
                                    onSuccess = {  },
                                    onBackToLogin = { authScreen = "login" },
                                    showSnack = showSnack
                                )
                            }

                        } else {
                            AppScaffold { nav ->
                                NavHost(
                                    navController = nav,
                                    startDestination = Dest.Suvestine.route
                                ) {
                                    composable(Dest.Suvestine.route) {
                                        Text("Suvestinė – čia rodysime dienos kalorijų tikslą")
                                    }
                                    composable(Dest.Prideti.route) {
                                        Text("Pridėti – čia bus AI atpažinimas ir kamera 📷")
                                    }
                                    composable(Dest.Profilis.route) {
                                        val vmProf = remember { ProfileViewModel(prefs) }
                                        val currentToken = token

                                        if (currentToken.isNullOrBlank()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Atsijungta...")
                                            }
                                        } else {
                                            ProfileScreen(
                                                vm = vmProf,
                                                prefs = prefs,
                                                token = currentToken,
                                                onLoggedOut = { showSnack("Sėkmingai atsijungta") },
                                                showSnack = showSnack
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
