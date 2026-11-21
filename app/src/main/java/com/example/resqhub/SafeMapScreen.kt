package com.example.resqhub

import android.content.Intent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import android.location.Geocoder
import android.location.Location
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
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

// ---------- Data (loaded from the database) ----------

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
    val distanceM: Int?,        // null when the phone has no GPS fix yet
    val walkMin: Int?,
    val driveMin: Int?,
    val primary: Boolean,
    val open: Boolean,
    val amenities: List<Amenity>,
    val lat: Double,
    val lon: Double,
    val phone: String,
)

/** Picks an icon and colour for an amenity name that came from the database. */
private fun amenityFor(label: String): Amenity = when {
    label.contains("medic", true) -> Amenity(label, Icons.Filled.MedicalServices, SafeGreen, SafeGreenTint)
    label.contains("generator", true) || label.contains("power", true) ->
        Amenity(label, Icons.Filled.Bolt, AmberAccent, AmberTint)
    label.contains("water", true) -> Amenity(label, Icons.Filled.WaterDrop, BlueAccent, BlueTint)
    label.contains("ada", true) || label.contains("access", true) ->
        Amenity(label, Icons.Filled.Accessible, BlueAccent, BlueTint)
    label.contains("charg", true) -> Amenity(label, Icons.Filled.BatteryChargingFull, AmberAccent, AmberTint)
    label.contains("tent", true) || label.contains("family", true) ->
        Amenity(label, Icons.Filled.Groups, SafeGreen, SafeGreenTint)
    else -> Amenity(label, Icons.Filled.Home, InkMuted, Color(0xFFE9ECF1))
}

private fun metersBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, result)
    return result[0].roundToInt()
}

private fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m" else "%.1f km".format(java.util.Locale.US, meters / 1000.0)

// ---------- Screen ----------

@Composable
fun SafeMapScreen(
    modifier: Modifier = Modifier,
    shelters: List<ShelterRow> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    userLocation: UserLocation? = null,
    onSos: () -> Unit = {},       // not used on this screen right now
    onProfile: () -> Unit = {},
) {
    val context = LocalContext.current

    // With GPS: real distance and walk/drive time. Primary hub first, then nearest.
    val items = remember(shelters, userLocation) {
        val loc = userLocation
        shelters.map { row ->
            val meters = loc?.let { metersBetween(it.latitude, it.longitude, row.lat, row.lon) }
            Shelter(
                id = row.id,
                name = row.name,
                via = row.via,
                distanceM = meters,
                walkMin = meters?.let { max(1, (it / 1000.0 / 5.0 * 60).roundToInt()) },
                driveMin = meters?.let { max(1, (it / 1000.0 / 30.0 * 60).roundToInt()) },
                primary = row.isPrimary,
                open = row.isOpen,
                amenities = row.amenities.map { amenityFor(it) },
                lat = row.lat,
                lon = row.lon,
                phone = row.phone,
            )
        }.sortedWith(
            compareByDescending<Shelter> { it.primary }.thenBy { it.distanceM ?: Int.MAX_VALUE }
        )
    }

    val visible = items

    fun openDirections(shelter: Shelter) {
        // Opens the phone's own maps app. Needs internet or the user's offline maps there.
        val label = Uri.encode(shelter.name)
        val uri = Uri.parse("geo:0,0?q=${shelter.lat},${shelter.lon}($label)")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure {
                Toast.makeText(context, "No map app found", Toast.LENGTH_SHORT).show()
            }
    }

    fun callShelter(shelter: Shelter) {
        if (shelter.phone.isBlank()) {
            Toast.makeText(context, "Number not set yet", Toast.LENGTH_SHORT).show()
        } else {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shelter.phone}")))
            }
        }
    }

    fun share(shelter: Shelter) {
        val text = "${shelter.name} (${shelter.id}) - " +
                "https://www.openstreetmap.org/?mlat=${shelter.lat}&mlon=${shelter.lon}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Share shelter")) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SafeMapHeader(onProfile = onProfile)
        LocationPill(userLocation = userLocation)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Evacuation Stations",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${visible.size} Active",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ResqRedTint)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        if (visible.isEmpty()) {
            val message = when {
                isLoading && items.isEmpty() -> "Loading evacuation stations..."
                error != null && items.isEmpty() -> "Could not load evacuation stations. $error"
                items.isEmpty() -> "No evacuation stations have been added yet."
                else -> "No evacuation stations to show."
            }
            Text(text = message, color = InkMuted, fontSize = 13.sp)
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

        visible.forEach { shelter ->
            ShelterCard(
                shelter = shelter,
                onDirections = { openDirections(shelter) },
                onContact = { callShelter(shelter) },
                onShare = { share(shelter) },
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Header, location, search, chips ----------

@Composable
private fun SafeMapHeader(onProfile: () -> Unit) {
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

/** Shows where the phone is right now (street name if the phone can look it up, otherwise coordinates). */
@Composable
private fun LocationPill(userLocation: UserLocation?) {
    val context = LocalContext.current

    // Round to ~10 m so small GPS wobble does not trigger a new lookup every second.
    val key = userLocation?.let { "%.4f,%.4f".format(java.util.Locale.US, it.latitude, it.longitude) }
    var address by remember(key) { mutableStateOf<String?>(null) }

    LaunchedEffect(key) {
        val loc = userLocation ?: return@LaunchedEffect
        address = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val list = Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocation(loc.latitude, loc.longitude, 1)
                val a = list?.firstOrNull()
                listOfNotNull(
                    a?.thoroughfare,
                    a?.subLocality,
                    a?.locality,
                ).distinct().joinToString(", ").ifBlank { null }
            } catch (e: Exception) {
                null   // no internet or no geocoder on this phone: fall back to coordinates
            }
        }
    }

    val text = when {
        userLocation == null -> "Finding your location..."
        address != null -> address.orEmpty()
        else -> "%.5f, %.5f".format(java.util.Locale.US, userLocation.latitude, userLocation.longitude)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (userLocation == null) InkMuted else ResqRed,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = InkDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

// ---------- Shelter card ----------

@Composable
private fun ShelterCard(
    shelter: Shelter,
    onDirections: () -> Unit,
    onContact: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (shelter.primary) 1.5.dp else 1.dp,
            if (shelter.primary) ResqRed.copy(alpha = 0.5f) else BorderGray,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (shelter.primary) 4.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (shelter.open) "OPEN NOW" else "CLOSED",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (shelter.open) SafeGreen else ResqRed,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (shelter.open) SafeGreenTint else ResqRedTint)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                if (shelter.primary) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "PRIMARY HUB",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE9ECF1))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ID: ${shelter.id}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                if (shelter.primary) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = InkMuted,
                        )
                    }
                }
            }

            Text(
                text = shelter.name,
                fontSize = if (shelter.primary) 20.sp else 16.sp,
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
                    text = shelter.via,
                    fontSize = 12.sp,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
            }

            // Actions (directions open the phone's maps app)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDirections,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = if (shelter.primary) {
                        ButtonDefaults.buttonColors(containerColor = ResqRed)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = BlueTint, contentColor = ResqRed)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Get Directions", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onContact,
                    modifier = Modifier.height(50.dp),
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
                    Text(
                        text = if (shelter.primary) "Desk" else "Contact",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
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