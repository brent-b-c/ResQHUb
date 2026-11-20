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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

private const val DefaultTarget = "Brgy. Military Cut-off (All Puroks)"

@Composable
fun ResponderBroadcastScreen(
    profile: ProfileRow,
    announcements: List<AnnouncementRow>,
    isLoading: Boolean,
    isPosting: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onPublish: (
        category: String,
        tone: String,
        tag: String,
        title: String,
        body: String,
        target: String,
        onDone: () -> Unit,
    ) -> Unit,
    onSetActive: (AnnouncementRow, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(DefaultTarget) }
    var category by rememberSaveable { mutableStateOf("barangay_official") }
    var tone by rememberSaveable { mutableStateOf("info") }

    val canPublish = title.isNotBlank() && body.isNotBlank() && !isPosting

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
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = ResqRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Broadcast",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                )
            }
            Text(
                text = "Send announcements to residents of Barangay Military Cut-off.",
                fontSize = 12.sp,
                color = InkMuted,
            )
        }

        // ---- Create form ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderGray),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Create new announcement",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )

                FieldLabel("TYPE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Barangay Official", category == "barangay_official", InkDark) {
                        category = "barangay_official"
                    }
                    Choice("Weather & Hazards", category == "weather_hazards", InkDark) {
                        category = "weather_hazards"
                    }
                }

                FieldLabel("PRIORITY")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Critical", tone == "critical", ResqRed) { tone = "critical" }
                    Choice("Info", tone == "info", BlueAccent) { tone = "info" }
                    Choice("Good news", tone == "good", SafeGreen) { tone = "good" }
                }

                FieldLabel("TITLE")
                Field(value = title, onChange = { title = it }, placeholder = "e.g. Road closure on Kennon Road")

                FieldLabel("MESSAGE")
                Field(
                    value = body,
                    onChange = { body = it },
                    placeholder = "What do residents need to know?",
                    singleLine = false,
                    minHeight = 96.dp,
                )

                FieldLabel("SEND TO")
                Field(value = target, onChange = { target = it }, placeholder = DefaultTarget)

                Button(
                    onClick = {
                        val tag = when {
                            tone == "critical" -> "CRITICAL ADVISORY"
                            category == "weather_hazards" -> "WEATHER & HAZARD"
                            tone == "good" -> "GOOD NEWS"
                            else -> "BARANGAY NOTICE"
                        }
                        onPublish(
                            category,
                            tone,
                            tag,
                            title.trim(),
                            body.trim(),
                            target.trim().ifBlank { DefaultTarget },
                        ) {
                            title = ""
                            body = ""
                            target = DefaultTarget
                            tone = "info"
                        }
                    },
                    enabled = canPublish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResqRed,
                        disabledContainerColor = ResqRed.copy(alpha = 0.35f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("Publish announcement", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---- List ----
        Text(
            text = "ANNOUNCEMENTS",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = InkMuted,
        )

        if (announcements.isEmpty()) {
            val message = when {
                isLoading -> "Loading announcements..."
                error != null -> "Could not load announcements. $error"
                else -> "No announcements yet."
            }
            Text(text = message, fontSize = 13.sp, color = InkMuted)
            if (error != null && !isLoading) {
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

        announcements.forEach { item ->
            AnnouncementCard(item = item, onSetActive = onSetActive)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = InkMuted,
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else InkMuted,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) color else ScreenBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = InkMuted) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.height(minHeight) else Modifier),
        shape = RoundedCornerShape(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = InkDark),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ResqRed,
            unfocusedBorderColor = BorderGray,
            cursorColor = ResqRed,
        ),
    )
}

@Composable
private fun AnnouncementCard(item: AnnouncementRow, onSetActive: (AnnouncementRow, Boolean) -> Unit) {
    val accent = when (item.tone) {
        "critical" -> ResqRed
        "good" -> SafeGreen
        else -> BlueAccent
    }
    val tint = when (item.tone) {
        "critical" -> ResqRedTint
        "good" -> SafeGreenTint
        else -> BlueTint
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
                Text(
                    text = item.tag.ifBlank { "NOTICE" },
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                if (!item.isActive) {
                    Text(
                        text = "UNPUBLISHED",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScreenBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = agoLabel(item.createdAt), fontSize = 11.sp, color = InkMuted)
            }

            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isActive) InkDark else InkMuted,
            )
            Text(text = item.body, fontSize = 12.sp, color = InkMuted)

            if (item.target.isNotBlank()) {
                Text(
                    text = "To: ${item.target}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val by = listOf(item.authorName, item.authorTitle).filter { it.isNotBlank() }.joinToString(" • ")
                Text(
                    text = if (by.isBlank()) "Posted by responders" else "By $by",
                    fontSize = 11.sp,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (item.isActive) "UNPUBLISH" else "PUBLISH AGAIN",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isActive) ResqRed else SafeGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSetActive(item, !item.isActive) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun agoLabel(iso: String): String {
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