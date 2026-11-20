package com.example.resqhub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private val BorderGray = Color(0xFFE5E7EB)
private val BlueAccent = Color(0xFF4F46E5)
private val AmberAccent = Color(0xFFF59E0B)

private val ActiveStatuses = setOf("submitted", "acknowledged", "dispatched", "en_route", "on_site")

private fun parseUtcMillis(iso: String): Long? {
    if (iso.length < 19) return null
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(iso.substring(0, 19))?.time
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ResponderHomeScreen(
    profile: ProfileRow,
    incidents: List<IncidentRow>,
    onDutyChange: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayAgo = System.currentTimeMillis() - 24L * 60 * 60 * 1000
    val last24h = incidents.filter { (parseUtcMillis(it.createdAt) ?: 0L) >= dayAgo }

    val total = last24h.size
    val pending = incidents.count { it.status == "submitted" }
    val active = incidents.count { it.status in ActiveStatuses }
    val resolved = last24h.count { it.status == "completed" }

    val critical = incidents
        .filter { it.severity == "urgent" && it.status in ActiveStatuses }
        .sortedByDescending { it.createdAt }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileCard(profile = profile, onDutyChange = onDutyChange)

        Text(
            text = "OVERVIEW",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = InkMuted,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Total Reports (24h)", total, InkDark, Modifier.weight(1f))
            StatTile("Pending", pending, ResqRed, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Active Events", active, AmberAccent, Modifier.weight(1f))
            StatTile("Resolved", resolved, SafeGreen, Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CRITICAL REPORTS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = InkMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "VIEW ALL",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = BlueAccent,
                modifier = Modifier.clickable { onOpenAlerts() },
            )
        }

        if (critical.isEmpty()) {
            Text(
                text = "No critical reports right now.",
                fontSize = 13.sp,
                color = InkMuted,
            )
        } else {
            critical.take(5).forEach { CriticalRow(it, onClick = onOpenAlerts) }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileCard(profile: ProfileRow, onDutyChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val initials = profile.fullName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "R" }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(InkDark),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName.ifBlank { "Responder" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = listOf(profile.title, profile.unitName)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "BERT Responder" }
                    Text(text = sub, fontSize = 12.sp, color = InkMuted)
                }
            }

            Text(
                text = "DUTY STATUS",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = InkMuted,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ScreenBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DutyOption("standby", "Standby", InkDark, profile.dutyStatus, onDutyChange, Modifier.weight(1f))
                DutyOption("on_duty", "On Duty", SafeGreen, profile.dutyStatus, onDutyChange, Modifier.weight(1f))
                DutyOption("busy", "Busy", ResqRed, profile.dutyStatus, onDutyChange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DutyOption(
    key: String,
    label: String,
    color: Color,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = key == selectedKey
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else Color.Transparent)
            .clickable { onSelect(key) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else InkMuted,
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$value",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
            Text(text = label, fontSize = 11.sp, color = InkMuted)
        }
    }
}

@Composable
private fun CriticalRow(incident: IncidentRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ResqRed)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.title.ifBlank { "Emergency Report" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "INC-#${incident.code.substringAfterLast('-')} • " +
                            if (incident.people == 1) "1 person" else "${incident.people} people",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkMuted,
                )
            }
            Text(
                text = if (incident.status == "submitted") "NEW" else incident.status.replace('_', ' ').uppercase(),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (incident.status == "submitted") ResqRed else SafeGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (incident.status == "submitted") ResqRedTint else Color(0xFFE9F8F0))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}