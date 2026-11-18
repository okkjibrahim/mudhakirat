package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.SubjectPart
import com.example.ui.components.MzButton
import com.example.ui.components.MzGhostButton
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.SubjectColors
import com.example.ui.theme.colorToHex
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import java.util.Calendar
import java.util.TimeZone

@Composable
fun sheetFieldColors() = LocalMudhakiratiColors.current.let { mz ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = mz.accent,
        unfocusedBorderColor = mz.line,
        focusedContainerColor = mz.field,
        unfocusedContainerColor = mz.field,
        focusedTextColor = mz.ink,
        unfocusedTextColor = mz.ink,
        focusedLabelColor = mz.accentText,
        unfocusedLabelColor = mz.muted,
    )
}

@Composable
fun WelcomeSheetContent(
    state: AppState,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    var name by remember { mutableStateOf(state.profile.name) }
    var add6th by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = Strings.pk("مرحبًا بك في مذاكرتي", "Welcome to Mudhakirati", lang),
            style = MaterialTheme.typography.headlineSmall,
            color = mz.ink,
        )
        Text(
            text = Strings.pk("اكتب اسمك لنخصّص تجربتك، ويمكنك إضافة مواد السادس بضغطة.", "Enter your name to personalize your space.", lang),
            style = MaterialTheme.typography.bodyMedium,
            color = mz.muted,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(Strings.pk("الاسم", "Your name", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("welcome_name_input"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { add6th = !add6th }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = add6th, onCheckedChange = { add6th = it })
            Text(
                text = Strings.pk("إضافة مواد السادس الإعدادي السبعة", "Add 7 core 6th-grade subjects", lang),
                style = MaterialTheme.typography.bodyMedium,
                color = mz.ink,
            )
        }
        MzButton(
            text = Strings.pk("ابدأ الآن", "Get started", lang),
            onClick = { onSave(name, add6th) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "welcome_save_button",
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(Strings.pk("تخطي", "Skip", lang), color = mz.muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheetContent(
    state: AppState,
    taskId: String?,
    presetDue: String?,
    onSave: (String?, String, String, String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val existing = remember(taskId, state.tasks) { state.tasks.firstOrNull { it.id == taskId } }

    var title by remember(taskId) { mutableStateOf(existing?.title ?: "") }
    var subjId by remember(taskId) { mutableStateOf(existing?.subj ?: "") }
    var due by remember(taskId, presetDue) { mutableStateOf(existing?.due ?: presetDue ?: DateUtils.todayKey()) }
    var pri by remember(taskId) { mutableStateOf(existing?.pri ?: "med") }
    var rem by remember(taskId) { mutableStateOf(existing?.rem ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (existing != null) Strings.pk("تعديل المهمة", "Edit task", lang) else Strings.pk("مهمة جديدة", "New task", lang),
            style = MaterialTheme.typography.titleLarge,
            color = mz.ink,
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(Strings.pk("عنوان المهمة", "Task title", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        )

        // Subject selector
        if (state.subjects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Strings.pk("المادة", "Subject", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val noneSel = subjId.isEmpty()
                    ChoiceChip(
                        label = Strings.pk("بدون", "None", lang),
                        selected = noneSel,
                        onClick = { subjId = "" },
                    )
                    state.subjects.forEach { sb ->
                        ChoiceChip(
                            label = sb.name,
                            selected = subjId == sb.id,
                            accent = parseHexColor(sb.color),
                            onClick = { subjId = sb.id },
                        )
                    }
                }
            }
        }

        // Priority selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Strings.pk("الأولوية", "Priority", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("high", "med", "low").forEach { p ->
                    ChoiceChip(
                        label = Strings.priorityLabel(p, lang),
                        selected = pri == p,
                        accent = if (p == "high") mz.hot else mz.accent,
                        onClick = { pri = p },
                    )
                }
            }
        }

        // Due date presets + picker
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Strings.pk("الموعد", "Due date", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val td = DateUtils.todayKey()
                val tm = DateUtils.addDaysKey(1)
                ChoiceChip(label = Strings.pk("اليوم", "Today", lang), selected = due == td, onClick = { due = td })
                ChoiceChip(label = Strings.pk("غدًا", "Tomorrow", lang), selected = due == tm, onClick = { due = tm })
                ChoiceChip(label = Strings.pk("بدون موعد", "None", lang), selected = due.isBlank(), onClick = { due = ""; rem = "" })
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = mz.field,
                    border = BorderStroke(1.dp, mz.line),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (due.isNotBlank() && due != td && due != tm) DateUtils.formatDateShort(due, lang, ar)
                            else Strings.pk("اختر تاريخ", "Pick date", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = mz.ink,
                        )
                    }
                }
            }
        }

        // Optional reminder time
        if (due.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { showTimePicker = true }.padding(vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (rem.isNotBlank()) "${Strings.pk("تذكير", "Reminder", lang)}: ${DateUtils.formatTime12(rem, lang, ar)}"
                        else Strings.pk("إضافة وقت للتذكير (اختياري)", "Add reminder time (optional)", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mz.ink,
                    )
                }
                if (rem.isNotBlank()) {
                    IconButton(onClick = { rem = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = Strings.pk("مسح التذكير", "Clear reminder", lang), tint = mz.muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (existing != null) {
                MzGhostButton(
                    text = Strings.pk("حذف", "Delete", lang),
                    isDanger = true,
                    onClick = { onDelete(existing.id); onClose() },
                )
            }
            MzButton(
                text = Strings.pk("حفظ", "Save", lang),
                enabled = title.trim().isNotEmpty(),
                onClick = { onSave(taskId, title, subjId, due, pri, rem) },
                modifier = Modifier.weight(1f),
                testTag = "task_save_button",
            )
        }
    }

    if (showDatePicker) {
        val initMs = remember(due) {
            DateUtils.parseDateKey(due)?.let {
                it.time + TimeZone.getDefault().getOffset(it.time)
            } ?: System.currentTimeMillis()
        }
        val dpState = rememberDatePickerState(initialSelectedDateMillis = initMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { utcMs ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
                        val key = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                        due = key
                    }
                    showDatePicker = false
                }) { Text(Strings.pk("تأكيد", "OK", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(Strings.pk("إلغاء", "Cancel", lang)) }
            },
        ) {
            DatePicker(state = dpState)
        }
    }

    if (showTimePicker) {
        val parts = rem.split(":")
        val tpState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 18,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = mz.surface,
            title = { Text(Strings.pk("وقت التذكير", "Reminder time", lang), color = mz.ink) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    rem = "%02d:%02d".format(tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text(Strings.pk("حفظ", "Save", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(Strings.pk("إلغاء", "Cancel", lang)) }
            },
        )
    }
}

@Composable
fun SubjectTypePickerSheetContent(
    lang: String,
    onPick: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(Strings.pk("ماذا تريد أن تضيف؟", "What would you like to add?", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)
        listOf(
            Triple(ActiveSheet.SubjectEditor(null), Strings.pk("مادة دراسية", "Study Subject", lang), Strings.pk("مع فصولها وهدف ساعاتها الأسبوعي", "With chapters and weekly hour target", lang)),
            Triple(ActiveSheet.ReviewEditor("rev", null), Strings.pk("مراجعة مركّزة", "Focused Review", lang), Strings.pk("تقسيم ساعات فصل على عدد الأيام", "Split chapter review hours over days", lang)),
            Triple(ActiveSheet.ReviewEditor("misc", null), Strings.pk("مواضيع متفرقة", "Misc Topics", lang), Strings.pk("إنشاء، قواعد، أو مواضيع جانبية", "Essays, grammar, or standalone topics", lang)),
            Triple(ActiveSheet.ReviewEditor("hw", null), Strings.pk("واجب منزلي", "Homework", lang), Strings.pk("تقسيم وقت الواجب على أيام التسليم", "Divide homework time until due day", lang)),
        ).forEach { (dest, title, sub) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = mz.field,
                border = BorderStroke(1.dp, mz.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPick(dest) },
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = mz.ink)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = mz.muted)
                }
            }
        }
    }
}

