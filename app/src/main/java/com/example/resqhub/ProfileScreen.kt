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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedTint = Color(0xFFFDEEEC)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val BorderGray = Color(0xFFE5E7EB)

/** The user's own profile. Used by both the citizen app and the responder app. */
@Composable
fun ProfileScreen(
    profile: ProfileRow?,
    email: String,
    isSaving: Boolean,
    onSave: (fullName: String, phone: String, onDone: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(profile?.fullName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(profile?.phone.orEmpty()) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    // The profile may arrive a moment after the screen opens; fill empty fields then.
    LaunchedEffect(profile?.id) {
        if (name.isBlank()) name = profile?.fullName.orEmpty()
        if (phone.isBlank()) phone = profile?.phone.orEmpty()
    }

    val isResponder = profile?.role == "responder" || profile?.role == "admin"
    val changed = name.trim() != profile?.fullName.orEmpty() || phone.trim() != profile?.phone.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkDark)
            }
            Text(
                text = "My Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkDark,
            )
        }

        // ---- Identity ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val initials = (profile?.fullName.orEmpty())
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "?" }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isResponder) InkDark else ResqRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }

                Text(
                    text = profile?.fullName?.ifBlank { null } ?: "Your name",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )
                if (email.isNotBlank()) {
                    Text(text = email, fontSize = 12.sp, color = InkMuted)
                }
                Text(
                    text = if (isResponder) "BERT RESPONDER" else "RESIDENT",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isResponder) InkDark else ResqRed,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isResponder) ScreenBg else ResqRedTint)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )

                if (isResponder) {
                    val duty = listOf(profile?.title.orEmpty(), profile?.unitName.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (duty.isNotBlank()) {
                        Text(text = duty, fontSize = 12.sp, color = InkMuted)
                    }
                }
            }
        }

        // ---- Edit details ----
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
                    text = "Personal details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                )

                ProfileField("Full name", name, { name = it; localError = null }, KeyboardType.Text, !isSaving)
                ProfileField("Mobile number", phone, { phone = it; localError = null }, KeyboardType.Phone, !isSaving)

                if (isResponder) {
                    Text(
                        text = "Your title and unit are set by the barangay administrator.",
                        fontSize = 11.sp,
                        color = InkMuted,
                    )
                }

                if (localError != null) {
                    Text(text = localError.orEmpty(), color = ResqRed, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (name.trim().length < 2) {
                            localError = "Please enter your full name."
                        } else {
                            onSave(name.trim(), phone.trim()) { localError = null }
                        }
                    },
                    enabled = changed && !isSaving,
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
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("Save changes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---- Log out ----
        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, ResqRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ResqRed),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You will need to log in again to send reports.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Log out", color = ResqRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("Cancel", color = InkMuted)
                }
            },
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ResqRed,
            unfocusedBorderColor = BorderGray,
            focusedLabelColor = ResqRed,
            cursorColor = ResqRed,
        ),
    )
}