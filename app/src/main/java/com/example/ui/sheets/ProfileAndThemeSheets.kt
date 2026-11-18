package com.example.ui.sheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExportUtils
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.ui.components.MzButton
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppPalette
import com.example.ui.theme.AppPalettes
import com.example.ui.theme.FontScaleSteps
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.findPalette
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsSheetContent(
    state: AppState,
    userEmail: String,
    fontScaleIdx: Int,
    onPickPhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onChangeFontScale: (Int) -> Unit,
    onSetLayout: (String) -> Unit,
    onOpenThemePicker: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetArabicDigits: (Boolean) -> Unit,
    onSetLockDone: (Boolean) -> Unit,
    onUpdateDailyReminder: (Boolean?, String?, String?) -> Unit,
    onExportFile: (String) -> Unit,
    onImportBackup: () -> Unit,
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lang = state.lang
    val ar = state.arDigits

    var nameInput by remember(state.profile.name) { mutableStateOf(state.profile.name) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var pwOpen by remember { mutableStateOf(false) }
    var curPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var pwMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Profile Header + Photo Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.clickable(onClick = onPickPhoto),
            ) {
                UserAvatar(name = state.profile.name, photoDataUrl = state.profile.photo, size = 62.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(mz.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = Strings.pk("تغيير الصورة", "Change photo", lang), tint = mz.accentInk, modifier = Modifier.size(14.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(userEmail, style = MaterialTheme.typography.bodySmall, color = mz.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.fid.isNotEmpty()) {
                    val fmtId = Strings.formatFriendId(state.fid)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(fmtId))
                            onShowToast(Strings.pk("تم نسخ الـ ID", "ID copied", lang))
                        },
                    ) {
                        Text("ID: $fmtId", style = rubikNumStyle(MaterialTheme.typography.labelMedium), color = mz.accentText)
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(13.dp))
                    }
                }
                if (state.profile.photo.isNotEmpty()) {
                    Text(
                        text = Strings.pk("حذف الصورة", "Remove photo", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = mz.hot,
                        modifier = Modifier.clickable(onClick = onDeletePhoto),
                    )
                }
            }
        }

        // Display name edit
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(Strings.pk("الاسم", "Display name", lang)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors(),
                modifier = Modifier.weight(1f),
            )
            MzButton(
                text = Strings.pk("حفظ", "Save", lang),
                onClick = { onUpdateProfileName(nameInput) },
            )
        }

        HorizontalDivider(color = mz.line)

        // Theme Picker launch row
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = mz.field,
            border = BorderStroke(1.dp, mz.line),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenThemePicker)
                .testTag("settings_open_theme_picker"),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = mz.accentText)
                    Column {
                        Text(Strings.pk("الثيم والألوان", "Theme & Color Palettes", lang), style = MaterialTheme.typography.titleSmall, color = mz.ink)
                        val curPalette = findPalette(state.palette)
                        Text(
                            text = when (state.theme) {
                                "light" -> Strings.pk("نهاري افتراضي", "System Light", lang)
                                "dark" -> Strings.pk("ليلي افتراضي", "System Dark", lang)
                                "auto" -> Strings.pk("تلقائي حسب النظام", "Auto System", lang)
                                else -> Strings.pk(curPalette.ar, curPalette.en, lang)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = mz.muted,
                        )
                    }
                }
                Text(Strings.pk("تغيير", "Change", lang), style = MaterialTheme.typography.labelMedium, color = mz.accentText)
            }
        }

        // Language
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Strings.pk("اللغة", "Language", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            MzSegmentedTabs(
                items = listOf("ar" to "العربية", "en" to "English"),
                selected = state.lang,
                onSelect = onSetLanguage,
                testTagPrefix = "settings_lang",
            )
        }

        // Font Size stepper
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = mz.field,
            border = BorderStroke(1.dp, mz.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(Strings.pk("حجم الخط", "Font size", lang), style = MaterialTheme.typography.bodyMedium, color = mz.ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { onChangeFontScale(fontScaleIdx - 1) }, enabled = fontScaleIdx > 0) {
                        Text("أ-", style = MaterialTheme.typography.labelLarge, color = mz.ink)
                    }
                    Text(Strings.fontSizeName(fontScaleIdx, lang), style = MaterialTheme.typography.labelMedium, color = mz.accentText)
                    IconButton(onClick = { onChangeFontScale(fontScaleIdx + 1) }, enabled = fontScaleIdx < FontScaleSteps.lastIndex) {
                        Text("أ+", style = MaterialTheme.typography.labelLarge, color = mz.ink)
                    }
                }
            }
        }

        // Display Layout Mode
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Strings.pk("حجم العرض", "Layout density", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "auto" to Strings.pk("تلقائي", "Auto", lang),
                    "phone" to Strings.pk("موبايل", "Phone", lang),
                    "tablet" to Strings.pk("تابلت", "Tablet", lang),
                    "laptop" to Strings.pk("عريض", "Wide", lang),
                ).forEach { (k, lbl) ->
                    ChoiceChip(label = lbl, selected = state.layout == k, onClick = { onSetLayout(k) })
                }
            }
        }

        // Toggles: Arabic Digits & Lock Achievements
        if (lang == "ar") {
            SettingToggleRow(
                label = "استخدام الأرقام العربية الشرقية (٠١٢٣)",
                checked = state.arDigits,
                onCheckedChange = onSetArabicDigits,
            )
        }
        SettingToggleRow(
            label = Strings.pk("قفل الإنجازات المنجزة (منع التراجع)", "Lock completed items", lang),
            checked = state.lockDone,
            onCheckedChange = onSetLockDone,
        )

        // Daily Reminder Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = mz.field,
            border = BorderStroke(1.dp, mz.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(Strings.pk("التذكير اليومي للدراسة", "Daily Study Reminder", lang), style = MaterialTheme.typography.titleSmall, color = mz.ink)
                    Switch(checked = state.remind.on, onCheckedChange = { onUpdateDailyReminder(it, null, null) })
                }
                if (state.remind.on) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showReminderTimePicker = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(Strings.pk("الوقت", "Time", lang), style = MaterialTheme.typography.bodyMedium, color = mz.muted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(16.dp))
                            Text(
                                text = DateUtils.formatTime12(state.remind.time, lang, ar),
                                style = rubikNumStyle(MaterialTheme.typography.labelLarge),
                                color = mz.accentText,
                            )
                        }
                    }
                    if (state.subjects.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChoiceChip(
                                label = Strings.pk("أي مادة", "Any subject", lang),
                                selected = state.remind.subj.isEmpty(),
                                onClick = { onUpdateDailyReminder(null, null, "") },
                            )
                            state.subjects.forEach { sb ->
                                ChoiceChip(
                                    label = sb.name,
                                    selected = state.remind.subj == sb.id,
                                    onClick = { onUpdateDailyReminder(null, null, sb.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export / Backup / Print
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Strings.pk("التصدير والنسخ الاحتياطي", "Export & Backup", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MzGhostButton(text = Strings.pk("طباعة / PDF", "Print / PDF", lang), onClick = { ExportUtils.printStudyReport(ctx, state) })
                MzGhostButton(text = "Excel (.xls)", onClick = { onExportFile("xls") })
                MzGhostButton(text = Strings.pk("مهام CSV", "Tasks CSV", lang), onClick = { onExportFile("csv_tasks") })
                MzGhostButton(text = Strings.pk("سجل CSV", "Log CSV", lang), onClick = { onExportFile("csv_logs") })
                MzGhostButton(text = Strings.pk("تقويم .ics", "Calendar .ics", lang), onClick = { onExportFile("ics") })
                MzGhostButton(text = Strings.pk("نسخة احتياطية JSON", "Backup JSON", lang), onClick = { onExportFile("json") })
                MzGhostButton(text = Strings.pk("استيراد نسخة", "Import Backup", lang), onClick = onImportBackup)
            }
        }

        // Change password collapsible
        TextButton(onClick = { pwOpen = !pwOpen }) {
            Text(Strings.pk("تغيير كلمة المرور", "Change password", lang), color = mz.accentText)
        }
        if (pwOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = curPw,
                    onValueChange = { curPw = it },
                    label = { Text(Strings.pk("كلمة المرور الحالية", "Current password", lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it },
                    label = { Text(Strings.pk("كلمة المرور الجديدة", "New password", lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pwMsg != null) {
                    Text(pwMsg!!.second, style = MaterialTheme.typography.bodySmall, color = if (pwMsg!!.first) mz.ok else mz.hot)
                }
                MzButton(
                    text = Strings.pk("تحديث كلمة المرور", "Update password", lang),
                    onClick = {
                        onChangePassword(curPw, newPw) { ok, msg ->
                            pwMsg = ok to msg
                            if (ok) { curPw = ""; newPw = "" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        HorizontalDivider(color = mz.line)

        // Developer Telegram + Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/okkj0")))
                } catch (_: Exception) {}
            }) {
                Text(Strings.pk("تواصل مع المطور (@okkj0)", "Contact developer (@okkj0)", lang), color = mz.muted)
            }
            MzGhostButton(
                text = Strings.pk("تسجيل الخروج", "Log out", lang),
                isDanger = true,
                onClick = onLogout,
                testTag = "settings_logout_button",
            )
        }
    }

    if (showReminderTimePicker) {
        val parts = state.remind.time.split(":")
        val tp = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 18,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            containerColor = mz.surface,
            title = { Text(Strings.pk("وقت التذكير اليومي", "Daily reminder time", lang), color = mz.ink) },
            text = { TimePicker(state = tp) },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateDailyReminder(null, "%02d:%02d".format(tp.hour, tp.minute), null)
                    showReminderTimePicker = false
                }) { Text(Strings.pk("حفظ", "Save", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text(Strings.pk("إلغاء", "Cancel", lang)) }
            },
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = mz.field,
        border = BorderStroke(1.dp, mz.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = mz.ink, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun ThemePickerSheetContent(
    state: AppState,
    onSelectTheme: (String, String?) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(Strings.pk("المظهر وثيمات الألوان", "Appearance & Themes", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)

        // Mode switcher: Custom / Auto / Light / Dark
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "custom" to Strings.pk("الباليتات", "Palettes", lang),
                "auto" to Strings.pk("تلقائي", "Auto", lang),
                "light" to Strings.pk("نهاري", "Light", lang),
                "dark" to Strings.pk("ليلي", "Dark", lang),
            ).forEach { (mode, lbl) ->
                val sel = state.theme == mode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (sel) mz.accent else mz.field,
                    border = BorderStroke(1.dp, if (sel) mz.accent else mz.line),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTheme(mode, null) }
                        .testTag("theme_mode_$mode"),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            color = if (sel) mz.accentInk else mz.ink,
                        )
                    }
                }
            }
        }

        // 3-column miniature visual card preview grid of ALL 13 palettes (white-yellow first!)
        val chunked = remember { AppPalettes.chunked(3) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { pal ->
                        val isActive = state.theme == "custom" && state.palette == pal.id
                        PalettePreviewCard(
                            palette = pal,
                            selected = isActive,
                            lang = lang,
                            onClick = { onSelectTheme("custom", pal.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePreviewCard(
    palette: AppPalette,
    selected: Boolean,
    lang: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mz = LocalMudhakiratiColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("palette_${palette.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.bg)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) mz.accent else palette.border,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(7.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(palette.text.copy(alpha = 0.7f)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(palette.primary),
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.secondary),
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(palette.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = if (palette.isDark) Color.Black else Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Text(
            text = Strings.pk(palette.ar, palette.en, lang),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) mz.accentText else mz.ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