@Composable
fun SubjectEditorSheetContent(
    state: AppState,
    subjectId: String?,
    onSave: (String?, String, Double, String, List<SubjectPart>) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val existing = remember(subjectId, state.subjects) { state.subjects.firstOrNull { it.id == subjectId } }

    var name by remember(subjectId) { mutableStateOf(existing?.name ?: "") }
    var target by remember(subjectId) { mutableStateOf((existing?.target ?: 3.0).toString()) }
    var colorHex by remember(subjectId) { mutableStateOf(existing?.color ?: colorToHex(SubjectColors[state.subjects.size % SubjectColors.size])) }
    val parts = remember(subjectId) {
        mutableStateListOf<SubjectPart>().apply {
            if (existing != null) addAll(existing.parts)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (existing != null) Strings.pk("تعديل المادة", "Edit subject", lang) else Strings.pk("مادة جديدة", "New subject", lang),
            style = MaterialTheme.typography.titleLarge,
            color = mz.ink,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(Strings.pk("اسم المادة", "Subject name", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("subject_name_input"),
        )

        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            label = { Text(Strings.pk("الهدف الأسبوعي (ساعات)", "Weekly target (hours)", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        // Color swatches
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Strings.pk("اللون", "Color", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectColors.forEach { c ->
                    val hex = colorToHex(c)
                    val isSel = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (isSel) Modifier.border(3.dp, mz.ink, CircleShape) else Modifier)
                            .clickable { colorHex = hex },
                    )
                }
            }
        }

        // Chapters / Parts
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(Strings.pk("الفصول والمحاضرات", "Chapters & lectures", lang), style = MaterialTheme.typography.labelMedium, color = mz.muted)
                TextButton(onClick = {
                    parts.add(SubjectPart(DateUtils.uid(), "${Strings.pk("الفصل", "Ch", lang)} ${parts.size + 1}", 2.0, 5, false))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(16.dp))
                    Text(Strings.pk("إضافة فصل", "Add chapter", lang), color = mz.accentText)
                }
            }

            parts.forEachIndexed { idx, part ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mz.field,
                    border = BorderStroke(1.dp, mz.line),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = part.name,
                            onValueChange = { v -> parts[idx] = part.copy(name = v) },
                            placeholder = { Text(Strings.pk("الاسم", "Name", lang)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.4f),
                        )
                        OutlinedTextField(
                            value = if (part.hours == 0.0) "" else part.hours.toString(),
                            onValueChange = { v -> parts[idx] = part.copy(hours = v.toDoubleOrNull() ?: 0.0) },
                            placeholder = { Text(Strings.pk("س", "h", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.7f),
                        )
                        IconButton(onClick = { parts.removeAt(idx) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = Strings.pk("حذف", "Delete", lang), tint = mz.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        MzButton(
            text = Strings.pk("حفظ المادة", "Save subject", lang),
            enabled = name.trim().isNotEmpty(),
            onClick = {
                onSave(
                    subjectId,
                    name,
                    target.toDoubleOrNull() ?: 3.0,
                    colorHex,
                    parts.filter { it.name.trim().isNotEmpty() },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "subject_save_button",
        )
    }
}

@Composable
fun ReviewEditorSheetContent(
    state: AppState,
    kind: String,
    reviewId: String?,
    onSave: (String?, String, String, Double, Int) -> Unit,
    onDelete: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val meta = Strings.reviewMeta(kind, lang)
    val existing = remember(reviewId, state.reviews) { state.reviews.firstOrNull { it.id == reviewId } }

    var name by remember(reviewId) { mutableStateOf(existing?.name ?: "") }
    var hours by remember(reviewId) { mutableStateOf((existing?.hours ?: 4.0).toString()) }
    var days by remember(reviewId) { mutableStateOf((existing?.days ?: 3).toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(meta.title, style = MaterialTheme.typography.titleLarge, color = mz.ink)
        Text(meta.hint, style = MaterialTheme.typography.bodySmall, color = mz.muted)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(meta.nameLabel) },
            placeholder = { Text(meta.placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text(meta.hoursLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors(),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                label = { Text(meta.daysLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors(),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (existing != null) {
                MzGhostButton(
                    text = Strings.pk("حذف", "Delete", lang),
                    isDanger = true,
                    onClick = { onDelete(existing.id) },
                )
            }
            MzButton(
                text = Strings.pk("حفظ", "Save", lang),
                enabled = name.trim().isNotEmpty() && (hours.toDoubleOrNull() ?: 0.0) > 0.0 && (days.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onSave(reviewId, kind, name, hours.toDoubleOrNull() ?: 4.0, days.toIntOrNull() ?: 3)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun TopicsEditorSheetContent(
    kind: String,
    lang: String,
    onAddTopics: (String, List<String>) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val meta = Strings.reviewMeta(kind, lang)
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(meta.title, style = MaterialTheme.typography.titleLarge, color = mz.ink)
        Text(
            text = Strings.pk("اكتب كل عنصر بسطر مستقل لإضافتها كقائمة مهام.", "Write one item per line to add them as checklist items.", lang),
            style = MaterialTheme.typography.bodySmall,
            color = mz.muted,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        MzButton(
            text = Strings.pk("إضافة للقائمة", "Add to list", lang),
            enabled = text.trim().isNotEmpty(),
            onClick = {
                val lines = text.split('\n', '،').map { it.trim() }.filter { it.isNotEmpty() }
                onAddTopics(kind, lines)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PlanEditorSheetContent(
    state: AppState,
    onSave: (String, Int) -> Unit,
    onClear: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    var name by remember { mutableStateOf(state.plan?.name ?: "") }
    var days by remember { mutableStateOf((state.plan?.days ?: 30).toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(Strings.pk("خطة العد التنازلي", "Countdown Plan", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(Strings.pk("اسم الخطة (مثال: امتحانات نصف السنة)", "Plan name (e.g. Finals)", lang)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = days,
            onValueChange = { days = it },
            label = { Text(Strings.pk("عدد الأيام المتبقية", "Days remaining", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.plan != null) {
                MzGhostButton(text = Strings.pk("إلغاء الخطة", "Clear plan", lang), isDanger = true, onClick = onClear)
            }
            MzButton(
                text = Strings.pk("حفظ الخطة", "Save plan", lang),
                onClick = { onSave(name, days.toIntOrNull() ?: 30) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ManualLogSheetContent(
    state: AppState,
    defaultSubjId: String,
    onSave: (Int, String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    var minsText by remember { mutableStateOf("25") }
    var subjId by remember { mutableStateOf(defaultSubjId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(Strings.pk("تسجيل وقت دراسة يدوي", "Log manual study time", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 25, 45, 60, 90).forEach { m ->
                ChoiceChip(
                    label = DateUtils.applyDigits("${m}د", lang, state.arDigits),
                    selected = minsText == m.toString(),
                    onClick = { minsText = m.toString() },
                )
            }
        }

        OutlinedTextField(
            value = minsText,
            onValueChange = { minsText = it },
            label = { Text(Strings.pk("المدة بالدقائق", "Duration (minutes)", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.subjects.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChoiceChip(label = Strings.pk("عام", "General", lang), selected = subjId.isEmpty(), onClick = { subjId = "" })
                state.subjects.forEach { sb ->
                    ChoiceChip(label = sb.name, selected = subjId == sb.id, accent = parseHexColor(sb.color), onClick = { subjId = sb.id })
                }
            }
        }

        MzButton(
            text = Strings.pk("إضافة للسجل", "Add to log", lang),
            enabled = (minsText.toIntOrNull() ?: 0) > 0,
            onClick = { onSave(minsText.toIntOrNull() ?: 25, subjId) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun TimerSettingsSheetContent(
    state: AppState,
    onSave: (Int, Int, Int) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    var focus by remember { mutableStateOf(state.settings.focus.toString()) }
    var brk by remember { mutableStateOf(state.settings.brk.toString()) }
    var goal by remember { mutableStateOf(state.settings.goal.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(Strings.pk("إعدادات المؤقت والهدف", "Timer & Daily Goal", lang), style = MaterialTheme.typography.titleLarge, color = mz.ink)
        OutlinedTextField(
            value = focus,
            onValueChange = { focus = it },
            label = { Text(Strings.pk("مدة التركيز (دقيقة)", "Focus duration (min)", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = brk,
            onValueChange = { brk = it },
            label = { Text(Strings.pk("مدة الاستراحة (دقيقة)", "Break duration (min)", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text(Strings.pk("الهدف اليومي (دقيقة)", "Daily goal (min)", lang)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        MzButton(
            text = Strings.pk("حفظ الإعدادات", "Save settings", lang),
            onClick = { onSave(focus.toIntOrNull() ?: 25, brk.toIntOrNull() ?: 5, goal.toIntOrNull() ?: 120) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color? = null,
) {
    val mz = LocalMudhakiratiColors.current
    val ac = accent ?: mz.accent
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ac.copy(alpha = 0.16f) else mz.field,
        border = BorderStroke(1.dp, if (selected) ac else mz.line),
        modifier = Modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) mz.ink else mz.muted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}
