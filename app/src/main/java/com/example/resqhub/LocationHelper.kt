package com.example.resqhub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
)

private fun Location.toUserLocation() = UserLocation(latitude, longitude, accuracy)

/** How old this fix is, in seconds. */
private fun Location.ageSeconds(): Long =
    (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000_000L

/** Old saved locations (from a previous place or a previous test) must not be shown as "now". */
private const val MaxFixAgeSeconds = 120L

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

/**
 * Asks for location permission (once) and returns the latest fix while the screen is showing.
 * Call this ONCE near the top of the screen and pass the result down.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberUserLocation(): State<UserLocation?> {
    val context = LocalContext.current
    val location = remember { mutableStateOf<UserLocation?>(null) }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    location.value = loc.toUserLocation()
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                runCatching {
                    if (manager.isProviderEnabled(provider)) {
                        // A saved location is only used if it is recent.
                        manager.getLastKnownLocation(provider)
                            ?.takeIf { it.ageSeconds() <= MaxFixAgeSeconds }
                            ?.let { location.value = it.toUserLocation() }

                        // The first fresh fix arrives on its own; after that, every 3 seconds if it changed.
                        manager.requestLocationUpdates(provider, 3000L, 0f, listener)
                    }
                }
            }

            onDispose { manager.removeUpdates(listener) }
        }
    }

    return location
}

/**
 * The street / area name for a location, or null while unknown (or if the phone cannot look it up,
 * for example with no internet).
 */
@Composable
fun rememberStreetName(userLocation: UserLocation?): State<String?> {
    val context = LocalContext.current
    // Round to ~10 m so small GPS wobble does not start a new lookup every second.
    val key = userLocation?.let { "%.4f,%.4f".format(java.util.Locale.US, it.latitude, it.longitude) }
    val name = remember(key) { mutableStateOf<String?>(null) }

    LaunchedEffect(key) {
        val loc = userLocation ?: return@LaunchedEffect
        name.value = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val a = Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
                listOfNotNull(a?.thoroughfare, a?.subLocality, a?.locality)
                    .distinct().joinToString(", ").ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }
    }
    return name
}