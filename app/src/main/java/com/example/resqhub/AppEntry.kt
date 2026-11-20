package com.example.resqhub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The first screen after login. It checks the account's role and opens the
 * citizen app or the responder app. (MainActivity still calls DashboardScreen.)
 */
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val uid = Supabase.currentUserId().orEmpty()
    // A separate ViewModel per user, so logging in as someone else never shows the previous user's data.
    val profileViewModel: ProfileViewModel = viewModel(key = "profile-$uid")

    LaunchedEffect(profileViewModel.authExpired) {
        if (profileViewModel.authExpired) onLogout()
    }

    val profile = profileViewModel.profile
    when {
        profile == null && profileViewModel.isLoading -> CenteredMessage(
            message = "Loading your account...",
            showSpinner = true,
        )

        profile == null -> CenteredMessage(
            message = profileViewModel.error ?: "Could not load your account.",
            onRetry = profileViewModel::load,
            onLogout = onLogout,
        )

        profile.role == "responder" || profile.role == "admin" ->
            ResponderApp(profile = profile, onDutyChange = { s, e -> profileViewModel.setDuty(s, e) }, onLogout = onLogout)

        else -> CitizenApp(onLogout = onLogout)
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    showSpinner: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showSpinner) {
            CircularProgressIndicator(color = Color(0xFFA81F1B), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA81F1B)),
            ) {
                Text("Try again")
            }
        }
        if (onLogout != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(14.dp)) {
                Text("Log out")
            }
        }
    }
}