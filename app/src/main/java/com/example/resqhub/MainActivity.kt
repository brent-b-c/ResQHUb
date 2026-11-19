package com.example.resqhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val loginViewModel: LoginViewModel = viewModel()
                val state = loginViewModel.uiState

                if (state.isLoggedIn) {
                    DashboardScreen(onLogout = loginViewModel::logout)
                } else {
                    LoginScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.error,
                        onLogin = loginViewModel::login,
                    )
                }
            }
        }
    }
}