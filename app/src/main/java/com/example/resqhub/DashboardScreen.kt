package com.example.resqhub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

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

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dispatch by remember { mutableStateOf<ReportSummary?>(null) }

    val userLocation by rememberUserLocation()

    // TEMPORARY: replace with the real SOS (saved to the database / sent over LoRa) later.
    val triggerSos = {
        val where = userLocation?.let {
            "%.5f, %.5f (±%dm)".format(Locale.US, it.latitude, it.longitude, it.accuracyM.toInt())
        } ?: "location unknown"
        Toast.makeText(context, "SOS triggered at $where (demo only)", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        containerColor = ScreenBg,
        bottomBar = {
            DashboardBottomBar(selected = selectedTab, onSelect = { selectedTab = it })
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DashboardHeader(onProfile = onLogout)
                SosHoldCard(onSos = triggerSos)
                HotlinesSection()
                BarangayMapCard()
            }

            1 -> SafeMapScreen(
                modifier = Modifier.padding(innerPadding),
                userLocation = userLocation,
                onSos = triggerSos,
                onProfile = onLogout,
            )

            2 -> {
                val active = dispatch
                if (active != null) {
                    DispatchScreen(
                        summary = active,
                        modifier = Modifier.padding(innerPadding),
                        userLocation = userLocation,
                        onSos = triggerSos,
                        onProfile = onLogout,
                        onClose = { dispatch = null },
                    )
                } else {
                    ReportScreen(
                        modifier = Modifier.padding(innerPadding),
                        userLocation = userLocation,
                        onSos = triggerSos,
                        onProfile = onLogout,
                        onSubmitted = { dispatch = it },
                    )
                }
            }

            3 -> AnnouncementsScreen(
                modifier = Modifier.padding(innerPadding),
                onProfile = onLogout,
                onReport = { selectedTab = 2 },
                onMapView = { selectedTab = 1 },
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Coming soon", color = InkMuted)
            }
        }
    }
}

// ---------- Header ----------

@Composable
private fun DashboardHeader(onProfile: () -> Unit) {
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )

        // TEMPORARY: the profile button logs out until we build a profile screen.
        IconButton(onClick = onProfile) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile / Log out",
                tint = InkDark,
            )
        }
    }
}

// ---------- Status ----------

@Composable
private fun StatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SafeGreenTint),
        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SafeGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "Safe & Monitored",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )
                Text(
                    text = "Barangay Military Cut-off, Baguio City",
                    fontSize = 12.sp,
                    color = InkMuted,
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Normal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeGreen,
                )
            }
        }
    }
}

// ---------- SOS (hold 3 seconds) ----------

@Composable
private fun SosHoldCard(onSos: () -> Unit) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Start filling the bar. If the finger stays down for 3 s, trigger SOS.
                        val job = scope.launch {
                            progress.animateTo(1f, tween(durationMillis = 3000, easing = LinearEasing))
                            onSos()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()   // waits until the finger lifts
                        job.cancel()
                        scope.launch { progress.snapTo(0f) }
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(ResqRedLight, ResqRed))
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HOLD FOR SOS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LIVE BEACON",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = "Dispatches 911 & high-precision GPS coordinates",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "3 SEC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Progress bar along the bottom edge while the user holds
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color.White,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardPreview() {
    DashboardScreen(onLogout = {})
}

// ---------- Hotlines ----------

private data class Hotline(
    val name: String,
    val subtitle: String,
    val number: String,      // TODO: fill in the real numbers
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
)

@Composable
private fun HotlinesSection() {
    val context = LocalContext.current

    val hotlines = listOf(
        Hotline("911 Emergency", "National Emergency Dispatch", "911", Icons.Filled.MedicalServices, ResqRed, ResqRedTint),
        Hotline("Baguio CDRRMO", "Disaster Response & Rescue", "", Icons.Filled.Shield, BlueAccent, BlueTint),
        Hotline("Baguio Police (BCPO)", "Hotline, Patrol & Assistance", "", Icons.Filled.LocalPolice, BlueAccent, BlueTint),
        Hotline("Barangay Military Cut-off", "BERT & Brgy Hall Hotline", "", Icons.Filled.Apartment, SafeGreen, SafeGreenTint),
    )

    fun call(hotline: Hotline) {
        if (hotline.number.isBlank()) {
            Toast.makeText(context, "Number not set yet", Toast.LENGTH_SHORT).show()
        } else {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hotline.number}")))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ResqRed)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "1-Tap Emergency Hotlines",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "24/7 Immediate",
                fontSize = 11.sp,
                color = InkMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE9ECF1))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        hotlines.forEach { hotline ->
            HotlineRow(hotline = hotline, onClick = { call(hotline) })
        }
    }
}

@Composable
private fun HotlineRow(hotline: Hotline, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hotline.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = hotline.icon,
                    contentDescription = null,
                    tint = hotline.tint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = hotline.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hotline.subtitle,
                    fontSize = 11.sp,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(hotline.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call ${hotline.name}",
                    tint = hotline.tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---------- Barangay map (a picture, not a live map) ----------

@Composable
private fun BarangayMapCard() {
    val context = LocalContext.current

    // Looks for a picture named barangay_map in res/drawable (png or jpg).
    // If you haven't added it yet, a placeholder is shown instead of crashing.
    val mapRes = remember {
        context.resources.getIdentifier("barangay_map", "drawable", context.packageName)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = ResqRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Map Military Cut-off",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SafeGreen)
                )
            }

            if (mapRes != 0) {
                Image(
                    painter = painterResource(id = mapRes),
                    contentDescription = "Map of Barangay Military Cut-off",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScreenBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Add your barangay map picture as barangay_map.png in res/drawable",
                        fontSize = 12.sp,
                        color = InkMuted,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScreenBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeGreenTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Military Cut-off Gym",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=16.4045,120.5985(Military Cut-off Gym)")
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            .onFailure {
                                Toast.makeText(context, "No map app found", Toast.LENGTH_SHORT).show()
                            }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResqRed),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- Bottom navigation ----------

@Composable
private fun DashboardBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "Alerts" to Icons.Filled.Warning,
        "Safe Map" to Icons.Filled.Explore,
        "Report" to Icons.Filled.NotificationsActive,
        "Announcements" to Icons.Filled.Campaign,
    )

    NavigationBar(containerColor = Color.White) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ResqRed,
                    selectedTextColor = ResqRed,
                    indicatorColor = ResqRedTint,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted,
                ),
            )
        }
    }
}