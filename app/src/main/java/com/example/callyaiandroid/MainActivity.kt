package com.example.callyaiandroid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.ui.theme.CallyAIAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var users by remember { mutableStateOf<List<com.example.callyaiandroid.model.User>>(emptyList()) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.getUsers()
                    }
                    users = result
                    Log.d("API_RESPONSE", "Users: $users")
                } catch (e: Exception) {
                    error = e.toString()
                    Log.e("API_ERROR", e.toString())
                }
            }

            CallyAIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Greeting(name = "Android")
                        Text(text = "Users fetched: ${users.size}")
                        error?.let { Text(text = "Error: $it") }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CallyAIAndroidTheme {
        Greeting("Android")
    }
}
