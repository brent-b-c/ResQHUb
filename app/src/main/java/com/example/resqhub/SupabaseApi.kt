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
    val target: String = "",       // who it is for
    val authorName: String = "",
    val authorTitle: String = "",
    val isActive: Boolean = true, // false = unpublished
)

data class ProfileRow(
    val id: String,
    val fullName: String,
    val phone: String,
    val role: String,          // citizen, responder or admin
    val title: String,         // e.g. BERT Leader
    val unitName: String,      // e.g. Alpha-2
    val dutyStatus: String,    // standby, on_duty or busy
)

data class IncidentRow(
    val id: String,
    val code: String,
    val title: String,
    val category: String,
    val severity: String,      // urgent, high or moderate
    val people: Int,
    val vulnerable: Boolean,
    val details: String,
    val address: String,
    val lat: Double?,
    val lon: Double?,
    val status: String,
    val createdAt: String,
    val reporterName: String,
    val reporterPhone: String,
    val responderId: String,
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

    // ---------- Who is logged in ----------

    fun currentUserId(): String? = session?.userId

    suspend fun getMyProfile(): ProfileRow {
        val id = session?.userId ?: throw ApiException("Please log in again.", 401, true)
        val o = getRows("/rest/v1/profiles?id=eq.$id&select=*").firstOrNull()
            ?: throw ApiException("Your profile was not found.")
        val profile = ProfileRow(
            id = o.str("id"),
            fullName = o.str("full_name"),
            phone = o.str("phone"),
            role = o.str("role").ifBlank { "citizen" },
            title = o.str("title"),
            unitName = o.str("unit_name"),
            dutyStatus = o.str("duty_status").ifBlank { "standby" },
        )
        saveProfileCache(profile)
        return profile
    }

    /** The last profile we loaded, so the app still opens correctly with no internet. */
    fun cachedProfile(): ProfileRow? {
        val text = prefs.getString("profile_json", null) ?: return null
        return try {
            val o = JSONObject(text)
            ProfileRow(
                id = o.str("id"),
                fullName = o.str("full_name"),
                phone = o.str("phone"),
                role = o.str("role").ifBlank { "citizen" },
                title = o.str("title"),
                unitName = o.str("unit_name"),
                dutyStatus = o.str("duty_status").ifBlank { "standby" },
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveProfileCache(p: ProfileRow) {
        val o = JSONObject()
            .put("id", p.id)
            .put("full_name", p.fullName)
            .put("phone", p.phone)
            .put("role", p.role)
            .put("title", p.title)
            .put("unit_name", p.unitName)
            .put("duty_status", p.dutyStatus)
        prefs.edit().putString("profile_json", o.toString()).apply()
    }

    // ---------- Reports: citizen side ----------

    /** Current status of one report (the citizen's dispatch screen polls this). */
    suspend fun getIncidentStatus(incidentId: String): String =
        getRows("/rest/v1/incidents?id=eq.$incidentId&select=status")
            .firstOrNull()?.str("status") ?: "submitted"

    // ---------- Reports: responder side ----------

    suspend fun getIncidents(): List<IncidentRow> =
        getRows(
            "/rest/v1/incidents?select=*,reporter:profiles!reporter_id(full_name,phone)" +
                    "&order=created_at.desc&limit=100"
        ).map { o ->
            val reporter = o.optJSONObject("reporter")
            IncidentRow(
                id = o.str("id"),
                code = o.str("code"),
                title = o.str("title"),
                category = o.str("category"),
                severity = o.str("severity"),
                people = o.optInt("people_count", 1),
                vulnerable = o.optBoolean("vulnerable", false),
                details = o.str("details"),
                address = o.str("address"),
                lat = if (o.isNull("lat")) null else o.getDouble("lat"),
                lon = if (o.isNull("lon")) null else o.getDouble("lon"),
                status = o.str("status"),
                createdAt = o.str("created_at"),
                reporterName = reporter?.str("full_name") ?: "",
                reporterPhone = reporter?.str("phone") ?: "",
                responderId = o.str("responder_id"),
            )
        }

    /** Accept / respond / complete a report (a database function checks the caller is a responder). */
    suspend fun updateIncidentStatus(incidentId: String, status: String) {
        callFunction(
            "update_incident_status",
            JSONObject().put("p_incident", incidentId).put("p_status", status).toString(),
        )
    }

    /** Standby / on_duty / busy. Also updates the saved profile so it survives a restart. */
    suspend fun setDutyStatus(status: String) {
        callFunction("set_duty_status", JSONObject().put("p_status", status).toString())
        cachedProfile()?.let { saveProfileCache(it.copy(dutyStatus = status)) }
    }

    private suspend fun callFunction(name: String, body: String) {
        val token = validToken()
        val (code, text) = request("POST", "/rest/v1/rpc/$name", body, token)
        if (code !in 200..299) {
            throw ApiException(
                message = errorMessage(text, "That action failed. Please try again."),
                statusCode = code,
                isAuthError = code == 401,
            )
        }
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
        getRows("/rest/v1/announcements?select=*&is_active=eq.true&order=created_at.desc&limit=50")
            .map { announcementFrom(it) }

    private fun announcementFrom(o: JSONObject) = AnnouncementRow(
        id = o.str("id"),
        category = o.str("category"),
        tone = o.str("tone"),
        tag = o.str("tag"),
        title = o.str("title"),
        body = o.str("body"),
        footer = o.str("footer"),
        createdAt = o.str("created_at"),
        target = o.str("target"),
        authorName = o.str("author_name"),
        authorTitle = o.str("author_title"),
        isActive = o.optBoolean("is_active", true),
    )

    // ---------- Announcements: responder side ----------

    /** Everything, including unpublished ones (responders are allowed to see those). */
    suspend fun getAllAnnouncements(): List<AnnouncementRow> =
        getRows("/rest/v1/announcements?select=*&order=created_at.desc&limit=100")
            .map { announcementFrom(it) }

    suspend fun createAnnouncement(
        category: String,
        tone: String,
        tag: String,
        title: String,
        body: String,
        target: String,
        authorName: String,
        authorTitle: String,
    ) {
        val token = validToken()
        val json = JSONObject()
            .put("category", category)
            .put("tone", tone)
            .put("tag", tag)
            .put("title", title)
            .put("body", body)
            .put("footer", target)
            .put("target", target)
            .put("author_name", authorName)
            .put("author_title", authorTitle)
            .put("is_live", true)
            .toString()
        val (code, text) = request(
            method = "POST",
            path = "/rest/v1/announcements",
            body = json,
            token = token,
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        )
        if (code !in 200..299) {
            throw ApiException(
                message = errorMessage(text, "Could not publish the announcement."),
                statusCode = code,
                isAuthError = code == 401,
            )
        }
    }

    suspend fun setAnnouncementActive(id: String, active: Boolean) {
        callFunction(
            "set_announcement_active",
            JSONObject().put("p_announcement", id).put("p_active", active).toString(),
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