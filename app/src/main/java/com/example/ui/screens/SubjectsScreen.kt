package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.Subject
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.SubjectGlyph
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlin.math.roundToInt

@Composable
fun SubjectsScreen(
    state: AppState,
    confirmDeleteSubjectId: String,
    onOpenSheet: (ActiveSheet) -> Unit,
    onTogglePartDone: (String, String) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onAddDefault6thSubjects: () -> Unit,
    onToggleReviewDaily: (String) -> Unit,
    onCleanReviews: (String) -> Unit,
    onToggleCheckItem: (String) -> Unit,
    onDeleteCheckItem: (String) -> Unit,
    onCleanCheckItems: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val missing6th = Strings.missingDefaultSubjects(state.subjects, lang)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoCol = maxWidth >= 660.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (missing6th.isNotEmpty()) {
                item {
                    MzCard(
                        backgroundColor = mz.soft,
                        borderColor = mz.accent.copy(alpha = 0.35f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = Strings.pk("مواد السادس الإعدادي", "6th Grade Subjects", lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = mz.ink,
                                )
                                Text(
                                    text = Strings.pk(
                                        "أضف المواد الأساسية السبعة بضغطة واحدة",
                                        "Add the 7 core subjects in one tap",
                                        lang,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mz.muted,
                                )
                            }
                            MzGhostButton(
                                text = Strings.pk("إضافة مواد السادس", "Add 6th grade", lang),
                                onClick = onAddDefault6thSubjects,
                                testTag = "subjects_add_6th",
                            )
                        }
                    }
                }
            }

            if (state.subjects.isEmpty()) {
                item {
                    MzCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = mz.accentText,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                text = Strings.pk("لم تضف أي مادة بعد", "No subjects added yet", lang),
                                style = MaterialTheme.typography.titleMedium,
                                color = mz.ink,
                            )
                            Text(
                                text = Strings.pk(
                                    "ابدأ بإضافة مادة أو مراجعة مركّزة من الزر + بالأسفل.",
                                    "Start adding a subject or focused review with the + button.",
                                    lang,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = mz.muted,
                            )
                        }
                    }
                }
            } else {
                val rows = if (isTwoCol) state.subjects.chunked(2) else state.subjects.map { listOf(it) }
                items(rows.size, key = { idx -> rows[idx].joinToString("-") { it.id } }) { rowIdx ->
                    val pair = rows[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { sb ->
                            Box(modifier = Modifier.weight(1f)) {
                                SubjectDetailCard(
                                    subject = sb,
                                    weekMinutes = DateUtils.weekMins(state, sb.id),
                                    confirmingDelete = confirmDeleteSubjectId == sb.id,
                                    lang = lang,
                                    arDigits = ar,
                                    onEdit = { onOpenSheet(ActiveSheet.SubjectEditor(sb.id)) },
                                    onDeleteTap = { onDeleteSubject(sb.id) },
                                    onTogglePart = { partId -> onTogglePartDone(sb.id, partId) },
                                )
                            }
                        }
                        if (isTwoCol && pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Focused Reviews / Misc / HW sections matching Subjects tab tools
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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectDetailCard(
    subject: Subject,
    weekMinutes: Int,
    confirmingDelete: Boolean,
    lang: String,
    arDigits: Boolean,
    onEdit: () -> Unit,
    onDeleteTap: () -> Unit,
    onTogglePart: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val subColor = parseHexColor(subject.color)
    val targetMin = (subject.target * 60.0).roundToInt().coerceAtLeast(30)
    val pct = ((weekMinutes.toDouble() / targetMin) * 100.0).roundToInt().coerceIn(0, 100)
    val doneParts = subject.parts.count { it.done }

    MzCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(subColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SubjectGlyph(subjectName = subject.name, tint = subColor, size = 21.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = mz.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${Strings.fmtMin(weekMinutes, lang, arDigits)} / ${
                            Strings.fmtHours(subject.target, lang, arDigits)
                        } (${DateUtils.applyDigits("$pct%", lang, arDigits)})",
                        style = rubikNumStyle(MaterialTheme.typography.bodySmall),
                        color = mz.muted,
                    )
                }

                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("subject_edit_${subject.id}"),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(16.dp))
                    Text(
                        text = Strings.pk(" تعديل", " Edit", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = mz.accentText,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (weekMinutes.toFloat() / targetMin.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = subColor,
                trackColor = mz.track,
                strokeCap = StrokeCap.Round,
            )

            if (subject.parts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${Strings.pk("الفصول", "Chapters", lang)} (${DateUtils.applyDigits("$doneParts/${subject.parts.size}", lang, arDigits)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = mz.muted,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        subject.parts.forEach { part ->
                            val chipBg by animateColorAsState(
                                targetValue = if (part.done) mz.ok.copy(alpha = 0.14f) else mz.field,
                                animationSpec = tween(200),
                                label = "part_bg",
                            )
                            val chipBorder by animateColorAsState(
                                targetValue = if (part.done) mz.ok.copy(alpha = 0.45f) else mz.line,
                                animationSpec = tween(200),
                                label = "part_border",
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = chipBg,
                                border = BorderStroke(1.dp, chipBorder),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTogglePart(part.id) }
                                    .testTag("subject_part_${subject.id}_${part.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    if (part.done) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = mz.ok,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        text = part.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (part.done) mz.ok else mz.ink,
                                        textDecoration = if (part.done) TextDecoration.LineThrough else TextDecoration.None,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (confirmingDelete) mz.hot else Color.Transparent,
                    border = BorderStroke(1.dp, if (confirmingDelete) mz.hot else mz.line),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDeleteTap)
                        .testTag("subject_delete_${subject.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = if (confirmingDelete) Color.White else mz.muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (confirmingDelete) Strings.pk("تأكيد الحذف", "Tap again to delete", lang)
                            else Strings.pk("حذف", "Delete", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (confirmingDelete) Color.White else mz.muted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
