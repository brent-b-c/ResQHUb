package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ReportViewModel(app: Application) : AndroidViewModel(app) {

    var isSubmitting by mutableStateOf(false)
        private set

    init {
        Supabase.init(app)
    }

    /** Saves the report in the database, then calls [onSuccess] with what the dispatch screen needs. */
    fun submit(
        draft: ReportDraft,
        onSuccess: (ReportSummary) -> Unit,
        onError: (String) -> Unit,
        onAuthExpired: () -> Unit,
    ) {
        if (isSubmitting) return
        isSubmitting = true

        viewModelScope.launch {
            try {
                val created = Supabase.createIncident(draft)
                onSuccess(
                    ReportSummary(
                        id = created.id,
                        code = created.code,
                        categoryLabel = draft.categoryLabel,
                        severity = draft.severityLabel,
                        people = draft.people,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) onAuthExpired() else onError(e.message ?: "Could not send your report.")
            } catch (e: Exception) {
                onError("Something went wrong. Please try again.")
            } finally {
                isSubmitting = false
            }
        }
    }
}