package com.example.resqhub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

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
private val PurpleAccent = Color(0xFF7C3AED)
private val PurpleTint = Color(0xFFF1EAFE)

// ---------- Data ----------

private data class IncidentCategory(
    val id: String,
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
)

private val IncidentCategories = listOf(
    IncidentCategory("accident", "Road Accident", "Collision / Vehicular", Icons.Filled.CarCrash, ResqRed, ResqRedTint),
    IncidentCategory("trapped", "Trapped", "Roof / Cut off", Icons.Filled.Accessibility, BlueAccent, BlueTint),
    IncidentCategory("fire", "Fire", "Smoke / Blaze", Icons.Filled.LocalFireDepartment, AmberAccent, AmberTint),
    IncidentCategory("medical", "Medical", "Injury / Trauma", Icons.Filled.MedicalServices, SafeGreen, SafeGreenTint),
    IncidentCategory("road_blocked", "Road Blocked", "Landslide / Tree", Icons.Filled.Block, PurpleAccent, PurpleTint),
    IncidentCategory("hazard", "Hazard", "Live wire / Gas leak", Icons.Filled.Bolt, ResqRed, ResqRedTint),
)

private val SeverityLevels = listOf("Urgent", "High", "Moderate")

// ---------- Screen ----------

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,
    onSos: () -> Unit = {},
    onProfile: () -> Unit = {},
    isSubmitting: Boolean = false,
    onSubmit: (ReportDraft) -> Unit = {},
) {
    val context = LocalContext.current

    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var severity by rememberSaveable { mutableIntStateOf(0) }
    var people by rememberSaveable { mutableIntStateOf(1) }
    var vulnerable by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf("") }

    // Progress: category + location + severity + details
    val done = listOf(
        categoryId != null,
        userLocation != null,
        true,
        details.isNotBlank(),
    ).count { it }

    fun comingSoon(what: String) {
        Toast.makeText(context, "$what: coming soon", Toast.LENGTH_SHORT).show()
    }

    fun submit() {
        val category = IncidentCategories.firstOrNull { it.id == categoryId }
        if (category == null) {
            Toast.makeText(context, "Please select a category first", Toast.LENGTH_SHORT).show()
            return
        }
        onSubmit(
            ReportDraft(
                category = category.id,
                categoryLabel = category.label,
                severity = SeverityLevels[severity].lowercase(),
                severityLabel = SeverityLevels[severity],
                people = people,
                vulnerable = vulnerable,
                details = details.trim(),
                lat = userLocation?.latitude,
                lon = userLocation?.longitude,
                accuracyM = userLocation?.accuracyM,
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ReportHeader(onProfile = onProfile)

        ProgressBlock(step = minOf(done + 1, 4), percent = done * 25)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Report an Incident",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkDark,
            )
            Text(
                text = "Direct to Brgy. Military Cut-off BERT",
                fontSize = 13.sp,
                color = InkMuted,
            )
        }

        // 1. Category
        SectionCard(number = 1, title = "Select Category", badge = "REQUIRED", badgeRed = true) {
            IncidentCategories.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { category ->
                        CategoryTile(
                            category = category,
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // 2. Location
        SectionCard(number = 2, title = "Incident Location", badge = "Offline LoRa P2P Locked", badgeRed = false) {
            LocationRow(userLocation = userLocation, onChange = { comingSoon("Change location") })
        }

        // 3. Severity & individuals
        SectionCard(number = 3, title = "Severity & Individuals") {
            Text(
                text = "URGENCY LEVEL",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = InkMuted,
            )
            SeveritySelector(selected = severity, onSelect = { severity = it })

            PeopleCounter(
                count = people,
                onMinus = { if (people > 1) people-- },
                onPlus = { if (people < 99) people++ },
            )

            VulnerableRow(checked = vulnerable, onChange = { vulnerable = it })
        }

        // 4. Photo & voice notes
        SectionCard(number = 4, title = "Photo & Voice Notes", badge = "OPTIONAL", badgeRed = false) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaButton(
                    icon = Icons.Filled.AddAPhoto,
                    title = "Add Photo",
                    subtitle = "Attach evidence",
                    onClick = { comingSoon("Photo") },
                    modifier = Modifier.weight(1f),
                )
                MediaButton(
                    icon = Icons.Filled.Mic,
                    title = "Voice Memo",
                    subtitle = "Tap to record",
                    onClick = { comingSoon("Voice memo") },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "DETAILS / HAZARDS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = InkMuted,
            )
            OutlinedTextField(
                value = details,
                onValueChange = { if (it.length <= 200) details = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Type incident details, hazards, or immediate needs here.", fontSize = 13.sp)
                },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text("${details.length}/200", fontSize = 10.sp, color = InkMuted)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResqRed,
                    unfocusedBorderColor = BorderGray,
                    cursorColor = ResqRed,
                    focusedContainerColor = BlueTint,
                    unfocusedContainerColor = BlueTint,
                ),
            )
        }

        Button(
            onClick = { submit() },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResqRed,
                disabledContainerColor = ResqRed.copy(alpha = 0.6f),
                disabledContentColor = Color.White,
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text("Sending...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("Submit Emergency Report", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "Immediate life danger? Call ", fontSize = 12.sp, color = InkMuted)
            Text(
                text = "911 directly.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
                    }
                },
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Header and progress ----------

@Composable
private fun ReportHeader(onProfile: () -> Unit) {
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

@Composable
private fun ProgressBlock(step: Int, percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "STEP $step OF 4",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ResqRedTint)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "• Incident Details",
                fontSize = 11.sp,
                color = InkMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$percent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color = ResqRed,
            trackColor = BorderGray,
        )
    }
}

// ---------- Section card ----------

@Composable
private fun SectionCard(
    number: Int,
    title: String,
    badge: String? = null,
    badgeRed: Boolean = false,
    content: @Composable () -> Unit,
) {
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
                    text = "$number",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ResqRed,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ResqRedTint)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    modifier = Modifier.weight(1f),
                )
                if (badge != null) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (badgeRed) ResqRed else SafeGreen,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (badgeRed) ResqRedTint else SafeGreenTint)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            content()
        }
    }
}

