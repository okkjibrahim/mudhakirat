package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.TimerMode
import com.example.model.TimerRuntime
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import kotlin.math.max

@Composable
fun TimerScreen(
    state: AppState,
    timer: TimerRuntime,
    onSetMode: (TimerMode) -> Unit,
    onSelectSubject: (String) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val td = DateUtils.todayKey()
    val todaySessions = state.logs.count { it.date == td }

    val total = max(0, timer.leftSec)
    val hrs = total / 3600
    val mins = (total % 3600) / 60
    val secs = total % 60
    val progress = if (timer.totalSec > 0) (timer.totalSec - timer.leftSec).toFloat() / timer.totalSec.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MzSegmentedTabs(
            items = listOf(
                TimerMode.FOCUS to Strings.pk("تركيز", "Focus", lang),
                TimerMode.BREAK to Strings.pk("استراحة", "Break", lang),
            ),
            selected = timer.mode,
            onSelect = onSetMode,
            testTagPrefix = "timer_mode",
        )

        // Subject selector pills
        if (state.subjects.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(state.subjects, key = { it.id }) { sb ->
                    val selected = sb.id == timer.subjId
                    val subCol = parseHexColor(sb.color)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) subCol.copy(alpha = 0.16f) else mz.surface,
                        border = BorderStroke(1.dp, if (selected) subCol else mz.line),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSelectSubject(sb.id) }
                            .testTag("timer_subj_${sb.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(subCol),
                            )
                            Text(
                                text = sb.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) mz.ink else mz.muted,
                            )
                        }
                    }
                }
            }
        }

        // Flip-Clock Display Card
        MzCard(
            cornerRadius = 26.dp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 26.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hrs > 0) {
                        FlipDigitCard(
                            value = DateUtils.applyDigits(hrs.toString().padStart(2, '0'), lang, ar),
                            label = Strings.pk("ساعة", "HRS", lang),
                        )
                        Text(":", style = rubikNumStyle(MaterialTheme.typography.displayMedium), color = mz.muted)
                    }
                    FlipDigitCard(
                        value = DateUtils.applyDigits(mins.toString().padStart(2, '0'), lang, ar),
                        label = Strings.pk("دقيقة", "MIN", lang),
                    )
                    Text(":", style = rubikNumStyle(MaterialTheme.typography.displayMedium), color = mz.muted)
                    FlipDigitCard(
                        value = DateUtils.applyDigits(secs.toString().padStart(2, '0'), lang, ar),
                        label = Strings.pk("ثانية", "SEC", lang),
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (timer.mode == TimerMode.FOCUS) mz.accent else mz.ok,
                    trackColor = mz.track,
                    strokeCap = StrokeCap.Round,
                )

                // Control buttons: Reset | Play/Pause | Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = mz.field,
                        border = BorderStroke(1.dp, mz.line),
                    ) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(52.dp).testTag("timer_reset_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = Strings.pk("إعادة تعيين", "Reset timer", lang),
                                tint = mz.ink,
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (timer.mode == TimerMode.FOCUS) mz.accent else mz.ok,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleRunning)
                            .testTag("timer_toggle_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (timer.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (timer.running) Strings.pk("إيقاف مؤقت", "Pause timer", lang)
                                else Strings.pk("بدء المؤقت", "Start timer", lang),
                                tint = if (timer.mode == TimerMode.FOCUS) mz.accentInk else Color.White,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = mz.field,
                        border = BorderStroke(1.dp, mz.line),
                    ) {
                        IconButton(
                            onClick = { onOpenSheet(ActiveSheet.TimerSettingsSheet) },
                            modifier = Modifier.size(52.dp).testTag("timer_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = Strings.pk("إعدادات المؤقت", "Timer settings", lang),
                                tint = mz.ink,
                            )
                        }
                    }
                }
            }
        }

        // Session count + Manual Log
        MzCard(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = Strings.pk("جلسات اليوم", "Today’s sessions", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = mz.muted,
                    )
                    Text(
                        text = DateUtils.applyDigits(todaySessions.toString(), lang, ar),
                        style = rubikNumStyle(MaterialTheme.typography.headlineSmall),
                        color = mz.ink,
                    )
                }
                MzGhostButton(
                    text = Strings.pk("سجّل وقتًا يدويًا", "Log time manually", lang),
                    onClick = { onOpenSheet(ActiveSheet.ManualLog) },
                    testTag = "timer_manual_log_button",
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun FlipDigitCard(
    value: String,
    label: String,
) {
    val mz = LocalMudhakiratiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = mz.field,
            border = BorderStroke(1.dp, mz.line),
            modifier = Modifier.size(width = 88.dp, height = 98.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { -it / 4 } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically { it / 4 } + fadeOut(tween(120)))
                    },
                    label = "flip_digit",
                ) { target ->
                    Text(
                        text = target,
                        style = rubikNumStyle(MaterialTheme.typography.displayLarge).copy(fontSize = 44.sp),
                        color = mz.ink,
                    )
                }
                HorizontalDivider(
                    color = mz.line.copy(alpha = 0.65f),
                    thickness = 1.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = mz.muted,
        )
    }
}
