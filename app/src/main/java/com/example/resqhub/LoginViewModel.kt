package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        Supabase.init(app)
        // If the user logged in before, go straight to the dashboard.
        uiState = LoginUiState(isLoggedIn = Supabase.hasSession())
    }

    /** The Supabase accounts use an email address to log in. */
    fun login(identifier: String, password: String) {
        if (uiState.isLoading) return

        if (!identifier.contains("@")) {
            uiState = LoginUiState(error = "Please enter your email address.")
            return
        }

        uiState = LoginUiState(isLoading = true)
        viewModelScope.launch {
            uiState = try {
                Supabase.signIn(identifier, password)
                LoginUiState(isLoggedIn = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                LoginUiState(error = e.message ?: "Login failed.")
            } catch (e: Exception) {
                LoginUiState(error = "Something went wrong. Please try again.")
            }
        }
    }

    fun logout() {
        uiState = LoginUiState()
        viewModelScope.launch { Supabase.signOut() }
    }
}