package com.example.resqhub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedTint = Color(0xFFFDEEEC)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)

/** The app a responder sees: Home, Alerts and Broadcast. */
@Composable
fun ResponderApp(
    profile: ProfileRow,
    onDutyChange: (String, (String) -> Unit) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val uid = Supabase.currentUserId().orEmpty()
    val viewModel: ResponderViewModel = viewModel(key = "responder-$uid")
    val broadcastViewModel: BroadcastViewModel = viewModel(key = "broadcast-$uid")

    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Check for new reports every 8 seconds while the app is open.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(8_000)
        }
    }
    LaunchedEffect(viewModel.authExpired, broadcastViewModel.authExpired) {
        if (viewModel.authExpired || broadcastViewModel.authExpired) onLogout()
    }
    // Reload the announcements each time the Broadcast tab is opened.
    LaunchedEffect(tab) {
        if (tab == 2) broadcastViewModel.refresh()
    }

    val unread = viewModel.incidents.count { it.status == "submitted" }

    Scaffold(
        containerColor = ScreenBg,
        topBar = { ResponderTopBar(onProfile = onLogout) },
        bottomBar = { ResponderBottomBar(selected = tab, unread = unread, onSelect = { tab = it }) },
    ) { innerPadding ->
        when (tab) {
            0 -> ResponderHomeScreen(
                modifier = Modifier.padding(innerPadding),
                profile = profile,
                incidents = viewModel.incidents,
                onDutyChange = { status ->
                    onDutyChange(status) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                },
                onOpenAlerts = { tab = 1 },
            )

            1 -> ResponderAlertsScreen(
                modifier = Modifier.padding(innerPadding),
                incidents = viewModel.incidents,
                isLoading = viewModel.isLoading,
                error = viewModel.error,
                busyIds = viewModel.busyIds,
                onRetry = viewModel::refresh,
                onAction = { incident, newStatus ->
                    viewModel.changeStatus(incident, newStatus) {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                },
            )

            2 -> ResponderBroadcastScreen(
                modifier = Modifier.padding(innerPadding),
                profile = profile,
                announcements = broadcastViewModel.announcements,
                isLoading = broadcastViewModel.isLoading,
                isPosting = broadcastViewModel.isPosting,
                error = broadcastViewModel.error,
                onRetry = broadcastViewModel::refresh,
                onPublish = { category, tone, tag, title, body, target, onDone ->
                    broadcastViewModel.publish(
                        category = category,
                        tone = tone,
                        tag = tag,
                        title = title,
                        body = body,
                        target = target,
                        author = profile,
                        onDone = {
                            Toast.makeText(context, "Announcement published", Toast.LENGTH_SHORT).show()
                            onDone()
                        },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                    )
                },
                onSetActive = { item, active ->
                    broadcastViewModel.setActive(item, active) {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                },
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

@Composable
private fun ResponderTopBar(onProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(InkDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = "resQHub",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "RESP",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = ResqRed,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ResqRedTint)
                .border(1.dp, ResqRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Spacer(Modifier.weight(1f))

        // TEMPORARY: the profile button logs out until we build a profile screen.
        IconButton(
            onClick = onProfile,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ResqRed),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile / Log out",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ResponderBottomBar(selected: Int, unread: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ResqRed,
            selectedTextColor = ResqRed,
            indicatorColor = ResqRedTint,
            unselectedIconColor = InkMuted,
            unselectedTextColor = InkMuted,
        )

        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = colors,
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unread > 0) {
                            Badge(containerColor = ResqRed, contentColor = Color.White) {
                                Text("$unread")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Alerts")
                }
            },
            label = { Text("Alerts", fontSize = 11.sp) },
            colors = colors,
        )
        NavigationBarItem(
            selected = selected == 2,
            onClick = { onSelect(2) },
            icon = { Icon(Icons.Filled.Campaign, contentDescription = "Broadcast") },
            label = { Text("Broadcast", fontSize = 11.sp) },
            colors = colors,
        )
    }
}