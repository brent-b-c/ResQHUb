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
    val info: String? = null,      // a friendly message (not an error)
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

    /** Creates a citizen account and, if allowed, logs straight in. */
    fun signUp(fullName: String, phone: String, email: String, password: String) {
        if (uiState.isLoading) return

        uiState = LoginUiState(isLoading = true)
        viewModelScope.launch {
            uiState = try {
                if (Supabase.signUp(email, password, fullName, phone)) {
                    LoginUiState(isLoggedIn = true)
                } else {
                    LoginUiState(info = "Account created. Please confirm your email, then log in.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                LoginUiState(error = e.message ?: "Could not create your account.")
            } catch (e: Exception) {
                LoginUiState(error = "Something went wrong. Please try again.")
            }
        }
    }

    /** Clears an old error or message when the user switches between Log in and Sign up. */
    fun clearMessages() {
        if (!uiState.isLoading) uiState = LoginUiState()
    }

    fun logout() {
        uiState = LoginUiState()
        viewModelScope.launch { Supabase.signOut() }
    }
}