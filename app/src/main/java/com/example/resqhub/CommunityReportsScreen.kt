package com.example.resqhub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private fun isFinished(status: String) = status == "completed" || status == "cancelled"

/** Every resident can open this from the bell icon. It never shows names, phones, photos or exact spots. */
@Composable
fun CommunityReportsScreen(
    reports: List<PublicReportRow>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf("ACTIVE") }

    val active = reports.filter { !isFinished(it.status) }
    val resolved = reports.filter { it.status == "completed" }
    val shown = when (filter) {
        "RESOLVED" -> resolved
        "ALL" -> reports
        else -> active
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkDark)
            }
            Column {
                Text(
                    text = "Community Reports",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                )
                Text(
                    text = "Reports from the last 7 days in Brgy. Military Cut-off",
                    fontSize = 12.sp,
                    color = InkMuted,
                )
            }
        }

        Text(
            text = "To protect privacy, names, phone numbers, photos and exact locations are not shown.",
            fontSize = 11.sp,
            color = InkMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BlueTint.copy(alpha = 0.7f))
                .padding(10.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill("ACTIVE", "ACTIVE (${active.size})", filter) { filter = it }
            FilterPill("RESOLVED", "RESOLVED (${resolved.size})", filter) { filter = it }
            FilterPill("ALL", "ALL (${reports.size})", filter) { filter = it }
        }

        if (shown.isEmpty()) {
            val message = when {
                isLoading && reports.isEmpty() -> "Loading reports..."
                error != null && reports.isEmpty() -> "Could not load reports. $error"
                filter == "ACTIVE" -> "No active reports right now."
                filter == "RESOLVED" -> "No resolved reports yet."
                else -> "No reports in the last 7 days."
            }
            Text(text = message, fontSize = 13.sp, color = InkMuted)
            if (error != null && reports.isEmpty() && !isLoading) {
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

        shown.forEach { ReportCard(it) }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterPill(key: String, label: String, selectedKey: String, onSelect: (String) -> Unit) {
    val selected = key == selectedKey
    Text(
        text = label,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else InkDark,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) InkDark else Color.White)
            .border(1.dp, if (selected) InkDark else BorderGray, CircleShape)
            .clickable { onSelect(key) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun ReportCard(report: PublicReportRow) {
    val accent: Color
    val tint: Color
    val priority: String
    when (report.severity) {
        "urgent" -> { accent = ResqRed; tint = ResqRedTint; priority = "CRITICAL" }
        "high" -> { accent = AmberAccent; tint = AmberTint; priority = "HIGH" }
        else -> { accent = SlateAccent; tint = SlateTint; priority = "MODERATE" }
    }

    val (statusText, statusColor, statusTint) = when (report.status) {
        "submitted" -> Triple("WAITING FOR RESPONDERS", ResqRed, ResqRedTint)
        "acknowledged" -> Triple("RECEIVED", AmberAccent, AmberTint)
        "dispatched" -> Triple("RESPONDERS ASSIGNED", BlueAccent, BlueTint)
        "en_route" -> Triple("UNITS EN ROUTE", BlueAccent, BlueTint)
        "on_site" -> Triple("RESPONDERS ON SITE", BlueAccent, BlueTint)
        "completed" -> Triple("RESOLVED", SafeGreen, SafeGreenTint)
        else -> Triple(report.status.uppercase(), SlateAccent, SlateTint)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = priority,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "#${report.code.substringAfterLast('-')}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(text = agoLabelText(report.createdAt), fontSize = 11.sp, color = InkMuted)
            }

            Text(
                text = report.title.ifBlank { "Emergency Report" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = report.address.ifBlank { "Brgy. Military Cut-off" },
                    fontSize = 12.sp,
                    color = InkMuted,
                )
            }

            Text(
                text = statusText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusTint)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

private fun agoLabelText(iso: String): String {
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