// ---------- 1. Category ----------

@Composable
private fun CategoryTile(
    category: IncidentCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) ResqRedTint else BlueTint.copy(alpha = 0.5f))
            .border(if (selected) 2.dp else 1.dp, if (selected) ResqRed else BorderGray, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(category.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.tint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = category.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Text(
                text = category.sub,
                fontSize = 10.sp,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(ResqRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ---------- 2. Location ----------

private fun formatCoordinates(lat: Double, lon: Double): String {
    val ns = if (lat >= 0) "N" else "S"
    val ew = if (lon >= 0) "E" else "W"
    return "%.4f° %s, %.4f° %s".format(Locale.US, abs(lat), ns, abs(lon), ew)
}

@Composable
private fun LocationRow(userLocation: UserLocation?, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlueTint.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ResqRed,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            // TODO: reverse-geocode the GPS point (needs internet) or use the nearest known purok offline
            Text(
                text = "Purok 2, Military Cut-off Rd",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Brgy. Military Cut-off, Baguio City",
                fontSize = 11.sp,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = userLocation?.let { formatCoordinates(it.latitude, it.longitude) }
                    ?: "Locating...",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ResqRed,
            )
        }

        Spacer(Modifier.width(8.dp))

        OutlinedButton(
            onClick = onChange,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderGray),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = InkDark,
            ),
        ) {
            Text("Change", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------- 3. Severity and individuals ----------

@Composable
private fun SeveritySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BlueTint.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SeverityLevels.forEachIndexed { index, label ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) ResqRed else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else InkMuted,
                )
            }
        }
    }
}

@Composable
private fun PeopleCounter(count: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlueTint.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "People Needing Help",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Text(
                text = "Verified persons at site",
                fontSize = 11.sp,
                color = InkMuted,
            )
        }

        StepperButton(icon = Icons.Filled.Remove, description = "Fewer", filled = false, onClick = onMinus)
        Text(
            text = "$count",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(icon = Icons.Filled.Add, description = "More", filled = true, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) ResqRed else Color.White)
            .border(1.dp, if (filled) ResqRed else BorderGray, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (filled) Color.White else InkDark,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun VulnerableRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlueTint.copy(alpha = 0.6f))
            .clickable { onChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = ResqRed),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = "Vulnerable Individuals Present",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
            )
            Text(
                text = "Elderly, infants, injured, or mobility impaired",
                fontSize = 11.sp,
                color = InkMuted,
            )
        }
    }
}

// ---------- 4. Media ----------

@Composable
private fun MediaButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(BlueTint.copy(alpha = 0.6f))
            .border(1.dp, BorderGray, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ResqRedTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ResqRed,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkDark)
        Text(text = subtitle, fontSize = 11.sp, color = InkMuted)
    }
}