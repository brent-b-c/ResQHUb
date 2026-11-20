package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The responder's report queue. */
class ResponderViewModel(app: Application) : AndroidViewModel(app) {

    var incidents by mutableStateOf<List<IncidentRow>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var authExpired by mutableStateOf(false)
        private set

    /** Reports that are being updated right now (their button shows a spinner). */
    var busyIds by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        Supabase.init(app)
    }

    fun refresh() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                incidents = Supabase.getIncidents()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else error = e.message
            } catch (e: Exception) {
                error = "Could not load reports. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    /** Moves a report to its next status, then reloads the list. */
    fun changeStatus(incident: IncidentRow, newStatus: String, onError: (String) -> Unit) {
        if (incident.id in busyIds) return
        busyIds = busyIds + incident.id

        viewModelScope.launch {
            try {
                Supabase.updateIncidentStatus(incident.id, newStatus)
                incidents = Supabase.getIncidents()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else onError(e.message ?: "That action failed.")
            } catch (e: Exception) {
                onError("Something went wrong. Please try again.")
            } finally {
                busyIds = busyIds - incident.id
            }
        }
    }
}