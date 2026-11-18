package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.AppTab
import com.example.model.CheckListItem
import com.example.model.ReviewItem
import com.example.model.Subject
import com.example.ui.components.AnimatedCheckBox
import com.example.ui.components.CircularRingProgress
import com.example.ui.components.MzButton
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubjectGlyph
import com.example.ui.components.TaskRowItem
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AppState,
    onClearExamples: () -> Unit,
    onStartTimer: () -> Unit,
    onFocusSubject: (String) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleSubjectDaily: (String) -> Unit,
    onToggleReviewDaily: (String) -> Unit,
    onCleanReviews: (String) -> Unit,
    onToggleCheckItem: (String) -> Unit,
    onDeleteCheckItem: (String) -> Unit,
    onCleanCheckItems: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val td = DateUtils.todayKey()
    val todayMin = DateUtils.minsOn(state, td)
    val goalMin = state.settings.goal.coerceAtLeast(10)
    val goalPct = min(100, ((todayMin.toDouble() / goalMin) * 100.0).roundToInt())
    val hasExamples = state.subjects.any { it.ex } || state.tasks.any { it.ex } || state.logs.any { it.ex }

    val planInfo = DateUtils.planInfo(state)
    val subjectsWithParts = state.subjects.filter { DateUtils.partsTotalHours(it).first > 0.0 }
    val needAttentionTasks = state.tasks.filter { !it.done }.sortedWith(
        compareBy<com.example.model.StudyTask> {
            when {
                it.due.isNotBlank() && it.due < td -> 0
                it.due == td -> 1
                it.pri == "high" -> 2
                it.due.isNotBlank() -> 3
                else -> 4
            }
        }.thenBy { it.due.ifEmpty { "9999-99-99" } },
    ).take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (hasExamples) {
            item {
                MzCard(
                    backgroundColor = mz.soft,
                    borderColor = mz.accent.copy(alpha = 0.35f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Strings.pk("هذه بيانات تجريبية، يمكنك مسحها متى شئت.", "This is example data, clear it anytime.", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = mz.ink,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = onClearExamples,
                            modifier = Modifier.testTag("home_clear_examples"),
                        ) {
                            Text(Strings.pk("مسح الأمثلة", "Clear", lang), color = mz.accentText)
                        }
                    }
                }
            }
        }

        // Hero Daily Goal Card
        item {
            val heroBrush = Brush.linearGradient(listOf(mz.heroStart, mz.heroEnd))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(heroBrush)
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = Strings.pk("هدف اليوم", "Today’s Goal", lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = mz.heroInk.copy(alpha = 0.78f),
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = DateUtils.applyDigits("$todayMin", lang, ar),
                                style = rubikNumStyle(MaterialTheme.typography.displayMedium),
                                color = mz.heroInk,
                            )
                            Text(
                                text = "/ ${DateUtils.applyDigits("$goalMin", lang, ar)} ${Strings.pk("د", "min", lang)}",
                                style = rubikNumStyle(MaterialTheme.typography.bodyMedium),
                                color = mz.heroInk.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (mz.heroInk == Color.White) Color.White.copy(alpha = 0.18f) else Color(0xFF1B1A14),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onStartTimer)
                                .testTag("home_start_session"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (mz.heroInk == Color.White) Color.White else Color(0xFFFFFDF7),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = Strings.pk("ابدأ جلسة", "Start session", lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (mz.heroInk == Color.White) Color.White else Color(0xFFFFFDF7),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularRingProgress(
                            fraction = todayMin.toFloat() / goalMin.toFloat(),
                            color = mz.heroInk,
                            trackColor = mz.heroInk.copy(alpha = 0.18f),
                            strokeWidth = 8.dp,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Text(
                            text = DateUtils.applyDigits("$goalPct%", lang, ar),
                            style = rubikNumStyle(MaterialTheme.typography.titleMedium),
                            color = mz.heroInk,
                        )
                    }
                }
            }
        }

        // Countdown Plan Card
        item {
            MzCard {
                if (planInfo == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = Strings.pk("خطة العد التنازلي", "Countdown Plan", lang),
                                style = MaterialTheme.typography.titleMedium,
                                color = mz.ink,
                            )
                            Text(
                                text = Strings.pk(
                                    "حدّد أيامك قبل الامتحان لتقسيم ساعات المحاضرات تلقائيًا.",
                                    "Set days left until your exam to split lecture hours automatically.",
                                    lang,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = mz.muted,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        MzGhostButton(
                            text = Strings.pk("ضبط خطة", "Set plan", lang),
                            onClick = { onOpenSheet(ActiveSheet.PlanEditor) },
                            testTag = "home_set_plan",
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventNote,
                                    contentDescription = null,
                                    tint = mz.accentText,
                                )
                                Text(
                                    text = planInfo.name.ifBlank { Strings.pk("خطة الدراسة", "Study Plan", lang) },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mz.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(
                                onClick = { onOpenSheet(ActiveSheet.PlanEditor) },
                                modifier = Modifier.testTag("home_edit_plan"),
                            ) {
                                Text(Strings.pk("تعديل", "Edit", lang), color = mz.accentText)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (planInfo.done) {
                                    Strings.pk("انتهت مدة الخطة 🎉", "Plan period complete 🎉", lang)
                                } else {
                                    Strings.nfmt(
                                        "${Strings.pk("المتبقي", "Remaining", lang)}: ${planInfo.left} ${Strings.daysWord(planInfo.left, lang)}",
                                        lang,
                                        ar,
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = mz.ink,
                            )
                            Text(
                                text = DateUtils.applyDigits("${planInfo.pct}%", lang, ar),
                                style = rubikNumStyle(MaterialTheme.typography.labelLarge),
                                color = mz.accentText,
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (planInfo.pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = mz.accent,
                            trackColor = mz.track,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        // Daily Lecture Split (when plan is active & subjects have parts)
        if (planInfo != null && subjectsWithParts.isNotEmpty()) {
            item {
                MzCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = Strings.pk("حصة اليوم من المحاضرات", "Today’s Lecture Share", lang),
                            trailing = {
                                val totalShare = DateUtils.todayShareTotal(state)
                                Text(
                                    text = Strings.fmtHours(totalShare, lang, ar),
                                    style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                                    color = mz.accentText,
                                )
                            },
                        )
                        subjectsWithParts.forEach { sb ->
                            val rem = DateUtils.subjectRemainingHours(sb)
                            val doneToday = sb.day != null && sb.day.date == td
                            if (rem > 0.0 || doneToday) {
                                val share = DateUtils.subjectDayShare(sb, planInfo.left.coerceAtLeast(1))
                                val subCol = parseHexColor(sb.color)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(mz.field)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AnimatedCheckBox(
                                        checked = doneToday,
                                        onToggle = { onToggleSubjectDaily(sb.id) },
                                        contentDescription = sb.name,
                                        accentColor = subCol,
                                        testTag = "home_lecture_check_${sb.id}",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subCol),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sb.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (doneToday) mz.muted else mz.ink,
                                        textDecoration = if (doneToday) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = Strings.fmtHours(share, lang, ar),
                                        style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                                        color = if (doneToday) mz.ok else mz.accentText,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Focused Review, Misc, Homework shared cards
        item {
            ReviewSplitCard(
                kind = "rev",
                reviews = state.reviews.filter { it.kind == "rev" },
                lang = lang,
                arDigits = ar,
                onAdd = { onOpenSheet(ActiveSheet.ReviewEditor("rev", null)) },
                onEdit = { id -> onOpenSheet(ActiveSheet.ReviewEditor("rev", id)) },
                onToggleDaily = onToggleReviewDaily,
                onClean = { onCleanReviews("rev") },
            )
        }

        item {
            ReviewAndChecklistSection(
                kind = "misc",
                reviews = state.reviews.filter { it.kind == "misc" },
                checks = state.lists.filter { it.kind == "misc" },
                lang = lang,
                arDigits = ar,
                onAddReview = { onOpenSheet(ActiveSheet.ReviewEditor("misc", null)) },
                onEditReview = { id -> onOpenSheet(ActiveSheet.ReviewEditor("misc", id)) },
                onToggleReview = onToggleReviewDaily,
                onCleanReviews = { onCleanReviews("misc") },
                onAddTopics = { onOpenSheet(ActiveSheet.TopicsEditor("misc")) },
                onToggleCheck = onToggleCheckItem,
                onDeleteCheck = onDeleteCheckItem,
                onCleanChecks = { onCleanCheckItems("misc") },
            )
        }

        item {
            ReviewAndChecklistSection(
                kind = "hw",
                reviews = state.reviews.filter { it.kind == "hw" },
                checks = state.lists.filter { it.kind == "hw" },
                lang = lang,
                arDigits = ar,
                onAddReview = { onOpenSheet(ActiveSheet.ReviewEditor("hw", null)) },
                onEditReview = { id -> onOpenSheet(ActiveSheet.ReviewEditor("hw", id)) },
                onToggleReview = onToggleReviewDaily,
                onCleanReviews = { onCleanReviews("hw") },
                onAddTopics = { onOpenSheet(ActiveSheet.TopicsEditor("hw")) },
                onToggleCheck = onToggleCheckItem,
                onDeleteCheck = onDeleteCheckItem,
                onCleanChecks = { onCleanCheckItems("hw") },
            )
        }

        // Quick-Jump Subjects Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = Strings.pk("موادك الدراسية", "Your Subjects", lang),
                    actionLabel = Strings.pk("الكل", "See all", lang),
                    onAction = { onNavigateTab(AppTab.SUBJECTS) },
                )
                if (state.subjects.isEmpty()) {
                    MzCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = Strings.pk("أضف موادك لتتابع ساعاتك اليومية.", "Add subjects to track your weekly hours.", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mz.muted,
                                modifier = Modifier.weight(1f),
                            )
                            MzGhostButton(
                                text = Strings.pk("إضافة مادة", "Add subject", lang),
                                onClick = { onOpenSheet(ActiveSheet.SubjectTypePicker) },
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.subjects, key = { it.id }) { sb ->
                            QuickSubjectTile(
                                subject = sb,
                                weekMinutes = DateUtils.weekMins(state, sb.id),
                                lang = lang,
                                arDigits = ar,
                                onClick = { onFocusSubject(sb.id) },
                            )
                        }
                    }
                }
            }
        }

        // Needs Attention Tasks
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = Strings.pk("مهام تحتاج انتباهك", "Needs Attention", lang),
                    actionLabel = Strings.pk("كل المهام", "All tasks", lang),
                    onAction = { onNavigateTab(AppTab.TASKS) },
                )
                if (needAttentionTasks.isEmpty()) {
                    MzCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = Strings.pk("قائمتك صافية الآن.", "All caught up for now.", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mz.muted,
                            )
                            MzGhostButton(
                                text = Strings.pk("+ مهمة", "+ Task", lang),
                                onClick = { onOpenSheet(ActiveSheet.TaskEditor()) },
                                testTag = "home_add_task",
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val subMap = state.subjects.associateBy { it.id }
                        needAttentionTasks.forEach { tk ->
                            TaskRowItem(
                                task = tk,
                                subject = subMap[tk.subj],
                                lang = lang,
                                arDigits = ar,
                                onToggleDone = { onToggleTask(tk.id) },
                                onEdit = { onOpenSheet(ActiveSheet.TaskEditor(tk.id)) },
                                onDelete = { onDeleteTask(tk.id) },
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun QuickSubjectTile(
    subject: Subject,
    weekMinutes: Int,
    lang: String,
    arDigits: Boolean,
    onClick: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val subColor = parseHexColor(subject.color)
    val targetMin = (subject.target * 60).roundToInt().coerceAtLeast(30)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = mz.surface,
        border = BorderStroke(1.dp, mz.line),
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("home_quick_subject_${subject.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(subColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SubjectGlyph(subjectName = subject.name, tint = subColor, size = 18.dp)
                }
                Text(
                    text = DateUtils.applyDigits("${(weekMinutes / 60.0).let { "%.1f".format(it) }}h", lang, arDigits),
                    style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                    color = mz.muted,
                )
            }
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleSmall,
                color = mz.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { (weekMinutes.toFloat() / targetMin.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = subColor,
                trackColor = mz.track,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun ReviewSplitCard(
    kind: String,
    reviews: List<ReviewItem>,
    lang: String,
    arDigits: Boolean,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onToggleDaily: (String) -> Unit,
    onClean: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val meta = Strings.reviewMeta(kind, lang)
    val hasFinished = reviews.any { DateUtils.reviewInfo(it).fin }

    MzCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(meta.title, style = MaterialTheme.typography.titleMedium, color = mz.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hasFinished) {
                        IconButton(onClick = onClean) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = Strings.pk("تنظيف المنجز", "Clean completed", lang),
                                tint = mz.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = mz.soft,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAdd)
                            .testTag("add_review_$kind"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(16.dp))
                            Text(Strings.pk("إضافة", "Add", lang), style = MaterialTheme.typography.labelSmall, color = mz.accentText)
                        }
                    }
                }
            }

            if (reviews.isEmpty()) {
                Text(
                    text = meta.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = mz.muted,
                )
            } else {
                reviews.forEach { rv ->
                    val info = DateUtils.reviewInfo(rv)
                    val col = parseHexColor(rv.color)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = mz.field,
                        border = BorderStroke(1.dp, mz.line),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!info.fin) {
                                AnimatedCheckBox(
                                    checked = info.done,
                                    onToggle = { onToggleDaily(rv.id) },
                                    contentDescription = rv.name,
                                    accentColor = col,
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = rv.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (info.done || info.fin) mz.muted else mz.ink,
                                    textDecoration = if (info.fin) TextDecoration.LineThrough else TextDecoration.None,
                                )
                                val subLine = when {
                                    info.fin -> Strings.pk("مكتمل ✓", "Completed ✓", lang)
                                    info.done -> "${Strings.pk("تمت حصة اليوم", "Today’s share done", lang)} (${Strings.fmtHours(info.share, lang, arDigits)})"
                                    else -> "${Strings.pk("حصة اليوم", "Today’s share", lang)}: ${Strings.fmtHours(info.share, lang, arDigits)} · ${
                                        Strings.nfmt("${info.left} ${Strings.daysWord(info.left, lang)}", lang, arDigits)
                                    }"
                                }
                                Text(
                                    text = subLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (info.late) mz.hot else mz.muted,
                                )
                            }
                            IconButton(onClick = { onEdit(rv.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = Strings.pk("تعديل", "Edit", lang),
                                    tint = mz.muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewAndChecklistSection(
    kind: String,
    reviews: List<ReviewItem>,
    checks: List<CheckListItem>,
    lang: String,
    arDigits: Boolean,
    onAddReview: () -> Unit,
    onEditReview: (String) -> Unit,
    onToggleReview: (String) -> Unit,
    onCleanReviews: () -> Unit,
    onAddTopics: () -> Unit,
    onToggleCheck: (String) -> Unit,
    onDeleteCheck: (String) -> Unit,
    onCleanChecks: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val meta = Strings.reviewMeta(kind, lang)
    val hasFinReviews = reviews.any { DateUtils.reviewInfo(it).fin }
    val hasDoneChecks = checks.any { it.done }

    MzCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(meta.title, style = MaterialTheme.typography.titleMedium, color = mz.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasFinReviews || hasDoneChecks) {
                        IconButton(onClick = {
                            onCleanReviews()
                            onCleanChecks()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = Strings.pk("تنظيف المنجز", "Clean completed", lang),
                                tint = mz.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = mz.field,
                        border = BorderStroke(1.dp, mz.line),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddTopics)
                            .testTag("add_checklist_$kind"),
                    ) {
                        Text(
                            text = Strings.pk("+ قائمة", "+ List", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = mz.ink,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = mz.soft,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddReview)
                            .testTag("add_split_$kind"),
                    ) {
                        Text(
                            text = Strings.pk("+ مقسّم", "+ Split", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = mz.accentText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (reviews.isEmpty() && checks.isEmpty()) {
                Text(meta.hint, style = MaterialTheme.typography.bodySmall, color = mz.muted)
            }

            reviews.forEach { rv ->
                val info = DateUtils.reviewInfo(rv)
                val col = parseHexColor(rv.color)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = mz.field,
                    border = BorderStroke(1.dp, mz.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!info.fin) {
                            AnimatedCheckBox(
                                checked = info.done,
                                onToggle = { onToggleReview(rv.id) },
                                contentDescription = rv.name,
                                accentColor = col,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        ) {
                            Text(
                                text = rv.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (info.done || info.fin) mz.muted else mz.ink,
                                textDecoration = if (info.fin) TextDecoration.LineThrough else TextDecoration.None,
                            )
                            Text(
                                text = if (info.fin) Strings.pk("مكتمل ✓", "Completed ✓", lang)
                                else "${Strings.fmtHours(info.share, lang, arDigits)} · ${Strings.nfmt("${info.left} ${Strings.daysWord(info.left, lang)}", lang, arDigits)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (info.late) mz.hot else mz.muted,
                            )
                        }
                        IconButton(onClick = { onEditReview(rv.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = Strings.pk("تعديل", "Edit", lang), tint = mz.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            checks.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mz.field,
                    border = BorderStroke(1.dp, mz.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedCheckBox(
                            checked = item.done,
                            onToggle = { onToggleCheck(item.id) },
                            contentDescription = item.name,
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.done) mz.muted else mz.ink,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDeleteCheck(item.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = Strings.pk("حذف", "Delete", lang),
                                tint = mz.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
