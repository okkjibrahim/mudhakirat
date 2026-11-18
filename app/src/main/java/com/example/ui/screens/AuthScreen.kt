package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AuthMode
import com.example.model.AuthUiState
import com.example.ui.components.MzButton
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.theme.FontScaleSteps
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    auth: AuthUiState,
    lang: String,
    fontScaleIdx: Int,
    onDismissIntro: () -> Unit,
    onOpenSheet: (AuthMode) -> Unit,
    onCloseSheet: () -> Unit,
    onSetMode: (AuthMode) -> Unit,
    onSubmitAuth: (String, String) -> Unit,
    onSubmitVerify: (String) -> Unit,
    onSkipLocal: () -> Unit,
    onResendCode: (Boolean) -> Unit,
    onSubmitForgot: (String) -> Unit,
    onSubmitReset: (String, String, String) -> Unit,
    onToggleLang: () -> Unit,
    onChangeFontScale: (Int) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val inf = rememberInfiniteTransition(label = "bird_bob")
    val bobY by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    LaunchedEffect(auth.showWelcomeIntro) {
        if (auth.showWelcomeIntro) {
            delay(1750L)
            onDismissIntro()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mz.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: Language & Font size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mz.surface,
                    border = BorderStroke(1.dp, mz.line),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onChangeFontScale(fontScaleIdx - 1) },
                            enabled = fontScaleIdx > 0,
                            modifier = Modifier.testTag("auth_font_dec"),
                        ) {
                            Text("أ-", style = MaterialTheme.typography.labelMedium, color = mz.ink)
                        }
                        IconButton(
                            onClick = { onChangeFontScale(fontScaleIdx + 1) },
                            enabled = fontScaleIdx < FontScaleSteps.lastIndex,
                            modifier = Modifier.testTag("auth_font_inc"),
                        ) {
                            Text("أ+", style = MaterialTheme.typography.labelMedium, color = mz.ink)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mz.surface,
                    border = BorderStroke(1.dp, mz.line),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleLang)
                        .testTag("auth_lang_toggle"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = mz.accentText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (lang == "ar") "EN" else "عربي",
                            style = MaterialTheme.typography.labelMedium,
                            color = mz.ink,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Minimal refined Book-Bird emblem on Canvas
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(mz.soft)
                        .padding(16.dp)
                        .graphicsLayer { translationY = bobY },
                    contentAlignment = Alignment.Center,
                ) {
                    CalmStudyEmblem(
                        accent = mz.accent,
                        accentDeep = mz.accentText,
                        ink = mz.ink,
                        surface = mz.surface,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = Strings.pk("مذاكرتي", "Mudhakirati", lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = mz.ink,
                    )
                    Text(
                        text = Strings.pk(
                            "منظّم دراسي هادئ يجمع المهام، المواد، مؤقت التركيز، وتقدّمك اليومي في مكان واحد.",
                            "A calm study workspace combining tasks, subjects, focus timer, and daily progress.",
                            lang,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mz.muted,
                        textAlign = TextAlign.Center,
                    )
                }

                // Feature summary pills
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val features = listOf(
                        Strings.pk("مهام وتقويم أسبوعي", "Tasks & Weekly View", lang),
                        Strings.pk("مؤقت تركيز واستراحة", "Focus & Break Timer", lang),
                        Strings.pk("خطط مراجعة مركّزة", "Focused Reviews", lang),
                        Strings.pk("مزامنة والأصدقاء", "Cloud Sync & Friends", lang),
                    )
                    features.forEach { lbl ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = mz.surface,
                            border = BorderStroke(1.dp, mz.line),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = mz.accentText,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(lbl, style = MaterialTheme.typography.labelSmall, color = mz.ink)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedVisibility(visible = !auth.showWelcomeIntro, enter = fadeIn()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MzButton(
                            text = Strings.pk("تسجيل الدخول", "Log in", lang),
                            onClick = { onOpenSheet(AuthMode.LOGIN) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_open_login",
                        )
                        MzGhostButton(
                            text = Strings.pk("إنشاء حساب جديد", "Create account", lang),
                            onClick = { onOpenSheet(AuthMode.REGISTER) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_open_register",
                        )
                        TextButton(
                            onClick = onSkipLocal,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("auth_guest_local"),
                        ) {
                            Text(
                                text = Strings.pk("المتابعة بوضع محلي بدون إنترنت", "Continue in offline local mode", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = mz.muted,
                            )
                        }
                    }
                }
            }
        }
    }

    if (auth.sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseSheet,
            sheetState = sheetState,
            containerColor = mz.surface,
            contentColor = mz.ink,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            AuthSheetContent(
                auth = auth,
                lang = lang,
                onSetMode = onSetMode,
                onSubmitAuth = onSubmitAuth,
                onSubmitVerify = onSubmitVerify,
                onSkipLocal = onSkipLocal,
                onResendCode = onResendCode,
                onSubmitForgot = onSubmitForgot,
                onSubmitReset = onSubmitReset,
            )
        }
    }
}

