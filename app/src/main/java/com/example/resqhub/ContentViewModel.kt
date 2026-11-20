package com.example.resqhub

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Loads the shared lists (shelters, hotlines, announcements) from the database. */
class ContentViewModel(app: Application) : AndroidViewModel(app) {

    var shelters by mutableStateOf<List<ShelterRow>>(emptyList())
        private set
    var hotlines by mutableStateOf<List<HotlineRow>>(emptyList())
        private set
    var announcements by mutableStateOf<List<AnnouncementRow>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
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
        error = null

        viewModelScope.launch {
            try {
                coroutineScope {
                    val s = async { Supabase.getShelters() }
                    val h = async { Supabase.getHotlines() }
                    val a = async { Supabase.getAnnouncements() }
                    shelters = s.await()
                    hotlines = h.await()
                    announcements = a.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isAuthError) authExpired = true else error = e.message
            } catch (e: Exception) {
                error = "Could not load data. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
}