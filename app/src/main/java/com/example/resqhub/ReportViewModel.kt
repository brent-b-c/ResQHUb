package com.example.resqhub

import android.app.Application
import android.net.Uri
import android.widget.Toast
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

    private fun uploadPhotosInBackground(incidentId: String, uris: List<String>) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val bytes = uris.mapNotNull { PhotoUtils.compressForUpload(context, Uri.parse(it)) }
                val failed = (uris.size - bytes.size) + Supabase.uploadAndAttachPhotos(incidentId, bytes)
                val message = if (failed == 0) "Photos attached to your report"
                else "Your report was sent, but $failed photo(s) could not be uploaded."
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "photo upload failed", e)
                Toast.makeText(context, "Your report was sent, but the photos could not be uploaded.", Toast.LENGTH_LONG).show()
            }
        }
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

                // The report is already saved. Pictures go up afterwards so they never delay help.
                if (draft.photoUris.isNotEmpty()) {
                    uploadPhotosInBackground(created.id, draft.photoUris)
                }
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