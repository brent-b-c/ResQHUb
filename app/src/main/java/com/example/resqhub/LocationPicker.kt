package com.example.resqhub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File
import java.util.Locale

private val ResqRed = Color(0xFFA81F1B)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val BorderGray = Color(0xFFE5E7EB)

// Barangay hall, used only if the phone has no location yet.
private const val FallbackLat = 16.4037
private const val FallbackLon = 120.6005

/**
 * Full-screen map to choose where the emergency is. It opens on [initialLat]/[initialLon]
 * (the phone's location by default). Move the map under the pin, then tap Confirm.
 */
@Composable
fun LocationPickerDialog(
    initialLat: Double?,
    initialLon: Double?,
    gpsLocation: UserLocation?,
    initialNote: String,
    onConfirm: (lat: Double, lon: Double, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf(initialNote) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val startLat = initialLat ?: gpsLocation?.latitude ?: FallbackLat
    val startLon = initialLon ?: gpsLocation?.longitude ?: FallbackLon

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    // OpenStreetMap asks apps to identify themselves, or it blocks the tiles.
                    Configuration.getInstance().apply {
                        userAgentValue = "resQHub/1.0 (${ctx.packageName})"
                        osmdroidBasePath = File(ctx.cacheDir, "osmdroid2")
                        osmdroidTileCache = File(File(ctx.cacheDir, "osmdroid2"), "tiles")
                    }
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(17.0)
                        controller.setCenter(GeoPoint(startLat, startLon))
                        onResume()
                        mapView = this
                    }
                },
            )

            // Fixed pin: the tip of the pin is the exact centre of the map.
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Selected location",
                tint = ResqRed,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(40.dp),
            )

            // Top bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Choose location", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkDark)
                    Text("Move the map so the pin is on the emergency.", fontSize = 11.sp, color = InkMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = InkDark)
                }
            }

            // Bottom panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 120) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Landmark or description (optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Purok 3, beside the basketball court", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResqRed,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = ResqRed,
                        cursorColor = ResqRed,
                    ),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val gps = gpsLocation
                            if (gps == null) {
                                Toast.makeText(context, "Your location is not available yet", Toast.LENGTH_SHORT).show()
                            } else {
                                mapView?.controller?.animateTo(GeoPoint(gps.latitude, gps.longitude))
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkDark),
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  My location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val center = mapView?.mapCenter
                            if (center != null) onConfirm(center.latitude, center.longitude, note.trim())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ResqRed),
                    ) {
                        Text("Confirm location", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "The map needs internet to show streets.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkMuted,
                )
            }
        }
    }
}