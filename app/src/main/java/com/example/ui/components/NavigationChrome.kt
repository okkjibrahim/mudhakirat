package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppTab
import com.example.model.TimerMode
import com.example.model.TimerRuntime
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings

private data class NavDest(
    val tab: AppTab,
    val ar: String,
    val en: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
)

private val NavDestinations = listOf(
    NavDest(AppTab.HOME, "الرئيسية", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    NavDest(AppTab.TASKS, "المهام", "Tasks", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle),
    NavDest(AppTab.SUBJECTS, "المواد", "Subjects", Icons.Rounded.Book, Icons.Outlined.Book),
    NavDest(AppTab.TIMER, "المؤقت", "Timer", Icons.Rounded.Timer, Icons.Outlined.Timer),
    NavDest(AppTab.HUB, "مساحتي", "My Space", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    NavDest(AppTab.PROGRESS, "التقدم", "Progress", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
)

@Composable
fun HeaderBar(
    userName: String,
    photoDataUrl: String,
    dateText: String,
    clockText: String,
    subtitle: String,
    greetingIdx: Int,
    streak: Int,
    unreadFriendsCount: Int,
    themeMode: String,
    lang: String,
    arDigits: Boolean,
    onOpenProfile: () -> Unit,
    onOpenStreak: () -> Unit,
    onOpenTheme: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(mz.bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenProfile)
                    .testTag("header_profile_button")
                    .semantics {
                        contentDescription = Strings.pk("الملف الشخصي والإعدادات", "Profile and settings", lang)
                    },
                contentAlignment = Alignment.Center,
            ) {
                UserAvatar(
                    name = userName,
                    photoDataUrl = photoDataUrl,
                    size = 42.dp,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = mz.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = mz.muted,
                    )
                    Text(
                        text = clockText,
                        style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                        color = mz.muted,
                        maxLines = 1,
                    )
                }
                Text(
                    text = Strings.greetingText(greetingIdx, userName, lang),
                    style = MaterialTheme.typography.titleLarge,
                    color = mz.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = mz.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Flame streak pill with unread notification dot
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = mz.surface,
                border = BorderStroke(1.dp, mz.line),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenStreak)
                    .testTag("header_streak_button"),
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AnimatedFlameIcon(streak = streak, size = 20.dp)
                        Text(
                            text = DateUtils.applyDigits(streak.toString(), lang, arDigits),
                            style = rubikNumStyle(MaterialTheme.typography.labelLarge),
                            color = mz.ink,
                        )
                    }
                    if (unreadFriendsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(mz.hot),
                        )
                    }
                }
            }

            // Quick Theme Picker Button
            Surface(
                shape = CircleShape,
                color = mz.surface,
                border = BorderStroke(1.dp, mz.line),
            ) {
                IconButton(
                    onClick = onOpenTheme,
                    modifier = Modifier.testTag("header_theme_button"),
                ) {
                    val icon = when (themeMode) {
                        "custom" -> Icons.Rounded.Palette
                        "light" -> Icons.Rounded.LightMode
                        "dark" -> Icons.Rounded.DarkMode
                        else -> Icons.Rounded.Contrast
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = Strings.pk("المظهر والألوان", "Appearance and theme", lang),
                        tint = mz.accentText,
                    )
                }
            }
        }
    }
}

@Composable
fun MiniTimerPill(
    timer: TimerRuntime,
    currentTab: AppTab,
    subjectName: String,
    lang: String,
    arDigits: Boolean,
    onOpenTimer: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    AnimatedVisibility(visible = timer.running && currentTab != AppTab.TIMER) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenTimer)
                .testTag("mini_timer_pill"),
            shape = RoundedCornerShape(14.dp),
            color = mz.soft,
            border = BorderStroke(1.dp, mz.accent.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(mz.accent),
                )
                Text(
                    text = if (timer.mode == TimerMode.FOCUS) {
                        subjectName.ifBlank { Strings.pk("تركيز", "Focus", lang) }
                    } else {
                        Strings.pk("استراحة", "Break", lang)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = mz.ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateUtils.applyDigits(DateUtils.mmss(timer.leftSec), lang, arDigits),
                    style = rubikNumStyle(MaterialTheme.typography.labelLarge),
                    color = mz.accentText,
                )
                Text(
                    text = Strings.pk("إيقاف", "Pause", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = mz.accentText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTogglePause)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun MzBottomNavigationBar(
    currentTab: AppTab,
    lang: String,
    unreadFriends: Int,
    onSelectTab: (AppTab) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    Surface(
        color = mz.surface,
        border = BorderStroke(1.dp, mz.line),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavDestinations.forEach { dest ->
                val selected = dest.tab == currentTab
                val fg by animateColorAsState(
                    targetValue = if (selected) mz.accentText else mz.muted,
                    animationSpec = tween(200),
                    label = "nav_fg",
                )
                val pillBg by animateColorAsState(
                    targetValue = if (selected) mz.soft else mz.surface.copy(alpha = 0f),
                    animationSpec = tween(200),
                    label = "nav_pill",
                )
                val label = Strings.pk(dest.ar, dest.en, lang)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(dest.tab) }
                        .testTag("nav_tab_${dest.tab.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) dest.activeIcon else dest.inactiveIcon,
                                contentDescription = label,
                                tint = fg,
                                modifier = Modifier.size(21.dp),
                            )
                            if (dest.tab == AppTab.HUB && unreadFriends > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(mz.hot),
                                )
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MzSideNavigationRail(
    currentTab: AppTab,
    lang: String,
    unreadFriends: Int,
    onSelectTab: (AppTab) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    NavigationRail(
        containerColor = mz.surface,
        contentColor = mz.ink,
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        NavDestinations.forEach { dest ->
            val selected = dest.tab == currentTab
            val label = Strings.pk(dest.ar, dest.en, lang)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(dest.tab) },
                icon = {
                    Box {
                        Icon(
                            imageVector = if (selected) dest.activeIcon else dest.inactiveIcon,
                            contentDescription = label,
                        )
                        if (dest.tab == AppTab.HUB && unreadFriends > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(mz.hot),
                            )
                        }
                    }
                },
                label = {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = mz.accentText,
                    selectedTextColor = mz.accentText,
                    indicatorColor = mz.soft,
                    unselectedIconColor = mz.muted,
                    unselectedTextColor = mz.muted,
                ),
                modifier = Modifier.testTag("rail_tab_${dest.tab.id}"),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onOpenSettings,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = Strings.pk("الإعدادات", "Settings", lang),
                )
            },
            label = {
                Text(Strings.pk("الإعدادات", "Settings", lang), style = MaterialTheme.typography.labelSmall)
            },
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = mz.muted,
                unselectedTextColor = mz.muted,
            ),
            modifier = Modifier.testTag("rail_settings"),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
