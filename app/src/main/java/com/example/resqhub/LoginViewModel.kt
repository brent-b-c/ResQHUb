package com.example.resqhub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

class LoginViewModel : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    // TEMPORARY fake login. Replace with a real backend / local session later.
    fun login(identifier: String, password: String) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            delay(1500) // pretend to talk to a server
            uiState = if (password == DEMO_PASSWORD) {
                uiState.copy(isLoading = false, isLoggedIn = true)
            } else {
                uiState.copy(isLoading = false, error = "Incorrect email or password.")
            }
        }
    }

    fun logout() {
        uiState = LoginUiState()
    }

    private companion object {
        const val DEMO_PASSWORD = "resqhub123"
    }
}