@Composable
private fun AuthSheetContent(
    auth: AuthUiState,
    lang: String,
    onSetMode: (AuthMode) -> Unit,
    onSubmitAuth: (String, String) -> Unit,
    onSubmitVerify: (String) -> Unit,
    onSkipLocal: () -> Unit,
    onResendCode: (Boolean) -> Unit,
    onSubmitForgot: (String) -> Unit,
    onSubmitReset: (String, String, String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    var email by remember(auth.emailInput, auth.mode) { mutableStateOf(auth.emailInput) }
    var password by remember(auth.mode) { mutableStateOf("") }
    var confirmPassword by remember(auth.mode) { mutableStateOf("") }
    var otpCode by remember(auth.mode) { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = mz.accent,
        unfocusedBorderColor = mz.line,
        focusedContainerColor = mz.field,
        unfocusedContainerColor = mz.field,
        focusedTextColor = mz.ink,
        unfocusedTextColor = mz.ink,
        focusedLabelColor = mz.accentText,
        unfocusedLabelColor = mz.muted,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (auth.mode == AuthMode.LOGIN || auth.mode == AuthMode.REGISTER) {
            MzSegmentedTabs(
                items = listOf(
                    AuthMode.LOGIN to Strings.pk("دخول", "Log in", lang),
                    AuthMode.REGISTER to Strings.pk("حساب جديد", "Sign up", lang),
                ),
                selected = auth.mode,
                onSelect = onSetMode,
                testTagPrefix = "auth_mode_tab",
            )
        }

        val heading = when (auth.mode) {
            AuthMode.LOGIN -> Strings.pk("تسجيل الدخول", "Log in", lang)
            AuthMode.REGISTER -> Strings.pk("إنشاء حساب جديد", "Create account", lang)
            AuthMode.VERIFY -> Strings.pk("تأكيد البريد الإلكتروني", "Verify your email", lang)
            AuthMode.FORGOT -> Strings.pk("استعادة كلمة المرور", "Reset password", lang)
            AuthMode.RESET -> Strings.pk("كلمة المرور الجديدة", "Set new password", lang)
            AuthMode.WELCOME -> ""
        }
        Text(text = heading, style = MaterialTheme.typography.titleLarge, color = mz.ink)

        if (auth.message.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = (if (auth.isSuccessMessage) mz.ok else mz.hot).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, (if (auth.isSuccessMessage) mz.ok else mz.hot).copy(alpha = 0.35f)),
            ) {
                Text(
                    text = auth.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (auth.isSuccessMessage) mz.ok else mz.hot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }

        when (auth.mode) {
            AuthMode.LOGIN, AuthMode.REGISTER -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Strings.pk("البريد الإلكتروني", "Email", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(Strings.pk("كلمة المرور", "Password", lang)) },
                    singleLine = true,
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPw = !showPw }) {
                            Icon(
                                imageVector = if (showPw) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = Strings.pk("إظهار كلمة المرور", "Toggle password visibility", lang),
                                tint = mz.muted,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                )

                if (auth.mode == AuthMode.LOGIN) {
                    TextButton(
                        onClick = { onSetMode(AuthMode.FORGOT) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            Strings.pk("نسيت كلمة المرور؟", "Forgot password?", lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = mz.accentText,
                        )
                    }
                }

                MzButton(
                    text = if (auth.isLoading) Strings.pk("جاري المتابعة...", "Working...", lang)
                    else if (auth.mode == AuthMode.LOGIN) Strings.pk("دخول", "Log in", lang)
                    else Strings.pk("إنشاء الحساب", "Create account", lang),
                    enabled = !auth.isLoading,
                    onClick = { onSubmitAuth(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_submit_button",
                )
            }

            AuthMode.VERIFY -> {
                Text(
                    text = Strings.pk("أدخل الرمز المرسل إلى $email", "Enter the code sent to $email", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = mz.muted,
                )
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text(Strings.pk("رمز التحقق (6 أرقام)", "6-digit code", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_otp_field"),
                )
                MzButton(
                    text = Strings.pk("تأكيد الرمز", "Verify code", lang),
                    enabled = !auth.isLoading,
                    onClick = { onSubmitVerify(otpCode) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_verify_submit",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onResendCode(false) }) {
                        Text(Strings.pk("إعادة إرسال", "Resend code", lang), color = mz.accentText)
                    }
                    TextButton(onClick = onSkipLocal) {
                        Text(Strings.pk("الدخول محليًا", "Use locally", lang), color = mz.muted)
                    }
                }
            }

            AuthMode.FORGOT -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Strings.pk("البريد الإلكتروني", "Email", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_forgot_email"),
                )
                MzButton(
                    text = Strings.pk("إرسال رمز الاستعادة", "Send reset code", lang),
                    enabled = !auth.isLoading,
                    onClick = { onSubmitForgot(email) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_forgot_submit",
                )
                TextButton(
                    onClick = { onSetMode(AuthMode.LOGIN) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(Strings.pk("العودة للدخول", "Back to login", lang), color = mz.muted)
                }
            }

            AuthMode.RESET -> {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text(Strings.pk("رمز الاستعادة", "Reset code", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(Strings.pk("كلمة المرور الجديدة", "New password", lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(Strings.pk("تأكيد كلمة المرور", "Confirm password", lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                MzButton(
                    text = Strings.pk("حفظ كلمة المرور", "Save password", lang),
                    enabled = !auth.isLoading,
                    onClick = { onSubmitReset(otpCode, password, confirmPassword) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { onResendCode(true) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(Strings.pk("إعادة إرسال الرمز", "Resend code", lang), color = mz.accentText)
                }
            }

            AuthMode.WELCOME -> Unit
        }
    }
}

@Composable
private fun CalmStudyEmblem(
    accent: Color,
    accentDeep: Color,
    ink: Color,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Open minimal book
        val leftPage = Path().apply {
            moveTo(w * 0.50f, h * 0.72f)
            quadraticTo(w * 0.30f, h * 0.60f, w * 0.12f, h * 0.66f)
            lineTo(w * 0.12f, h * 0.84f)
            quadraticTo(w * 0.30f, h * 0.78f, w * 0.50f, h * 0.88f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.50f, h * 0.72f)
            quadraticTo(w * 0.70f, h * 0.60f, w * 0.88f, h * 0.66f)
            lineTo(w * 0.88f, h * 0.84f)
            quadraticTo(w * 0.70f, h * 0.78f, w * 0.50f, h * 0.88f)
            close()
        }
        drawPath(leftPage, color = surface)
        drawPath(rightPage, color = surface)
        drawPath(leftPage, color = accentDeep, style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawPath(rightPage, color = accentDeep, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Calm stylized geometric bird / owl reading emblem
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(accent, accentDeep)),
            topLeft = Offset(w * 0.31f, h * 0.22f),
            size = Size(w * 0.38f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.19f, w * 0.19f),
        )
        // Minimal eyes
        drawCircle(color = surface, radius = w * 0.055f, center = Offset(w * 0.42f, h * 0.38f))
        drawCircle(color = surface, radius = w * 0.055f, center = Offset(w * 0.58f, h * 0.38f))
        drawCircle(color = ink, radius = w * 0.025f, center = Offset(w * 0.42f, h * 0.38f))
        drawCircle(color = ink, radius = w * 0.025f, center = Offset(w * 0.58f, h * 0.38f))

        // Beak
        val beak = Path().apply {
            moveTo(w * 0.47f, h * 0.43f)
            lineTo(w * 0.53f, h * 0.43f)
            lineTo(w * 0.50f, h * 0.49f)
            close()
        }
        drawPath(beak, color = Color(0xFFD97706))
    }
}
