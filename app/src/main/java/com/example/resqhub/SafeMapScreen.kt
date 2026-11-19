package com.example.resqhub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt


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
private val AmberAccent = Color(0xFFF59E0B)
private val AmberTint = Color(0xFFFEF6E4)

// ---------- Shared map setup (used by every map in the app) ----------

internal fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = "resQHub/1.0 (capstone project; Android; contact: YOUR_EMAIL_HERE)"
        osmdroidBasePath = File(context.cacheDir, "osmdroid2")
        osmdroidTileCache = File(context.cacheDir, "osmdroid2/tiles")
    }
}

// ---------- Sample data (TEMPORARY: replace with the database / LoRa updates later) ----------

private data class Amenity(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
)

private data class Shelter(
    val id: String,
    val name: String,
    val via: String,
    val distanceKm: Double,
    val walkMin: Int,
    val driveMin: Int,
    val used: Int,
    val total: Int,
    val intake: String,
    val floodSafe: Boolean,
    val amenities: List<Amenity>,
    val point: GeoPoint,
    val phone: String,
) {
    val percent: Int get() = used * 100 / total
}

private val MedicStation = Amenity("Medic Station", Icons.Filled.MedicalServices, SafeGreen, SafeGreenTint)
private val GeneratorBackup = Amenity("Generator Backup", Icons.Filled.Bolt, AmberAccent, AmberTint)
private val PotableWater = Amenity("Potable Water Fill", Icons.Filled.WaterDrop, BlueAccent, BlueTint)
private val AdaAccess = Amenity("ADA Compliant Access", Icons.Filled.Accessible, BlueAccent, BlueTint)

// TODO: replace coordinates, capacities and phone numbers with real data
private val SampleShelters = listOf(
    Shelter(
        id = "BGO-S01",
        name = "Baguio Convention Center",
        via = "Gov. Pack Rd",
        distanceKm = 1.2,
        walkMin = 6,
        driveMin = 3,
        used = 520,
        total = 800,
        intake = "Gate 2",
        floodSafe = true,
        amenities = listOf(MedicStation, GeneratorBackup, PotableWater, AdaAccess),
        point = GeoPoint(16.4115, 120.5930),
        phone = "",
    ),
    Shelter(
        id = "BGO-S02",
        name = "Teachers Camp Gym",
        via = "Leonard Wood Rd",
        distanceKm = 2.5,
        walkMin = 14,
        driveMin = 6,
        used = 200,
        total = 500,
        intake = "Main entrance",
        floodSafe = true,
        amenities = listOf(MedicStation, PotableWater),
        point = GeoPoint(16.4130, 120.6000),
        phone = "",
    ),
)

// ---------- Screen ----------

