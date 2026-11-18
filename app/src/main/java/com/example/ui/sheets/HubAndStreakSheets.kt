package com.example.ui.sheets

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.ExportUtils
import com.example.model.AppState
import com.example.model.ChatState
import com.example.ui.components.AnimatedFlameIcon
import com.example.ui.components.MzButton
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.UserAvatar
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.SubjectColors
import com.example.ui.theme.colorToHex
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCellSheetContent(
    state: AppState,
    cellKey: String,
    onSave: (String, String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    var value by remember(cellKey) { mutableStateOf(state.sched.cells[cellKey] ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(Strings.pk("تعديل الحصة", "Edit period", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(Strings.pk("اسم المادة", "Subject", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.subjects.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.subjects.forEach { sb ->
                    ChoiceChip(
                        label = sb.name,
                        selected = value == sb.name,
                        onClick = { value = sb.name },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (value.isNotBlank()) {
                MzGhostButton(
                    text = Strings.pk("مسح", "Clear", lang),
                    isDanger = true,
                    onClick = { onSave(cellKey, "") },
                )
            }
            MzButton(
                text = Strings.pk("حفظ", "Save", lang),
                onClick = { onSave(cellKey, value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ScheduleDaysSheetContent(
    state: AppState,
    onToggleDay: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val dayNames = if (lang == "en") Strings.daysLongEn else Strings.daysLongAr

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Strings.pk("أيام الدوام بالجدول", "Visible schedule days", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)
        Strings.scheduleDayOrder.forEach { dayIdx ->
            val checked = state.sched.days.contains(dayIdx)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleDay(dayIdx) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = { onToggleDay(dayIdx) })
                Text(dayNames[dayIdx], style = MaterialTheme.typography.bodyLarge, color = mz.ink)
            }
        }
        MzButton(text = Strings.pk("تم", "Done", lang), onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun NoteEditorSheetContent(
    state: AppState,
    noteId: String?,
    onSave: (String?, String, String, String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val existing = remember(noteId, state.notes) { state.notes.firstOrNull { it.id == noteId } }

    var title by remember(noteId) { mutableStateOf(existing?.title ?: "") }
    var txt by remember(noteId) { mutableStateOf(existing?.txt ?: "") }
    var colorHex by remember(noteId) { mutableStateOf(existing?.color ?: colorToHex(SubjectColors[5])) }
    var pin by remember(noteId) { mutableStateOf(existing?.pin ?: false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (existing != null) Strings.pk("تعديل الملاحظة", "Edit note", lang) else Strings.pk("ملاحظة جديدة", "New note", lang),
            style = MaterialTheme.typography.titleLarge,
            color = mz.ink,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(Strings.pk("العنوان (اختياري)", "Title (optional)", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = txt,
            onValueChange = { txt = it },
            label = { Text(Strings.pk("المحتوى", "Note content", lang)) },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectColors.forEach { c ->
                    val hex = colorToHex(c)
                    val isSel = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (isSel) Modifier.border(2.5.dp, mz.ink, CircleShape) else Modifier)
                            .clickable { colorHex = hex },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Strings.pk("تثبيت", "Pin", lang), style = MaterialTheme.typography.bodyMedium, color = mz.ink)
                Switch(checked = pin, onCheckedChange = { pin = it })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (existing != null) {
                MzGhostButton(text = Strings.pk("حذف", "Delete", lang), isDanger = true, onClick = { onDelete(existing.id) })
            }
            MzButton(
                text = Strings.pk("حفظ", "Save", lang),
                enabled = txt.trim().isNotEmpty(),
                onClick = { onSave(noteId, title, txt, colorHex, pin) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreakDetailsSheetContent(
    state: AppState,
    streakFriendsAlert: Boolean,
    onToggleStreakAlert: (Boolean) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = state.lang
    val ar = state.arDigits
    val streak = DateUtils.computeStreak(state)
    val longest = DateUtils.computeLongestStreak(state)
    val stageName = Strings.flameStageName(streak, lang)
    val previewBmp = remember(state.act, state.logs, streak, mz) {
        ExportUtils.renderStreakBitmap(state, mz)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedFlameIcon(streak = streak, size = 54.dp)
        Text(
            text = "${DateUtils.applyDigits(streak.toString(), lang, ar)} ${Strings.daysWord(streak, lang)}",
            style = rubikNumStyle(MaterialTheme.typography.headlineMedium),
            color = mz.ink,
        )
        Text(
            text = "$stageName · ${Strings.pk("أطول سلسلة", "Longest", lang)}: ${DateUtils.applyDigits(longest.toString(), lang, ar)}",
            style = MaterialTheme.typography.bodyMedium,
            color = mz.muted,
        )

        // Last 7 days checks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            (-6..0).forEach { offset ->
                val dk = DateUtils.addDaysKey(offset)
                val active = DateUtils.actOn(state, dk)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (active) mz.accent else mz.field)
                            .border(1.dp, if (active) mz.accent else mz.line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = mz.accentInk, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(DateUtils.formatWeekdayShort(dk, lang, ar), style = MaterialTheme.typography.labelSmall, color = mz.muted)
                }
            }
        }

        // Milestones 3, 7, 14, 30, 60, 100
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 7, 14, 30, 60, 100).forEach { ms ->
                val hit = longest >= ms || streak >= ms
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hit) mz.soft else mz.field,
                    border = BorderStroke(1.dp, if (hit) mz.accent else mz.line),
                ) {
                    Text(
                        text = "${DateUtils.applyDigits("$ms", lang, ar)} ${Strings.daysWord(ms, lang)}",
                        style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                        color = if (hit) mz.accentText else mz.muted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Alert friends toggle
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = mz.field,
            border = BorderStroke(1.dp, mz.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = Strings.pk("تنبيه الأصدقاء عند الدراسة", "Alert friends when studying", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mz.ink,
                )
                Switch(checked = streakFriendsAlert, onCheckedChange = onToggleStreakAlert)
            }
        }

        // Shareable Card preview + Save/Share
        Image(
            bitmap = previewBmp.asImageBitmap(),
            contentDescription = Strings.pk("بطاقة الستريك", "Streak card preview", lang),
            modifier = Modifier
                .width(160.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, mz.line, RoundedCornerShape(16.dp)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MzGhostButton(
                text = Strings.pk("حفظ الصورة", "Save PNG", lang),
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val bos = java.io.ByteArrayOutputStream()
                        previewBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos)
                        val uri = ExportUtils.saveToDownloads(ctx, "mudhakirati-streak-${DateUtils.todayKey()}.png", "image/png", bos.toByteArray())
                        withContext(Dispatchers.Main) {
                            onShowToast(if (uri != null) Strings.pk("تم حفظ البطاقة بالتنزيلات", "Card saved to Downloads", lang) else Strings.pk("تعذّر الحفظ", "Save failed", lang))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            MzButton(
                text = Strings.pk("مشاركة", "Share", lang),
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val f = File(ctx.cacheDir, "streak-card.png")
                        FileOutputStream(f).use { previewBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                        withContext(Dispatchers.Main) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(send, Strings.pk("مشاركة الستريك", "Share streak", lang)))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun FriendChatSheetContent(
    chat: ChatState,
    lang: String,
    arDigits: Boolean,
    onToggleNickname: () -> Unit,
    onSaveNickname: (String) -> Unit,
    onBlockFriend: () -> Unit,
    onSendMessage: (String, String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    var msgInput by remember { mutableStateOf("") }
    var nickInput by remember(chat.code) { mutableStateOf(chat.name) }
    val listState = rememberLazyListState()

    LaunchedEffect(chat.msgs.size) {
        if (chat.msgs.isNotEmpty()) {
            listState.animateScrollToItem(chat.msgs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp, max = 580.dp)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserAvatar(name = chat.name, photoDataUrl = chat.photo, size = 40.dp)
            Text(chat.name, style = MaterialTheme.typography.titleMedium, color = mz.ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleNickname) {
                Icon(Icons.Rounded.Edit, contentDescription = Strings.pk("تغيير الاسم المستعار", "Rename nickname", lang), tint = mz.muted)
            }
            IconButton(onClick = onBlockFriend) {
                Icon(Icons.Rounded.Block, contentDescription = Strings.pk("حظر", "Block", lang), tint = mz.hot)
            }
        }

        if (chat.nicknameInputOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nickInput,
                    onValueChange = { nickInput = it },
                    label = { Text(Strings.pk("اللقب المحلي", "Nickname", lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                MzButton(text = Strings.pk("حفظ", "Save", lang), onClick = { onSaveNickname(nickInput) })
            }
        }

        HorizontalDivider(color = mz.line)

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chat.msgs, key = { it.id }) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (m.mine) mz.accent else mz.field,
                        border = if (m.mine) null else BorderStroke(1.dp, mz.line),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = m.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (m.mine) mz.accentInk else mz.ink,
                            )
                            if (m.mine) {
                                val read = m.id <= chat.readUpto
                                Text(
                                    text = if (read) "✓✓" else "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (if (m.mine) mz.accentInk else mz.muted).copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick cheers strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Strings.quickCheers(lang).forEach { cheer ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = mz.soft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSendMessage(cheer, "cheer") },
                ) {
                    Text(
                        text = cheer,
                        style = MaterialTheme.typography.labelSmall,
                        color = mz.accentText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        // Composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = msgInput,
                onValueChange = { msgInput = it },
                placeholder = { Text(Strings.pk("اكتب رسالة...", "Write a message...", lang)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors(),
                modifier = Modifier.weight(1f).testTag("chat_message_input"),
            )
            IconButton(
                onClick = {
                    val clean = msgInput.trim()
                    if (clean.isNotEmpty()) {
                        onSendMessage(clean, "text")
                        msgInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(mz.accent)
                    .testTag("chat_send_button"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = Strings.pk("إرسال", "Send", lang), tint = mz.accentInk)
            }
        }
    }
}
