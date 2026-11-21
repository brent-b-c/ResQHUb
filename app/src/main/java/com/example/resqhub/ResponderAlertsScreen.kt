package com.example.resqhub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedTint = Color(0xFFFDEEEC)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val SafeGreen = Color(0xFF2E9E6B)
private val SafeGreenTint = Color(0xFFE9F8F0)
private val BorderGray = Color(0xFFE5E7EB)
private val BlueAccent = Color(0xFF4F46E5)
private val BlueTint = Color(0xFFEEF0FF)
private val AmberAccent = Color(0xFFF59E0B)
private val AmberTint = Color(0xFFFEF6E4)
private val SlateAccent = Color(0xFF64748B)
private val SlateTint = Color(0xFFEEF1F6)

private val ActiveStatuses = setOf("submitted", "acknowledged", "dispatched", "en_route", "on_site")

private fun severityRank(severity: String) = when (severity) {
    "urgent" -> 0
    "high" -> 1
    else -> 2
}

/** What the main button on a card does next, depending on how far the report has got. */
private data class NextAction(val label: String, val status: String, val strong: Boolean)

private fun nextAction(i: IncidentRow): NextAction? = when (i.status) {
    "submitted" -> when (i.severity) {
        "urgent" -> NextAction("RESPOND", "en_route", true)
        "high" -> NextAction("Accept Task", "dispatched", false)
        else -> NextAction("Acknowledge", "acknowledged", false)
    }
    "acknowledged" -> NextAction("Accept Task", "dispatched", false)
    "dispatched" -> NextAction("Mark En Route", "en_route", false)
    "en_route" -> NextAction("Mark On Site", "on_site", false)
    "on_site" -> NextAction("Mark Completed", "completed", true)
    else -> null
}

private fun statusLabel(status: String) = when (status) {
    "submitted" -> "NEW"
    "acknowledged" -> "ACKNOWLEDGED"
    "dispatched" -> "ASSIGNED"
    "en_route" -> "EN ROUTE"
    "on_site" -> "ON SITE"
    "completed" -> "COMPLETED"
    "cancelled" -> "CANCELLED"
    else -> status.uppercase()
}

// ---------- Screen ----------

