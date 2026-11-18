package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfirmDialogState
import com.example.model.StudyTask
import com.example.model.Subject
import com.example.model.ToastMessage
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings

@Composable
fun MzCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = backgroundColor ?: mz.surface,
        border = BorderStroke(1.dp, borderColor ?: mz.line),
        shadowElevation = if (mz.isDark) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun MzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    testTag: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val mz = LocalMudhakiratiColors.current
    val container = if (isDanger) mz.hot else mz.accent
    val contentCol = if (isDanger) Color.White else mz.accentInk
    val shape = RoundedCornerShape(14.dp)

    if (!isDanger && mz.isWhiteYellow && enabled) {
        val brush = Brush.horizontalGradient(listOf(mz.heroStart, mz.heroEnd))
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(shape)
                .background(brush)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (leadingIcon != null) leadingIcon()
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = mz.heroInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = contentCol,
                disabledContainerColor = mz.track,
                disabledContentColor = mz.muted,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (leadingIcon != null) leadingIcon()
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MzGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    testTag: String? = null,
) {
    val mz = LocalMudhakiratiColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDanger) mz.hot.copy(alpha = 0.5f) else mz.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDanger) mz.hot else mz.ink,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun <T> MzSegmentedTabs(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "seg",
) {
    val mz = LocalMudhakiratiColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = mz.field,
        border = BorderStroke(1.dp, mz.line),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { idx, (value, label) ->
                val isSel = value == selected
                val bg by animateColorAsState(
                    targetValue = if (isSel) mz.surface else Color.Transparent,
                    animationSpec = tween(220),
                    label = "seg_bg",
                )
                val fg by animateColorAsState(
                    targetValue = if (isSel) mz.ink else mz.muted,
                    animationSpec = tween(220),
                    label = "seg_fg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .then(if (isSel) Modifier.border(1.dp, mz.line, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable { onSelect(value) }
                        .testTag("${testTagPrefix}_$idx"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Animated completion checkbox with smooth scaling spring animation and subtle color transition.
 */
@Composable
fun AnimatedCheckBox(
    checked: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    accentColor: Color? = null,
    testTag: String? = null,
) {
    val mz = LocalMudhakiratiColors.current
    val fillTarget = accentColor ?: mz.ok
    val boxScale by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "check_box_scale",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "check_icon_scale",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "check_alpha",
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) fillTarget else Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "check_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) fillTarget else mz.muted.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 240),
        label = "check_border",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .semantics { this.contentDescription = contentDescription }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(boxScale)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkAlpha
                    },
            )
        }
    }
}

