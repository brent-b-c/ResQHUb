package com.example.resqhub

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import javax.net.ssl.SSLException

/**
 * Talks to Supabase over plain HTTPS (no extra libraries needed).
 *
 * Everything the screens need goes through this one object, so when the LoRa
 * version arrives next year only this file has to change.
 */

class ApiException(
    message: String,
    val statusCode: Int = 0,
    val isAuthError: Boolean = false,
) : Exception(message)

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSec: Long,
    val userId: String,
    val email: String,
)

/** What the Report screen collects (values already use the database's names). */
data class ReportDraft(
    val category: String,        // accident, trapped, fire, medical, road_blocked, hazard
    val categoryLabel: String,
    val severity: String,        // urgent, high, moderate
    val severityLabel: String,
    val people: Int,
    val vulnerable: Boolean,
    val details: String,
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Float?,
)

data class CreatedIncident(
    val id: String,
    val code: String,
    val status: String,
)

data class ShelterRow(
    val id: String,
    val name: String,
    val via: String,
    val lat: Double,
    val lon: Double,
    val phone: String,
    val isPrimary: Boolean,
    val isOpen: Boolean,
    val amenities: List<String>,
)

data class HotlineRow(
    val name: String,
    val subtitle: String,
    val number: String,
)

data class AnnouncementRow(
    val id: String,
    val category: String,     // barangay_official or weather_hazards
    val tone: String,         // critical, good or info
    val tag: String,
    val title: String,
    val body: String,
    val footer: String,
    val createdAt: String,    // ISO time from the database (UTC)
)

object Supabase {

    private lateinit var prefs: SharedPreferences
    private var session: Session? = null