@Composable
fun ResponderAlertsScreen(
    incidents: List<IncidentRow>,
    isLoading: Boolean,
    error: String?,
    busyIds: Set<String>,
    onRetry: () -> Unit,
    onAction: (IncidentRow, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }

    val active = incidents.filter { it.status in ActiveStatuses }
    val unread = active.count { it.status == "submitted" }

    val critical = active.count { it.severity == "urgent" }
    val high = active.count { it.severity == "high" }
    val moderate = active.count { it.severity == "moderate" }

    val shown = active
        .filter {
            when (filter) {
                "CRITICAL" -> it.severity == "urgent"
                "HIGH" -> it.severity == "high"
                "MODERATE" -> it.severity == "moderate"
                else -> true
            }
        }
        .sortedWith(compareBy<IncidentRow> { severityRank(it.severity) }.thenByDescending { it.createdAt })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = ResqRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Emergency Alerts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.weight(1f),
                )
                if (unread > 0) {
                    Text(
                        text = "$unread UNREAD",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ResqRed)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                text = "Active BERT emergency response queue for Barangay Military Cut-off, Baguio City.",
                fontSize = 12.sp,
                color = InkMuted,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("ALL", "ALL (${active.size})", InkDark, filter) { filter = it }
            FilterChip("CRITICAL", "CRITICAL ($critical)", ResqRed, filter) { filter = it }
            FilterChip("HIGH", "HIGH ($high)", AmberAccent, filter) { filter = it }
            FilterChip("MODERATE", "MODERATE ($moderate)", SlateAccent, filter) { filter = it }
        }

        if (shown.isEmpty()) {
            val message = when {
                isLoading && incidents.isEmpty() -> "Loading reports..."
                error != null && incidents.isEmpty() -> "Could not load reports. $error"
                active.isEmpty() -> "No active reports. New emergency reports will appear here."
                else -> "No reports in this category."
            }
            Text(text = message, fontSize = 13.sp, color = InkMuted)
            if (error != null && incidents.isEmpty() && !isLoading) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
                ) {
                    Text("Try again", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        shown.forEach { incident ->
            AlertCard(
                incident = incident,
                busy = incident.id in busyIds,
                onAction = onAction,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterChip(
    key: String,
    label: String,
    color: Color,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val selected = key == selectedKey
    Text(
        text = label,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else color,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) color else Color.White)
            .border(1.dp, if (selected) color else BorderGray, CircleShape)
            .clickable { onSelect(key) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// ---------- Alert card ----------

@Composable
private fun AlertCard(
    incident: IncidentRow,
    busy: Boolean,
    onAction: (IncidentRow, String) -> Unit,
) {
    val context = LocalContext.current

    val accent: Color
    val tint: Color
    val priorityLabel: String
    when (incident.severity) {
        "urgent" -> {
            accent = ResqRed; tint = ResqRedTint; priorityLabel = "CRITICAL PRIORITY"
        }
        "high" -> {
            accent = AmberAccent; tint = AmberTint; priorityLabel = "HIGH PRIORITY"
        }
        else -> {
            accent = SlateAccent; tint = SlateTint; priorityLabel = "MODERATE PRIORITY"
        }
    }

    val action = nextAction(incident)
    val hasLocation = incident.lat != null && incident.lon != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Priority, incident number, time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (incident.severity == "high") {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(11.dp),
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = priorityLabel,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "INC-#${incident.code.substringAfterLast('-')}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = agoText(incident.createdAt), fontSize = 11.sp, color = InkMuted)
                }

                Text(
                    text = incident.title.ifBlank { "Emergency Report" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )

                Text(
                    text = incident.details.ifBlank { "No additional details were provided." },
                    fontSize = 12.sp,
                    color = InkMuted,
                )

                // People and reporter
                val who = buildString {
                    append(if (incident.people == 1) "1 person" else "${incident.people} people")
                    if (incident.vulnerable) append(" • Vulnerable present")
                    if (incident.reporterName.isNotBlank()) append(" • ${incident.reporterName}")
                }
                Text(
                    text = who,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark,
                )

                // Photos sent by the resident (tap one to enlarge)
                if (incident.photos.isNotEmpty()) {
                    var viewing by rememberSaveable(incident.id) { mutableStateOf<String?>(null) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        incident.photos.forEach { path ->
                            StorageImage(
                                path = path,
                                onClick = { viewing = path },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        }
                    }
                    viewing?.let { PhotoViewerDialog(path = it, onDismiss = { viewing = null }) }
                }

                // Location (tap to open in maps) and status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BlueTint.copy(alpha = 0.6f))
                        .clickable(enabled = hasLocation) {
                            val uri = Uri.parse(
                                "geo:0,0?q=${incident.lat},${incident.lon}(${Uri.encode(incident.title)})"
                            )
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure {
                                    Toast.makeText(context, "No map app found", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ResqRed,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = locationText(incident),
                        fontSize = 11.sp,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusLabel(incident.status),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (incident.status == "submitted") ResqRed else SafeGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (incident.status == "submitted") ResqRedTint else SafeGreenTint)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }

                if (action != null) {
                    Button(
                        onClick = { onAction(incident, action.status) },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (action.strong) {
                            ButtonDefaults.buttonColors(
                                containerColor = ResqRed,
                                disabledContainerColor = ResqRed.copy(alpha = 0.6f),
                                disabledContentColor = Color.White,
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = BlueTint,
                                contentColor = InkDark,
                                disabledContainerColor = BlueTint,
                                disabledContentColor = InkMuted,
                            )
                        },
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (action.strong) Color.White else InkMuted,
                            )
                        } else {
                            if (!action.strong) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(action.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Helpers ----------

private fun locationText(i: IncidentRow): String = when {
    i.address.isNotBlank() -> i.address
    i.lat != null && i.lon != null -> "%.5f, %.5f".format(Locale.US, i.lat, i.lon)
    else -> "Location not shared"
}

/** "2026-11-20T08:14:22.123+00:00" -> "3m ago". The database sends UTC time. */
private fun agoText(iso: String): String {
    if (iso.length < 19) return ""
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val then = format.parse(iso.substring(0, 19))?.time ?: return ""
        val minutes = ((System.currentTimeMillis() - then) / 60_000).coerceAtLeast(0)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    } catch (e: Exception) {
        ""
    }
}