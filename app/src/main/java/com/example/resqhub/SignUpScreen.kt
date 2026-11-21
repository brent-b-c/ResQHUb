package com.example.resqhub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ResqRed = Color(0xFFA81F1B)
private val ResqRedLight = Color(0xFFC8372D)
private val SafeGreen = Color(0xFF2E9E6B)
private val ScreenBg = Color(0xFFF6F7F9)
private val InkDark = Color(0xFF1B1F2A)
private val InkMuted = Color(0xFF6B7280)
private val BorderGray = Color(0xFFE5E7EB)

/** Create-account screen for residents. Responder accounts are made by the barangay in Supabase. */
@Composable
fun SignUpScreen(
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onSignUp: (fullName: String, phone: String, email: String, password: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus()
        localError = when {
            fullName.trim().length < 2 -> "Please enter your full name."
            phone.filter { it.isDigit() }.length < 7 -> "Please enter a valid mobile number."
            !email.contains("@") || !email.contains(".") -> "Please enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            password != confirm -> "The two passwords do not match."
            else -> null
        }
        if (localError == null) onSignUp(fullName.trim(), phone.trim(), email.trim(), password)
    }

    val shownError = errorMessage ?: localError

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(ResqRedLight, ResqRed))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("Create your account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkDark)
        Text(
            text = "Join resQHub to send reports and get barangay alerts.",
            fontSize = 13.sp,
            color = InkMuted,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                SignUpField(
                    value = fullName,
                    onChange = { fullName = it; localError = null },
                    label = "Full name",
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Text,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
                SignUpField(
                    value = phone,
                    onChange = { phone = it; localError = null },
                    label = "Mobile number",
                    icon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Phone,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
                SignUpField(
                    value = email,
                    onChange = { email = it; localError = null },
                    label = "Email address",
                    icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
                SignUpField(
                    value = password,
                    onChange = { password = it; localError = null },
                    label = "Password",
                    icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Password,
                    hidden = !passwordVisible,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
                SignUpField(
                    value = confirm,
                    onChange = { confirm = it; localError = null },
                    label = "Confirm password",
                    icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Password,
                    hidden = !passwordVisible,
                    imeAction = ImeAction.Done,
                    onNext = { submit() },
                )

                Spacer(Modifier.height(12.dp))

                if (shownError != null) {
                    Text(
                        text = shownError,
                        color = ResqRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (infoMessage != null) {
                    Text(
                        text = infoMessage,
                        color = SafeGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    if (isLoading) listOf(ResqRed.copy(alpha = 0.6f), ResqRedLight.copy(alpha = 0.6f))
                                    else listOf(ResqRed, ResqRedLight)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = InkMuted, fontSize = 14.sp)
            TextButton(onClick = onBack, enabled = !isLoading) {
                Text("Log in", color = ResqRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SignUpField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType,
    onNext: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    hidden: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = icon,
        trailingIcon = trailing,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ResqRed,
            unfocusedBorderColor = BorderGray,
            focusedLabelColor = ResqRed,
            cursorColor = ResqRed,
            focusedLeadingIconColor = ResqRed,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
    )
}