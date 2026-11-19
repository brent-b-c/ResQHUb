package com.example.resqhub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

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

/** What the Report screen hands over when the user submits. */
data class ReportSummary(
    val categoryLabel: String,
    val severity: String,
    val people: Int,
)

private enum class StepState { DONE, ACTIVE, PENDING }

// ---------- Screen ----------

@Composable
fun DispatchScreen(
    summary: ReportSummary,
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,
    onSos: () -> Unit = {},
    onProfile: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    // TEMPORARY demo data. Later the status, ETA, responder position and speed
    // come from status packets sent back over the LoRa mesh.
    val incidentNumber = rememberSaveable { (1000..9999).random() }
    val startMillis = rememberSaveable { System.currentTimeMillis() }
    var secondsLeft by rememberSaveable { mutableIntStateOf(220) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val loggedTime = timeFormat.format(Date(startMillis))
    val assignedTime = timeFormat.format(Date(startMillis + 120_000))

    val etaText = "%02d:%02d".format(Locale.US, secondsLeft / 60, secondsLeft % 60)
    val etaMinutes = ceil(secondsLeft / 60.0).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DispatchHeader(onSos = onSos, onProfile = onProfile)

        DispatchSummaryCard(
            incidentId = "#BGO-MC-$incidentNumber",
            title = "Brgy. Military Cut-off • ${summary.categoryLabel}",
            subtitle = "${summary.severity} priority • ${summary.people} " +
                    (if (summary.people == 1) "citizen" else "citizens") + " requiring help",
            eta = etaText,
        )

        ProgressCard(
            loggedTime = loggedTime,
            assignedTime = assignedTime,
            etaMinutes = etaMinutes,
        )

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderGray),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
        ) {
            // TEMPORARY: lets you leave this screen while there is no real dispatch system yet.
            Text("Close dispatch (demo)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Header ----------

@Composable
private fun DispatchHeader(onSos: () -> Unit, onProfile: () -> Unit) {
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

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "resQHub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeGreen,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SafeGreenTint)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SettingsInputAntenna,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "LoRa P2P Mesh • 100% Offline Active",
                    fontSize = 10.sp,
                    color = InkMuted,
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(ResqRed)
                .clickable { onSos() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "SOS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        IconButton(onClick = onProfile) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile / Log out",
                tint = InkDark,
            )
        }
    }
}

// ---------- Summary card ----------

@Composable
private fun DispatchSummaryCard(
    incidentId: String,
    title: String,
    subtitle: String,
    eta: String,
) {
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
                    .background(ResqRed)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ResqRed)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "DISPATCH ACTIVE • UNITS EN ROUTE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ResqRed,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = incidentId,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlueTint)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark,
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = InkMuted,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ETA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ResqRed,
                        )
                        Text(
                            text = eta,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = ResqRed,
                        )
                    }
                }
            }
        }
    }
}

// ---------- Dispatch progress ----------

@Composable
private fun ProgressCard(loggedTime: String, assignedTime: String, etaMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dispatch Progress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "ACTIVE PHASE 3",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = BlueAccent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BlueTint)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            TimelineStep(
                state = StepState.DONE,
                title = "Incident Logged & Triaged",
                time = loggedTime,
                detail = "Automated sensor verification • Priority 1",
                isLast = false,
            )
            TimelineStep(
                state = StepState.DONE,
                title = "Assigned: CDRRMO Unit 4",
                time = assignedTime,
                detail = "Swift Response EMT Crew deployed",
                isLast = false,
            )
            TimelineStep(
                state = StepState.ACTIVE,
                title = "En Route via Military Cut-off Rd",
                time = "• ETA ${etaMinutes}M",
                detail = "Navigating along Military Cut-off Rd past slope wash",
                isLast = false,
            )
            TimelineStep(
                state = StepState.PENDING,
                title = "On-Site & Evacuation Extraction",
                time = "PENDING",
                detail = "Target Safe Zone: Military Cut-off Barangay Hall & Evac Center",
                isLast = true,
            )
        }
    }
}

@Composable
private fun TimelineStep(
    state: StepState,
    title: String,
    time: String,
    detail: String,
    isLast: Boolean,
) {
    val iconTint = when (state) {
        StepState.DONE -> SafeGreen
        StepState.ACTIVE -> ResqRed
        StepState.PENDING -> InkMuted.copy(alpha = 0.6f)
    }
    val icon: ImageVector = when (state) {
        StepState.DONE -> Icons.Filled.CheckCircle
        StepState.ACTIVE -> Icons.Filled.Refresh
        StepState.PENDING -> Icons.Outlined.Schedule
    }
    val titleColor = if (state == StepState.PENDING) InkMuted else InkDark
    val timeColor = if (state == StepState.ACTIVE) ResqRed else InkMuted

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (state == StepState.DONE) SafeGreen.copy(alpha = 0.4f) else BorderGray
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 14.dp)
                .then(
                    if (state == StepState.ACTIVE)
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ResqRedTint.copy(alpha = 0.6f))
                            .padding(10.dp)
                    else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state == StepState.ACTIVE) ResqRed else titleColor,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = time,
                    fontSize = 11.sp,
                    fontWeight = if (state == StepState.ACTIVE) FontWeight.Bold else FontWeight.Normal,
                    color = timeColor,
                )
            }
            Text(
                text = detail,
                fontSize = 11.sp,
                color = InkMuted,
            )
        }
    }
}