    /** Call once with any context. Safe to call many times. */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("resqhub_session", Context.MODE_PRIVATE)
        session = loadSession()
    }

    fun hasSession(): Boolean = session != null

    // ---------- Login / logout ----------

    suspend fun signIn(email: String, password: String) {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val (code, text) = request("POST", "/auth/v1/token?grant_type=password", body)
        if (code !in 200..299) {
            throw ApiException(
                message = errorMessage(text, "Login failed. Please try again."),
                statusCode = code,
                isAuthError = code == 400 || code == 401,
            )
        }
        saveSession(parseSession(JSONObject(text)))
    }

    suspend fun signOut() {
        val old = session
        clearSession()
        if (old != null) {
            // Best effort: if there is no internet the local logout still worked.
            runCatching { request("POST", "/auth/v1/logout", null, old.accessToken) }
        }
    }

    // ---------- Reports ----------

    suspend fun createIncident(draft: ReportDraft): CreatedIncident {
        val token = validToken()

        val body = JSONObject().apply {
            put("category", draft.category)
            put("severity", draft.severity)
            put("people_count", draft.people)
            put("vulnerable", draft.vulnerable)
            put("details", if (draft.details.isBlank()) JSONObject.NULL else draft.details)
            draft.lat?.let { put("lat", it) }
            draft.lon?.let { put("lon", it) }
            draft.accuracyM?.let { put("accuracy_m", it.toDouble()) }
        }.toString()

        val (code, text) = request(
            method = "POST",
            path = "/rest/v1/incidents",
            body = body,
            token = token,
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        if (code !in 200..299) {
            throw ApiException(
                message = errorMessage(text, "Could not send your report. Please try again."),
                statusCode = code,
                isAuthError = code == 401,
            )
        }
        val row = JSONArray(text).getJSONObject(0)
        return CreatedIncident(
            id = row.getString("id"),
            code = row.getString("code"),
            status = row.getString("status"),
        )
    }

    // ---------- Shared data (read only) ----------

    suspend fun getShelters(): List<ShelterRow> =
        getRows("/rest/v1/shelters?select=*&order=is_primary.desc,name.asc").map { o ->
            val amenityArray = o.optJSONArray("amenities")
            val amenities = mutableListOf<String>()
            if (amenityArray != null) {
                for (i in 0 until amenityArray.length()) amenities.add(amenityArray.getString(i))
            }
            ShelterRow(
                id = o.str("id"),
                name = o.str("name"),
                via = o.str("via"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                phone = o.str("phone"),
                isPrimary = o.optBoolean("is_primary", false),
                isOpen = o.optBoolean("is_open", true),
                amenities = amenities,
            )
        }

    suspend fun getHotlines(): List<HotlineRow> =
        getRows("/rest/v1/hotlines?select=*&order=sort_order.asc").map { o ->
            HotlineRow(
                name = o.str("name"),
                subtitle = o.str("subtitle"),
                number = o.str("number"),
            )
        }

    suspend fun getAnnouncements(): List<AnnouncementRow> =
        getRows("/rest/v1/announcements?select=*&is_active=eq.true&order=created_at.desc&limit=50").map { o ->
            AnnouncementRow(
                id = o.str("id"),
                category = o.str("category"),
                tone = o.str("tone"),
                tag = o.str("tag"),
                title = o.str("title"),
                body = o.str("body"),
                footer = o.str("footer"),
                createdAt = o.str("created_at"),
            )
        }

    private suspend fun getRows(path: String): List<JSONObject> {
        val token = validToken()
        val (code, text) = request("GET", path, null, token)
        if (code !in 200..299) {
            throw ApiException(
                message = errorMessage(text, "Could not load data. Please try again."),
                statusCode = code,
                isAuthError = code == 401,
            )
        }
        val array = JSONArray(text)
        return List(array.length()) { array.getJSONObject(it) }
    }

    /** Reads a text column; a database NULL becomes "" (not the word "null"). */
    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else getString(key)

    // ---------- Session handling ----------

    /** Returns a valid access token, refreshing it first if it is about to expire. */
    private suspend fun validToken(): String {
        val current = session ?: throw ApiException("Please log in again.", 401, true)
        val now = System.currentTimeMillis() / 1000
        if (current.expiresAtSec - 60 > now) return current.accessToken

        val body = JSONObject().put("refresh_token", current.refreshToken).toString()
        val (code, text) = request("POST", "/auth/v1/token?grant_type=refresh_token", body)
        if (code !in 200..299) {
            val expired = code == 400 || code == 401
            if (expired) clearSession()
            throw ApiException("Your session expired. Please log in again.", code, expired)
        }
        val refreshed = parseSession(JSONObject(text))
        saveSession(refreshed)
        return refreshed.accessToken
    }

    private fun parseSession(o: JSONObject): Session {
        val user = o.getJSONObject("user")
        val expiresAt = if (o.has("expires_at")) {
            o.getLong("expires_at")
        } else {
            System.currentTimeMillis() / 1000 + o.optLong("expires_in", 3600)
        }
        return Session(
            accessToken = o.getString("access_token"),
            refreshToken = o.getString("refresh_token"),
            expiresAtSec = expiresAt,
            userId = user.getString("id"),
            email = user.optString("email"),
        )
    }

    private fun saveSession(s: Session) {
        session = s
        prefs.edit()
            .putString("access_token", s.accessToken)
            .putString("refresh_token", s.refreshToken)
            .putLong("expires_at", s.expiresAtSec)
            .putString("user_id", s.userId)
            .putString("email", s.email)
            .apply()
    }

    private fun loadSession(): Session? {
        val access = prefs.getString("access_token", null) ?: return null
        val refresh = prefs.getString("refresh_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return Session(
            accessToken = access,
            refreshToken = refresh,
            expiresAtSec = prefs.getLong("expires_at", 0L),
            userId = userId,
            email = prefs.getString("email", "") ?: "",
        )
    }

    private fun clearSession() {
        session = null
        prefs.edit().clear().apply()
    }

    // ---------- HTTP ----------

    private suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = URL(SupabaseConfig.URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            // The user's token goes in Authorization. The publishable key only goes in "apikey".
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            extraHeaders.forEach { (k, v) -> connection.setRequestProperty(k, v) }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            code to text
        } catch (e: UnknownHostException) {
            Log.e("Supabase", "$method $path failed", e)
            throw ApiException("No internet connection. Please check your connection and try again.")
        } catch (e: SocketTimeoutException) {
            Log.e("Supabase", "$method $path failed", e)
            throw ApiException("The connection timed out. Please try again.")
        } catch (e: SSLException) {
            Log.e("Supabase", "$method $path failed", e)
            throw ApiException("Secure connection failed. Check that the phone's date and time are correct.")
        } catch (e: IOException) {
            Log.e("Supabase", "$method $path failed", e)
            throw ApiException("Network error (${e.javaClass.simpleName}: ${e.message})")
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(text: String, fallback: String): String {
        return try {
            val o = JSONObject(text)
            val message = o.optString("msg")
                .ifBlank { o.optString("error_description") }
                .ifBlank { o.optString("message") }
            message.ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }
}