@Composable
fun SafeMapScreen(
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,
    onSos: () -> Unit = {},
    onProfile: () -> Unit = {},
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(SampleShelters.first().id) }

    val userPoint = userLocation?.let { GeoPoint(it.latitude, it.longitude) }

    // With GPS: recompute distance and walk/drive time, nearest first.
    val shelters = remember(userLocation) {
        if (userPoint == null) {
            SampleShelters
        } else {
            SampleShelters.map { s ->
                val km = userPoint.distanceToAsDouble(s.point) / 1000.0
                s.copy(
                    distanceKm = (km * 10).roundToInt() / 10.0,
                    walkMin = max(1, (km / 5.0 * 60).roundToInt()),
                    driveMin = max(1, (km / 30.0 * 60).roundToInt()),
                )
            }.sortedBy { it.distanceKm }
        }
    }

    val gpsText = userLocation?.let { "GPS \u00B1${it.accuracyM.toInt()}m" } ?: "GPS searching..."

    val visible = remember(query, shelters) {
        shelters.filter {
            query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.via.contains(query, ignoreCase = true)
        }
    }
    val selected = visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull()

    fun comingSoon() {
        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SafeMapHeader(gpsText = gpsText, onSos = onSos, onProfile = onProfile)
        SessionBanner()
        ShelterSearchBar(query = query, onChange = { query = it })
        FilterChipsRow(shelterCount = visible.size, onComingSoon = ::comingSoon)

        MapSection(
            shelters = visible,
            selectedId = selected?.id,
            userPoint = userPoint,
            onSelect = { selectedId = it },
            onComingSoon = ::comingSoon,
        )

        RouteTicker()

        if (selected != null) {
            ShelterDetailCard(
                shelter = selected,
                onNavigate = {
                    val label = Uri.encode(selected.name)
                    val uri = Uri.parse("geo:0,0?q=${selected.point.latitude},${selected.point.longitude}($label)")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        .onFailure {
                            Toast.makeText(context, "No map app found", Toast.LENGTH_SHORT).show()
                        }
                },
                onDesk = {
                    if (selected.phone.isBlank()) {
                        Toast.makeText(context, "Desk number not set yet", Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selected.phone}")))
                        }
                    }
                },
                onShare = {
                    val text = "${selected.name} (${selected.id}) - " +
                            "https://www.openstreetmap.org/?mlat=${selected.point.latitude}&mlon=${selected.point.longitude}"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Share shelter")) }
                },
            )
        } else {
            Text(
                text = "No shelters match your search.",
                color = InkMuted,
                fontSize = 13.sp,
            )
        }

        visible.filter { it.id != selected?.id }.forEach { shelter ->
            ShelterRow(shelter = shelter, onView = { selectedId = shelter.id })
        }
    }
}

// ---------- Header, banner, search, chips ----------

@Composable
private fun SafeMapHeader(gpsText: String, onSos: () -> Unit, onProfile: () -> Unit) {
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
            // TEMPORARY text: GPS accuracy and cache status come from real data later
            Text(
                text = "$gpsText \u2022 Cache 100%",
                fontSize = 11.sp,
                color = InkMuted,
            )
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

@Composable
private fun SessionBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ResqRed)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Session Rd \u2022 Baguio City Center Hub",
            fontSize = 12.sp,
            color = InkDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "LORA MESH",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = SafeGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SafeGreenTint)
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ShelterSearchBar(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search shelters, clear corridors, medical posts...",
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ResqRed,
            unfocusedBorderColor = BorderGray,
            cursorColor = ResqRed,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
    )
}

