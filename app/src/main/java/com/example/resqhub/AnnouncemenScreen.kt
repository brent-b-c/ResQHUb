package com.example.resqhub

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedLight = Color(0xFFC8372D)
private val ResqRedTint = Color(0xFFFDEEEC)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val SafeGreen = Color(0xFF2E9E6B)
private val SafeGreenTint = Color(0xFFE9F8F0)
private val BorderGray = Color(0xFFE5E7EB)
private val BlueAccent = Color(0xFF4F46E5)
private val BlueTint = Color(0xFFEEF0FF)

// ---------- Data (loaded from the database) ----------

private enum class Tone { CRITICAL, GOOD, INFO }

private data class Announcement(
    val id: String,
    val category: String,          // matches one of the filter chips
    val tag: String,
    val tone: Tone,
    val timeAgo: String,
    val title: String,
    val body: String,
    val footer: String,
    val footerIcon: ImageVector,
)

private val Filters = listOf("All", "Barangay Official", "Weather & Hazards")

private fun AnnouncementRow.toUi(): Announcement {
    val toneValue = when (tone) {
        "critical" -> Tone.CRITICAL
        "good" -> Tone.GOOD
        else -> Tone.INFO
    }
    return Announcement(
        id = id,
        category = if (category == "weather_hazards") "Weather & Hazards" else "Barangay Official",
        tag = tag,
        tone = toneValue,
        timeAgo = timeAgo(createdAt),
        title = title,
        body = body,
        footer = footer,
        footerIcon = when (toneValue) {
            Tone.CRITICAL -> Icons.Filled.Shield
            Tone.GOOD -> Icons.Filled.CheckCircle
            Tone.INFO -> Icons.Filled.Person
        },
    )
}

/** "2026-11-20T08:14:22.123+00:00" -> "15m ago". The database sends UTC time. */
private fun timeAgo(iso: String): String {
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

// ---------- Screen ----------

@Composable
fun AnnouncementsScreen(
    modifier: Modifier = Modifier,
    announcements: List<AnnouncementRow> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onProfile: () -> Unit = {},
    onReport: () -> Unit = {},
    onMapView: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf("All") }

    val items = remember(announcements) { announcements.map { it.toUi() } }
    val matching = items.filter { filter == "All" || it.category == filter }
    val critical = matching.filter { it.tone == Tone.CRITICAL }
    val bulletins = matching.filter { it.tone != Tone.CRITICAL }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnnouncementsHeader(onProfile = onProfile)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ResqRed,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Brgy. Military Cut-off",
                fontSize = 12.sp,
                color = InkMuted,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Announcements",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkDark,
            )
            Text(
                text = "Official updates & verified reports for your neighborhood",
                fontSize = 13.sp,
                color = InkMuted,
            )
        }

        FilterRow(selected = filter, onSelect = { filter = it })

        critical.forEach { advisory ->
            AnnouncementCard(item = advisory, onMapView = onMapView)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Community Bulletins",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${bulletins.size} Active",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ResqRedTint)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        if (matching.isEmpty()) {
            val message = when {
                isLoading && items.isEmpty() -> "Loading announcements..."
                error != null && items.isEmpty() -> "Could not load announcements. $error"
                items.isEmpty() -> "No announcements yet. Updates from your barangay and responders will appear here."
                else -> "No announcements in this category."
            }
            Text(text = message, fontSize = 13.sp, color = InkMuted)
            if (error != null && items.isEmpty() && !isLoading) {
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

        bulletins.forEach { bulletin ->
            AnnouncementCard(item = bulletin)
        }

        HazardCta(onReport = onReport)

        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Header and filters ----------

@Composable
private fun AnnouncementsHeader(onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(ResqRedLight, ResqRed))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = "resQHub",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onProfile) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile / Log out",
                tint = InkDark,
            )
        }
    }
}

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Filters.forEach { name ->
            val isSelected = name == selected
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else InkDark,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) InkDark else Color.White)
                    .border(1.dp, if (isSelected) InkDark else BorderGray, CircleShape)
                    .clickable { onSelect(name) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

// ---------- Cards ----------

@Composable
private fun AnnouncementCard(item: Announcement, onMapView: (() -> Unit)? = null) {
    val accent = when (item.tone) {
        Tone.CRITICAL -> ResqRed
        Tone.GOOD -> SafeGreen
        Tone.INFO -> BlueAccent
    }
    val tagBg = when (item.tone) {
        Tone.CRITICAL -> ResqRedTint
        Tone.GOOD -> SafeGreenTint
        Tone.INFO -> BlueTint
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.tone == Tone.CRITICAL) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = item.tag,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(text = item.timeAgo, fontSize = 11.sp, color = InkMuted)
                }

                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )
                Text(
                    text = item.body,
                    fontSize = 12.sp,
                    color = InkMuted,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = item.footerIcon,
                            contentDescription = null,
                            tint = if (item.tone == Tone.CRITICAL) accent else InkMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.footer,
                            fontSize = 11.sp,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (onMapView != null) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onMapView,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
                        ) {
                            Text("Map View  >", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HazardCta(onReport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BlueTint),
        border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ResqRedTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        tint = ResqRed,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Notice a hazard in Military Cut-off?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                    )
                    Text(
                        text = "Submit photos and coordinates to notify BERT field dispatch immediately.",
                        fontSize = 11.sp,
                        color = InkMuted,
                    )
                }
            }

            Button(
                onClick = onReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ResqRed),
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Report an Incident", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}