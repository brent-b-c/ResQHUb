package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Loads the logged-in user's profile, which says whether they are a citizen or a responder. */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    var profile by mutableStateOf<ProfileRow?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var authExpired by mutableStateOf(false)
        private set

    init {
        Supabase.init(app)
        // Show the saved profile straight away; the fresh one replaces it a moment later.
        profile = Supabase.cachedProfile()
        load()
    }

    /** Changes the duty switch right away, and puts it back if the server says no. */
    fun setDuty(status: String, onError: (String) -> Unit) {
        val before = profile ?: return
        if (before.dutyStatus == status) return
        profile = before.copy(dutyStatus = status)

        viewModelScope.launch {
            try {
                Supabase.setDutyStatus(status)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                profile = profile?.copy(dutyStatus = before.dutyStatus)
                if (e.isAuthError) authExpired = true else onError(e.message ?: "Could not change your status.")
            } catch (e: Exception) {
                profile = profile?.copy(dutyStatus = before.dutyStatus)
                onError("Could not change your status.")
            }
        }
    }

    fun load() {
        if (isLoading) return
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                profile = Supabase.getMyProfile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else error = e.message
            } catch (e: Exception) {
                error = "Could not load your account. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
}