@Composable
fun TaskRowItem(
    task: StudyTask,
    subject: Subject?,
    lang: String,
    arDigits: Boolean,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    trailingControl: (@Composable () -> Unit)? = null,
) {
    val mz = LocalMudhakiratiColors.current
    val td = DateUtils.todayKey()
    val isOverdue = !task.done && task.due.isNotBlank() && task.due < td
    val titleColor by animateColorAsState(
        targetValue = if (task.done) mz.muted else mz.ink,
        animationSpec = tween(240),
        label = "task_title_color",
    )
    val cardBorder by animateColorAsState(
        targetValue = when {
            task.done -> mz.line.copy(alpha = 0.7f)
            isOverdue -> mz.hot.copy(alpha = 0.45f)
            else -> mz.line
        },
        animationSpec = tween(220),
        label = "task_card_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        color = if (task.done) mz.surface.copy(alpha = 0.72f) else mz.surface,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedCheckBox(
                checked = task.done,
                onToggle = onToggleDone,
                contentDescription = Strings.pk("تحديد المهمة كمكتملة", "Toggle task done", lang),
                testTag = "task_check_${task.id}",
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val hasMeta = subject != null || task.due.isNotBlank() || task.pri != "med" || task.rem.isNotBlank()
                if (hasMeta) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (subject != null) {
                            val subCol = parseHexColor(subject.color)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(subCol),
                                )
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mz.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (task.due.isNotBlank()) {
                            Text(
                                text = Strings.dueLabel(task.due, lang, arDigits),
                                style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                color = if (isOverdue) mz.hot else mz.muted,
                            )
                        }
                        if (task.rem.isNotBlank() && !task.done) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = mz.accentText,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = DateUtils.formatTime12(task.rem, lang, arDigits),
                                    style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                    color = mz.accentText,
                                )
                            }
                        }
                        if (task.pri == "high" && !task.done) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = mz.hot.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = Strings.priorityLabel(task.pri, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mz.hot,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        } else if (task.pri == "low" && !task.done) {
                            Text(
                                text = Strings.priorityLabel(task.pri, lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = mz.muted,
                            )
                        }
                    }
                }
            }

            if (trailingControl != null) {
                trailingControl()
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("task_delete_${task.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = Strings.pk("حذف المهمة", "Delete task", lang),
                        tint = mz.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    photoDataUrl: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val mz = LocalMudhakiratiColors.current
    val bitmap = remember(photoDataUrl) {
        if (photoDataUrl.startsWith("data:image")) {
            val comma = photoDataUrl.indexOf(',')
            if (comma > 0) {
                try {
                    val bytes = Base64.decode(photoDataUrl.substring(comma + 1), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(mz.soft)
            .border(1.5.dp, mz.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = name.ifBlank { "Avatar" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "م"
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = mz.accentText,
                fontSize = (size.value * 0.44f).sp,
            )
        }
    }
}

@Composable
fun SubjectGlyph(
    subjectName: String,
    tint: Color,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    val icon = when (Strings.subjectKeyIcon(subjectName)) {
        "dome" -> Icons.Rounded.AutoStories
        "flask" -> Icons.Rounded.Science
        "atom" -> Icons.Rounded.Bolt
        "dna" -> Icons.Rounded.Biotech
        "calc" -> Icons.Rounded.Calculate
        "globe" -> Icons.Rounded.Public
        else -> Icons.Rounded.AutoStories
    }
    Icon(
        imageVector = icon,
        contentDescription = subjectName,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/**
 * 6-stage evolving flame badge representing the user's daily study streak.
 */
@Composable
fun AnimatedFlameIcon(
    streak: Int,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val stage = Strings.flameStage(streak)
    val isLit = streak > 0
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outerColor = when {
            !isLit -> Color(0xFFB5AE9B)
            stage <= 1 -> Color(0xFFF59E0B)
            stage <= 3 -> Color(0xFFEA580C)
            else -> Color(0xFFDC2626)
        }
        val innerColor = when {
            !isLit -> Color(0xFFD6D0C0)
            stage <= 1 -> Color(0xFFFDE68A)
            else -> Color(0xFFFACC15)
        }

        val scaleFactor = 0.84f + stage * 0.032f
        val topY = h * (1f - scaleFactor) * 0.6f
        val outerPath = Path().apply {
            moveTo(w * 0.50f, topY + h * 0.04f)
            cubicTo(w * 0.78f, h * 0.30f, w * 0.88f, h * 0.56f, w * 0.74f, h * 0.80f)
            cubicTo(w * 0.64f, h * 0.94f, w * 0.36f, h * 0.94f, w * 0.26f, h * 0.80f)
            cubicTo(w * 0.12f, h * 0.56f, w * 0.22f, h * 0.30f, w * 0.50f, topY + h * 0.04f)
            close()
        }
        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFB703), outerColor)),
        )

        val innerPath = Path().apply {
            moveTo(w * 0.50f, h * 0.38f)
            cubicTo(w * 0.65f, h * 0.52f, w * 0.68f, h * 0.68f, w * 0.60f, h * 0.80f)
            cubicTo(w * 0.55f, h * 0.88f, w * 0.45f, h * 0.88f, w * 0.40f, h * 0.80f)
            cubicTo(w * 0.32f, h * 0.68f, w * 0.35f, h * 0.52f, w * 0.50f, h * 0.38f)
            close()
        }
        drawPath(path = innerPath, color = innerColor)

        if (isLit && stage >= 3) {
            drawCircle(
                color = Color(0xFFFDE047),
                radius = w * 0.055f,
                center = Offset(w * 0.22f, h * 0.25f),
            )
            drawCircle(
                color = Color(0xFFFB923C),
                radius = w * 0.045f,
                center = Offset(w * 0.80f, h * 0.22f),
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val mz = LocalMudhakiratiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = mz.ink,
        )
        if (trailing != null) {
            trailing()
        } else if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = mz.accentText,
                )
            }
        }
    }
}

@Composable
fun UndoToastBanner(
    toast: ToastMessage?,
    lang: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mz = LocalMudhakiratiColors.current
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        if (toast != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (mz.isDark) mz.surface else Color(0xFF1F1D17),
                border = BorderStroke(1.dp, if (mz.isDark) mz.line else Color(0xFF343026)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = toast.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mz.isDark) mz.ink else Color(0xFFFDFBF5),
                        modifier = Modifier.weight(1f),
                    )
                    if (toast.canUndo) {
                        TextButton(
                            onClick = onUndo,
                            modifier = Modifier.testTag("toast_undo_button"),
                        ) {
                            Text(
                                text = Strings.pk("تراجع", "Undo", lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = mz.accent,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = Strings.pk("إغلاق", "Close", lang),
                            tint = Color(0xFFA8A295),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MzConfirmDialog(
    state: ConfirmDialogState?,
    onDismiss: () -> Unit,
) {
    if (state == null) return
    val mz = LocalMudhakiratiColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = mz.surface,
        titleContentColor = mz.ink,
        textContentColor = mz.muted,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = state.title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = {
                    val action = state.onConfirm
                    onDismiss()
                    action()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isDanger) mz.hot else mz.accent,
                    contentColor = if (state.isDanger) Color.White else mz.accentInk,
                ),
                modifier = Modifier.testTag("confirm_dialog_ok"),
            ) {
                Text(state.confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_dialog_cancel"),
            ) {
                Text(state.cancelLabel, style = MaterialTheme.typography.labelMedium, color = mz.muted)
            }
        },
    )
}

@Composable
fun CircularRingProgress(
    fraction: Float,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp = 7.dp,
    modifier: Modifier = Modifier,
) {
    val animFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(550),
        label = "ring_progress",
    )
    Canvas(modifier = modifier) {
        val sw = strokeWidth.toPx()
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
        if (animFraction > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animFraction,
                useCenter = false,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
    }
}
