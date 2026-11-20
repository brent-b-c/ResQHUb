package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The responder's Broadcast tab: the list of announcements and posting new ones. */
class BroadcastViewModel(app: Application) : AndroidViewModel(app) {

    var announcements by mutableStateOf<List<AnnouncementRow>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isPosting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var authExpired by mutableStateOf(false)
        private set

    init {
        Supabase.init(app)
        refresh()
    }

    fun refresh() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                announcements = Supabase.getAllAnnouncements()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else error = e.message
            } catch (e: Exception) {
                error = "Could not load announcements. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    /** Publishes a new announcement. [onDone] runs only if it worked (so the form can clear). */
    fun publish(
        category: String,
        tone: String,
        tag: String,
        title: String,
        body: String,
        target: String,
        author: ProfileRow,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (isPosting) return
        isPosting = true

        viewModelScope.launch {
            try {
                Supabase.createAnnouncement(
                    category = category,
                    tone = tone,
                    tag = tag,
                    title = title,
                    body = body,
                    target = target,
                    authorName = author.fullName,
                    authorTitle = author.title,
                )
                announcements = Supabase.getAllAnnouncements()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else onError(e.message ?: "Could not publish.")
            } catch (e: Exception) {
                onError("Could not publish. Please try again.")
            } finally {
                isPosting = false
            }
        }
    }

    /** Unpublish (hide from citizens) or publish again. */
    fun setActive(item: AnnouncementRow, active: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Supabase.setAnnouncementActive(item.id, active)
                announcements = Supabase.getAllAnnouncements()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else onError(e.message ?: "Could not update.")
            } catch (e: Exception) {
                onError("Could not update. Please try again.")
            }
        }
    }
}