package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExportUtils
import com.example.model.AppState
import com.example.model.ProgressRange
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    state: AppState,
    monthOffset: Int,
    selectedDayKey: String,
    onSetRange: (ProgressRange) -> Unit,
    onMoveMonthOffset: (Int) -> Unit,
    onSelectDay: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val ctx = LocalContext.current
    val lang = state.lang
    val ar = state.arDigits
    val td = DateUtils.todayKey()
    val ms = remember(state.logs, state.settings.goal, monthOffset, lang, ar) {
        DateUtils.monthStats(state, monthOffset)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MzSegmentedTabs(
                items = listOf(
                    ProgressRange.WEEK to Strings.pk("أسبوع", "Week", lang),
                    ProgressRange.MONTH to Strings.pk("شهر", "Month", lang),
                ),
                selected = state.progRange,
                onSelect = onSetRange,
                testTagPrefix = "progress_range",
            )
        }

        if (state.progRange == ProgressRange.WEEK) {
            val wkDays = (-6..0).map { offset ->
                val k = DateUtils.addDaysKey(offset)
                Triple(k, DateUtils.formatWeekdayShort(k, lang, ar), DateUtils.minsOn(state, k))
            }
            val wkTotal = wkDays.sumOf { it.third }
            val streak = DateUtils.computeStreak(state)
            val dailyAvg = (wkTotal / 7.0).roundToInt()
            val doneTasks = state.tasks.count { it.done }
            val taskPct = if (state.tasks.isEmpty()) 0 else ((doneTasks.toDouble() / state.tasks.size) * 100.0).roundToInt()

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            title = Strings.pk("مجموع الأسبوع", "Week total", lang),
                            value = Strings.fmtMin(wkTotal, lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                        KpiTile(
                            title = Strings.pk("الستريك الحالي", "Current streak", lang),
                            value = "${DateUtils.applyDigits("$streak", lang, ar)} ${Strings.daysWord(streak, lang)}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            title = Strings.pk("المعدل اليومي", "Daily average", lang),
                            value = Strings.fmtMin(dailyAvg, lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                        KpiTile(
                            title = Strings.pk("إنجاز المهام", "Task completion", lang),
                            value = DateUtils.applyDigits("$taskPct%", lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 7-day bar chart
            item {
                val maxVal = max(state.settings.goal, wkDays.maxOfOrNull { it.third } ?: 60).coerceAtLeast(30)
                MzCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = Strings.pk("ساعات الدراسة آخر 7 أيام", "Study activity (last 7 days)", lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = mz.ink,
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                        ) {
                            val n = wkDays.size
                            val slotW = size.width / n
                            val barW = slotW * 0.48f
                            val goalY = size.height * (1f - (state.settings.goal.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                            drawLine(
                                color = mz.muted.copy(alpha = 0.45f),
                                start = Offset(0f, goalY),
                                end = Offset(size.width, goalY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                            )
                            wkDays.forEachIndexed { idx, (key, _, mins) ->
                                val barH = ((mins.toFloat() / maxVal.toFloat()) * (size.height - 12f)).coerceAtLeast(if (mins > 0) 8f else 3f)
                                val x = slotW * idx + (slotW - barW) / 2f
                                val col = when {
                                    key == td -> mz.accent
                                    mins >= state.settings.goal -> mz.ok
                                    mins > 0 -> mz.accent.copy(alpha = 0.65f)
                                    else -> mz.track
                                }
                                drawRoundRect(
                                    color = col,
                                    topLeft = Offset(x, size.height - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(10f, 10f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            wkDays.forEach { (key, dayLbl, mins) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = if (mins > 0) DateUtils.applyDigits("$mins", lang, ar) else "",
                                        style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                        color = if (key == td) mz.accentText else mz.muted,
                                    )
                                    Text(
                                        text = dayLbl,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (key == td) FontWeight.Bold else FontWeight.Medium,
                                        color = if (key == td) mz.accentText else mz.muted,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subject Distribution (Week)
            item {
                val weekBySubj = remember(state.logs, td) {
                    val allowed = (0..6).map { DateUtils.addDaysKey(-it) }.toSet()
                    val m = mutableMapOf<String, Int>()
                    state.logs.filter { allowed.contains(it.date) }.forEach { l ->
                        m[l.subj] = (m[l.subj] ?: 0) + l.min
                    }
                    m
                }
                SubjectBreakdownCard(
                    state = state,
                    bySubj = weekBySubj,
                    lang = lang,
                    ar = ar,
                )
            }
        } else {
            // MONTH VIEW
            val selectedEntry = ms.days.firstOrNull { it.key == selectedDayKey } ?: ms.days.lastOrNull { it.isToday } ?: ms.days.firstOrNull()
            val avgElapsed = if (ms.elapsedDays > 0) (ms.totalMin.toDouble() / ms.elapsedDays).roundToInt() else 0

            item {
                MzCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { onMoveMonthOffset(-1) }, enabled = ms.canGoPrev) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = Strings.pk("الشهر السابق", "Previous month", lang), tint = if (ms.canGoPrev) mz.ink else mz.muted)
                        }
                        Text(ms.label, style = MaterialTheme.typography.titleMedium, color = mz.ink)
                        IconButton(onClick = { onMoveMonthOffset(1) }, enabled = monthOffset < 0) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = Strings.pk("الشهر التالي", "Next month", lang), tint = if (monthOffset < 0) mz.ink else mz.muted)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            title = Strings.pk("مجموع الشهر", "Month total", lang),
                            value = Strings.fmtMin(ms.totalMin, lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                        KpiTile(
                            title = Strings.pk("المعدل اليومي", "Daily avg", lang),
                            value = Strings.fmtMin(avgElapsed, lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            title = Strings.pk("أيام نشطة", "Active days", lang),
                            value = DateUtils.applyDigits("${ms.activeDays}/${ms.elapsedDays}", lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                        KpiTile(
                            title = Strings.pk("أيام تحقق الهدف", "Goal days", lang),
                            value = DateUtils.applyDigits("${ms.goalDays}", lang, ar),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Interactive Monthly Canvas Bar Chart with tap & scrub
            item {
                val maxV = max(ms.goalMin, ms.maxMin).coerceAtLeast(30)
                MzCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = Strings.pk("تفصيل الشهر (اسحب للمعاينة)", "Monthly chart (tap or scrub)", lang),
                                style = MaterialTheme.typography.titleSmall,
                                color = mz.ink,
                            )
                            if (selectedEntry != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = mz.soft,
                                ) {
                                    Text(
                                        text = "${DateUtils.formatDateShort(selectedEntry.key, lang, ar)} · ${Strings.fmtMin(selectedEntry.minutes, lang, ar)}",
                                        style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                        color = mz.accentText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .pointerInput(ms.days) {
                                    detectTapGestures { offset ->
                                        if (ms.days.isNotEmpty() && size.width > 0) {
                                            val idx = ((offset.x / size.width) * ms.days.size).toInt().coerceIn(0, ms.days.lastIndex)
                                            onSelectDay(ms.days[idx].key)
                                        }
                                    }
                                }
                                .pointerInput(ms.days) {
                                    detectHorizontalDragGestures { change, _ ->
                                        if (ms.days.isNotEmpty() && size.width > 0) {
                                            val idx = ((change.position.x / size.width) * ms.days.size).toInt().coerceIn(0, ms.days.lastIndex)
                                            onSelectDay(ms.days[idx].key)
                                        }
                                    }
                                },
                        ) {
                            val n = ms.days.size.coerceAtLeast(1)
                            val slotW = size.width / n
                            val barW = (slotW * 0.68f).coerceAtLeast(2f)
                            val goalY = size.height * (1f - (ms.goalMin.toFloat() / maxV.toFloat()).coerceIn(0f, 1f))
                            drawLine(
                                color = mz.muted.copy(alpha = 0.45f),
                                start = Offset(0f, goalY),
                                end = Offset(size.width, goalY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                            )
                            ms.days.forEachIndexed { idx, entry ->
                                val h = ((entry.minutes.toFloat() / maxV.toFloat()) * (size.height - 8f)).coerceAtLeast(if (entry.minutes > 0) 6f else 2f)
                                val x = idx * slotW + (slotW - barW) / 2f
                                val isSel = selectedEntry?.key == entry.key
                                val col = when {
                                    isSel -> mz.ink
                                    entry.isToday -> mz.accent
                                    ms.goalMin > 0 && entry.minutes >= ms.goalMin -> mz.ok
                                    entry.minutes > 0 -> mz.accent.copy(alpha = 0.6f)
                                    else -> mz.track
                                }
                                drawRoundRect(
                                    color = col,
                                    topLeft = Offset(x, size.height - h),
                                    size = Size(barW, h),
                                    cornerRadius = CornerRadius(4f, 4f),
                                )
                            }
                        }
                    }
                }
            }

            // Month Details card
            item {
                val activeAvg = if (ms.activeDays > 0) (ms.totalMin.toDouble() / ms.activeDays).roundToInt() else 0
                MzCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Strings.pk("إحصائيات الشهر", "Month highlights", lang), style = MaterialTheme.typography.titleMedium, color = mz.ink)
                        HighlightRow(
                            label = Strings.pk("أفضل يوم", "Best day", lang),
                            value = ms.bestDay?.let { "${DateUtils.formatDateShort(it.key, lang, ar)} (${Strings.fmtMin(it.minutes, lang, ar)})" } ?: "—",
                        )
                        HighlightRow(
                            label = Strings.pk("أطول سلسلة متصلة", "Longest streak", lang),
                            value = "${DateUtils.applyDigits("${ms.longestStreak}", lang, ar)} ${Strings.daysWord(ms.longestStreak, lang)}",
                        )
                        HighlightRow(
                            label = Strings.pk("المعدل بأيام الدراسة", "Avg per study day", lang),
                            value = Strings.fmtMin(activeAvg, lang, ar),
                        )
                        HighlightRow(
                            label = Strings.pk("عدد الجلسات", "Sessions", lang),
                            value = DateUtils.applyDigits("${ms.sessions}", lang, ar),
                        )
                    }
                }
            }

            item {
                SubjectBreakdownCard(
                    state = state,
                    bySubj = ms.bySubj,
                    lang = lang,
                    ar = ar,
                )
            }
        }

        item {
            MzGhostButton(
                text = Strings.pk("طباعة التقرير / PDF", "Print Report / PDF", lang),
                onClick = { ExportUtils.printStudyReport(ctx, state) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "progress_print_pdf",
            )
        }
    }
}

@Composable
private fun KpiTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val mz = LocalMudhakiratiColors.current
    MzCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = mz.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = rubikNumStyle(MaterialTheme.typography.titleLarge),
            color = mz.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    val mz = LocalMudhakiratiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = mz.muted)
        Text(value, style = rubikNumStyle(MaterialTheme.typography.bodyMedium).copy(fontWeight = FontWeight.SemiBold), color = mz.ink)
    }
}

@Composable
private fun SubjectBreakdownCard(
    state: AppState,
    bySubj: Map<String, Int>,
    lang: String,
    ar: Boolean,
) {
    val mz = LocalMudhakiratiColors.current
    val total = bySubj.values.sum()
    val subMap = state.subjects.associateBy { it.id }

    MzCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = Strings.pk("توزيع الساعات حسب المادة", "Time distribution by subject", lang),
                style = MaterialTheme.typography.titleMedium,
                color = mz.ink,
            )
            if (total <= 0) {
                Text(
                    text = Strings.pk("لا توجد جلسات مسجّلة في هذه الفترة بعد.", "No sessions logged in this period yet.", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = mz.muted,
                )
            } else {
                val sortedEntries = bySubj.entries.filter { it.value > 0 }.sortedByDescending { it.value }
                // Stacked proportional bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(mz.track),
                ) {
                    sortedEntries.forEach { (sid, mins) ->
                        val sb = subMap[sid]
                        val col = if (sb != null) parseHexColor(sb.color) else mz.muted
                        Box(
                            modifier = Modifier
                                .weight(mins.toFloat().coerceAtLeast(1f))
                                .height(10.dp)
                                .background(col),
                        )
                    }
                }

                sortedEntries.forEach { (sid, mins) ->
                    val sb = subMap[sid]
                    val col = if (sb != null) parseHexColor(sb.color) else mz.muted
                    val pct = ((mins.toDouble() / total) * 100.0).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(col))
                        Text(
                            text = sb?.name ?: Strings.pk("عام / مراجعة", "General / Review", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = mz.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${Strings.fmtMin(mins, lang, ar)} (${DateUtils.applyDigits("$pct%", lang, ar)})",
                            style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                            color = mz.muted,
                        )
                    }
                }
            }
        }
    }
}
