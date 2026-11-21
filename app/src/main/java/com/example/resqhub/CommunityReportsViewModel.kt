package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The list of recent reports that every resident can see (private details left out). */
class CommunityReportsViewModel(app: Application) : AndroidViewModel(app) {

    var reports by mutableStateOf<List<PublicReportRow>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var authExpired by mutableStateOf(false)
        private set

    /** Reports still being handled (not finished): this is the number on the bell. */
    val activeCount: Int
        get() = reports.count { it.status != "completed" && it.status != "cancelled" }

    init {
        Supabase.init(app)
    }

    fun refresh() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                reports = Supabase.getPublicReports()
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
}