package com.example.resqhub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedLight = Color(0xFFC8372D)
private val ResqRedTint = Color(0xFFFDEEEC)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val BorderGray = Color(0xFFE5E7EB)
private val BlueAccent = Color(0xFF4F46E5)
private val BlueTint = Color(0xFFEEF0FF)

// TODO: put the real CDRRMO dispatch command number here
private const val CdrrmoNumber = ""

/** What the Report screen hands over when the user submits. */
data class ReportSummary(
    val id: String,              // database id, used to follow the status
    val code: String,            // e.g. BGO-MC-1000, created by the database
    val categoryLabel: String,
    val severity: String,
    val people: Int,
)

/** What the red bar says, depending on how far the responders have got. */
private fun dispatchStatusText(status: String) = when (status) {
    "submitted" -> "REPORT SENT • WAITING FOR RESPONDERS"
    "acknowledged" -> "REPORT RECEIVED • PREPARING RESPONSE"
    "dispatched" -> "RESPONDERS ASSIGNED"
    "en_route" -> "DISPATCH ACTIVE • UNITS EN ROUTE"
    "on_site" -> "RESPONDERS ON SITE"
    "completed" -> "RESOLVED"
    "cancelled" -> "REPORT CANCELLED"
    else -> "REPORT SENT"
}

// ---------- Screen ----------

@Composable
fun DispatchScreen(
    summary: ReportSummary,
    status: String = "submitted",
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,   // not used on this screen right now
    onSos: () -> Unit = {},               // not used on this screen right now
    onProfile: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DispatchHeader(onProfile = onProfile)

        DispatchSummaryCard(
            status = status,
            incidentId = "#${summary.code}",
            title = "Brgy. Military Cut-off • ${summary.categoryLabel}",
            subtitle = "${summary.severity} • ${summary.people} " +
                    (if (summary.people == 1) "Citizen" else "Citizens") + " Requiring Extraction",
        )

        Button(
            onClick = {
                if (CdrrmoNumber.isBlank()) {
                    Toast.makeText(context, "Dispatch command number not set yet", Toast.LENGTH_SHORT).show()
                } else {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CdrrmoNumber")))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InkDark),
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "CALL DISPATCH COMMAND (CDRRMO)",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        SlideToComplete(onComplete = onClose)
    }
}

// ---------- Header ----------

@Composable
private fun DispatchHeader(onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ResqRedLight, ResqRed))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    append("resQ")
                    withStyle(SpanStyle(color = ResqRed)) { append("Hub") }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "LIVE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ResqRedTint)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
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
    status: String,
    incidentId: String,
    title: String,
    subtitle: String,
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
                        text = dispatchStatusText(status),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ResqRed,
                        modifier = Modifier.weight(1f),
                    )
                }

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

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = InkMuted,
                    )
                }
            }
        }
    }
}

// ---------- Slide to complete ----------

@Composable
private fun SlideToComplete(onComplete: () -> Unit) {
    val density = LocalDensity.current
    var offsetPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(BlueTint),
    ) {
        val handleSize = 44.dp
        val maxPx = with(density) { (maxWidth - handleSize - 8.dp).toPx() }

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                contentDescription = null,
                tint = BlueAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "SLIDE TO COMPLETE DISPATCH",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = BlueAccent,
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .padding(4.dp)
                .size(handleSize)
                .clip(CircleShape)
                .background(InkDark)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(0f, maxPx)
                    },
                    onDragStopped = {
                        if (offsetPx >= maxPx * 0.85f) onComplete()
                        offsetPx = 0f
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Slide to complete",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}