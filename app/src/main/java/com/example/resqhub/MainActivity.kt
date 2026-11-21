package com.example.resqhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val loginViewModel: LoginViewModel = viewModel()
                val state = loginViewModel.uiState
                var showSignUp by rememberSaveable { mutableStateOf(false) }

                when {
                    state.isLoggedIn -> DashboardScreen(onLogout = loginViewModel::logout)

                    showSignUp -> SignUpScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.error,
                        infoMessage = state.info,
                        onSignUp = loginViewModel::signUp,
                        onBack = {
                            loginViewModel.clearMessages()
                            showSignUp = false
                        },
                    )

                    else -> LoginScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.error,
                        onLogin = loginViewModel::login,
                        onCreateAccount = {
                            loginViewModel.clearMessages()
                            showSignUp = true
                        },
                    )
                }
            }
        }
    }
}