package com.example.resqhub

import android.content.Intent
import android.graphics.DashPathEffect
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat
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
private val OrangeAccent = Color(0xFFF59E0B)

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dispatch by remember { mutableStateOf<ReportSummary?>(null) }

    val userLocation by rememberUserLocation()
    val userPoint = userLocation?.let { GeoPoint(it.latitude, it.longitude) }

    // TEMPORARY: replace with the real SOS (BLE -> LoRa node) later.
    val triggerSos = {
        val where = userLocation?.let {
            "%.5f, %.5f (\u00B1%dm)".format(Locale.US, it.latitude, it.longitude, it.accuracyM.toInt())
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
                DashboardHeader(onSos = triggerSos, onProfile = onLogout)
                StatusCard()
                SosHoldCard(onSos = triggerSos)
                HotlinesSection()
                SafeHavensCard(userPoint = userPoint)
                AlertCard(
                    onSafe = {
                        // TEMPORARY: later this sends an "I am Safe" packet over LoRa.
                        Toast.makeText(context, "Marked safe (demo only)", Toast.LENGTH_SHORT).show()
                    }
                )
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

@Composable
private fun DashboardHeader(onSos: () -> Unit, onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(ResqRedLight, ResqRed))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "resQHub",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SafeGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Brgy. Military Cut-off \u2022 LoRa Mesh Relay (Baguio)",
                    fontSize = 11.sp,
                    color = InkMuted,
                    maxLines = 2,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

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

        Spacer(Modifier.width(4.dp))

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
                    text = "Brgy. Military Cut-off, Baguio City \u2022 LoRa Gateway BGO-MC01 Active",
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
                    Text(
                        text = "HOLD FOR SOS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
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
        Hotline("Mil. Cut-off BEI", "Brgy Emergency Desk", "", Icons.Filled.Shield, SafeGreen, SafeGreenTint),
        Hotline("Baguio CDRRM", "Disaster Response", "", Icons.Filled.Shield, BlueAccent, BlueTint),
        Hotline("911 Emergency", "National Dispatch", "911", Icons.Filled.MedicalServices, ResqRed, ResqRedTint),
        Hotline("BCPO Station 5", "Mil. Cut-off Police", "", Icons.Filled.LocalPolice, BlueAccent, BlueTint),
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                fontSize = 17.sp,
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

        hotlines.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { hotline ->
                    HotlineCard(
                        hotline = hotline,
                        modifier = Modifier.weight(1f),
                        onClick = { call(hotline) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HotlineCard(hotline: Hotline, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
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
            Spacer(Modifier.width(10.dp))
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
                    maxLines = 2,
                )
            }
        }
    }
}

// ---------- Safe havens ----------

@Composable
private fun SafeHavensCard(userPoint: GeoPoint?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    tint = ResqRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Safe Havens & Route",
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
                Spacer(Modifier.width(6.dp))
                Text(text = "Radius 5 mi", fontSize = 12.sp, color = InkMuted)
            }

            // Map preview (static in the card; the full interactive map goes in the Safe Map tab)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                SafeHavensMap(userPoint = userPoint, modifier = Modifier.fillMaxSize())

                // Transparent layer on top: stops the map from fighting with page scrolling
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable {
                            Toast.makeText(context, "Full map coming in the Safe Map tab", Toast.LENGTH_SHORT).show()
                        }
                )

                Text(
                    text = "Clear Route \u2022 4 min walk",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
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
                    Text(
                        text = "Open \u2022 Capacity 42%",
                        fontSize = 11.sp,
                        color = InkMuted,
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

// ---------- Alert card ----------

@Composable
private fun AlertCard(onSafe: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(ResqRed, OrangeAccent)))
            )

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SLOPE TELEMETRY ALERT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ResqRed,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ResqRedTint)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Mil. Cut-off Sector",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = InkMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = "10m ago", fontSize = 11.sp, color = InkMuted)
                }

                // TEMPORARY text. Later this comes from LoRa sensor nodes via the gateway.
                Text(
                    text = "Soil saturation telemetry along Military Cut-off Rd and South Drive slope " +
                            "is at Warning Level 2. Passable with extreme caution. Evacuation center at " +
                            "Military Cut-off Gym is fully operational.",
                    fontSize = 13.sp,
                    color = InkDark,
                )

                Button(
                    onClick = onSafe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResqRed),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("I am Safe", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        "Evacuate" to Icons.AutoMirrored.Filled.DirectionsWalk,
    )

    NavigationBar(containerColor = Color.White) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
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

// ---------- Map (osmdroid) ----------

// TODO: replace with the user's real GPS location and the real evacuation center coordinates
private val DemoUserPoint = GeoPoint(16.4015, 120.5935)
private val DemoHavenPoint = GeoPoint(16.4045, 120.5985)

@Composable
private fun SafeHavensMap(userPoint: GeoPoint?, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            configureOsmdroid(ctx)
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(17.0)
                onResume()
            }
        },
        update = { map ->
            val user = userPoint ?: DemoUserPoint
            // If the phone is far from Baguio (for example an emulator set elsewhere),
            // just show the shelter instead of an empty, zoomed-out map.
            val farAway = user.distanceToAsDouble(DemoHavenPoint) > 20_000.0

            map.overlays.clear()

            if (!farAway) {
                map.overlays.add(
                    Polyline().apply {
                        setPoints(listOf(user, DemoHavenPoint))
                        outlinePaint.color = SafeGreen.toArgb()
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 14f), 0f)
                    }
                )
                map.overlays.add(
                    Marker(map).apply {
                        position = user
                        title = "You are here"
                        icon = ContextCompat.getDrawable(map.context, android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )
            }

            map.overlays.add(
                Marker(map).apply {
                    position = DemoHavenPoint
                    title = "Military Cut-off Gym"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )

            map.controller.setCenter(
                if (farAway) DemoHavenPoint
                else GeoPoint(
                    (user.latitude + DemoHavenPoint.latitude) / 2,
                    (user.longitude + DemoHavenPoint.longitude) / 2,
                )
            )
            map.invalidate()
        },
        onRelease = { it.onDetach() },
    )
}