@Composable
private fun FilterChipsRow(shelterCount: Int, onComingSoon: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = "Shelters ($shelterCount)",
            icon = Icons.Filled.Home,
            container = InkDark,
            content = Color.White,
            borderColor = InkDark,
            onClick = {},
        )
        FilterPill(
            label = "Safe Corridors",
            icon = Icons.Filled.Directions,
            container = Color.White,
            content = InkDark,
            borderColor = BorderGray,
            onClick = onComingSoon,
        )
        FilterPill(
            label = "Medical (3)",
            icon = Icons.Filled.MedicalServices,
            container = Color.White,
            content = InkDark,
            borderColor = BorderGray,
            onClick = onComingSoon,
        )
        FilterPill(
            label = "Kennon Rd Blocked",
            icon = Icons.Filled.Warning,
            container = ResqRedTint,
            content = ResqRed,
            borderColor = ResqRed.copy(alpha = 0.3f),
            onClick = onComingSoon,
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

// ---------- Map ----------

@Composable
private fun MapSection(
    shelters: List<Shelter>,
    selectedId: String?,
    userPoint: GeoPoint?,
    onSelect: (String) -> Unit,
    onComingSoon: () -> Unit,
) {
    var layer by remember { mutableStateOf("TOPO") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
    ) {
        SheltersMapView(
            shelters = shelters,
            selectedId = selectedId,
            userPoint = userPoint,
            onSelect = onSelect,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer switch (only TOPO works for now; SAT and RADAR need extra data sources)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .padding(3.dp),
        ) {
            listOf("TOPO", "SAT", "RADAR").forEach { name ->
                val isSelected = layer == name
                Text(
                    text = name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else InkMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) InkDark else Color.Transparent)
                        .clickable {
                            if (name == "TOPO") layer = name else onComingSoon()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SheltersMapView(
    shelters: List<Shelter>,
    selectedId: String?,
    userPoint: GeoPoint?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            configureOsmdroid(ctx)
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(15.0)
                controller.setCenter(SampleShelters.first().point)

                // Let the map pan instead of the page scrolling while a finger is on the map
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                onResume()
            }
        },
        update = { map ->
            map.overlays.clear()
            shelters.forEach { shelter ->
                map.overlays.add(
                    Marker(map).apply {
                        position = shelter.point
                        title = shelter.name
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onSelect(shelter.id)
                            true
                        }
                    }
                )
            }

            // "You are here" marker (skipped if the phone is far from Baguio, e.g. an emulator)
            if (userPoint != null &&
                userPoint.distanceToAsDouble(SampleShelters.first().point) < 20_000.0
            ) {
                map.overlays.add(
                    Marker(map).apply {
                        position = userPoint
                        title = "You are here"
                        icon = ContextCompat.getDrawable(map.context, android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )
            }

            // Move the camera only when the selected shelter changes
            val selected = shelters.firstOrNull { it.id == selectedId }
            if (selected != null && map.tag != selected.id) {
                map.controller.animateTo(selected.point)
                map.tag = selected.id
            }
            map.invalidate()
        },
        onRelease = { it.onDetach() },
    )
}

// ---------- Road status ticker (TEMPORARY demo data; later fed by LoRa updates) ----------

@Composable
private fun RouteTicker() {
    val items = listOf(
        Triple("Marcos Hwy", "Optimal", SafeGreen),
        Triple("Nagulian Rd", "Passable", AmberAccent),
        Triple("Kennon Rd", "Blocked", ResqRed),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (road, status, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$road: ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

// ---------- Shelter cards ----------

@Composable
private fun ShelterDetailCard(
    shelter: Shelter,
    onNavigate: () -> Unit,
    onDesk: () -> Unit,
    onShare: () -> Unit,
) {
    val percent = shelter.percent
    val barColor = when {
        percent >= 80 -> ResqRed
        percent >= 50 -> AmberAccent
        else -> SafeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OPEN NOW",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SafeGreen,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SafeGreenTint)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ID: ${shelter.id}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = InkMuted,
                    )
                }
            }

            Text(
                text = shelter.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NearMe,
                    contentDescription = null,
                    tint = ResqRed,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${shelter.distanceKm} km away via ${shelter.via}",
                    fontSize = 12.sp,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${shelter.walkMin} min walk / ${shelter.driveMin} min drive",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark,
                )
            }

            // Capacity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ScreenBg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CAPACITY STATUS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${shelter.used} / ${shelter.total} Bed Spaces ($percent% Full)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                    )
                }
                LinearProgressIndicator(
                    progress = { shelter.used.toFloat() / shelter.total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = barColor,
                    trackColor = BorderGray,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fast intake line: ${shelter.intake}",
                        fontSize = 11.sp,
                        color = InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    if (shelter.floodSafe) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Safe from flood zone",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SafeGreen,
                        )
                    }
                }
            }

            // Amenities, two per row
            shelter.amenities.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { amenity ->
                        AmenityChip(amenity = amenity, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResqRed),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Start Navigation", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDesk,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Desk", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AmenityChip(amenity: Amenity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(amenity.bg)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = amenity.icon,
            contentDescription = null,
            tint = amenity.tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = amenity.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShelterRow(shelter: Shelter, onView: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScreenBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shelter.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${shelter.percent}% Cap",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ScreenBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = "${shelter.distanceKm} km away \u2022 ${shelter.via}",
                    fontSize = 11.sp,
                    color = InkMuted,
                )
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(
                onClick = onView,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
            ) {
                Text("View", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}