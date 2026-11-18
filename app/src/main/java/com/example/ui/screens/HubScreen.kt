package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.FriendItem
import com.example.model.FriendsState
import com.example.model.HubSubTab
import com.example.ui.components.AnimatedFlameIcon
import com.example.ui.components.MzButton
import com.example.ui.components.MzCard
import com.example.ui.components.MzGhostButton
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.components.UserAvatar
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings

@Composable
fun HubScreen(
    state: AppState,
    hubTab: HubSubTab,
    scheduleDayView: Boolean,
    selectedSchedDay: Int,
    friendsState: FriendsState,
    friendNameFn: (String, String) -> String,
    onSelectHubTab: (HubSubTab) -> Unit,
    onToggleScheduleView: () -> Unit,
    onSelectScheduleDay: (Int) -> Unit,
    onChangePeriods: (Int) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
    onRefreshFriends: () -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onRespondRequest: (String, Boolean) -> Unit,
    onCancelOutgoing: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onOpenChat: (FriendItem) -> Unit,
    onSetNowKind: (String) -> Unit,
    onSetNowDuration: (Int) -> Unit,
    onSelectNowSubject: (String) -> Unit,
    onSetNowCustomName: (String) -> Unit,
    onStartNowStatus: () -> Unit,
    onEndNowStatus: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val lang = state.lang
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        MzSegmentedTabs(
            items = listOf(
                HubSubTab.SCHED to Strings.pk("الجدول", "Schedule", lang),
                HubSubTab.FRIENDS to Strings.pk("الأصدقاء", "Friends", lang),
                HubSubTab.NOTES to Strings.pk("الملاحظات", "Notes", lang),
            ),
            selected = hubTab,
            onSelect = onSelectHubTab,
            testTagPrefix = "hub_subtab",
        )
        Spacer(modifier = Modifier.height(10.dp))

        when (hubTab) {
            HubSubTab.SCHED -> ScheduleTabBody(
                state = state,
                scheduleDayView = scheduleDayView,
                selectedSchedDay = selectedSchedDay,
                onToggleScheduleView = onToggleScheduleView,
                onSelectScheduleDay = onSelectScheduleDay,
                onChangePeriods = onChangePeriods,
                onOpenSheet = onOpenSheet,
            )
            HubSubTab.FRIENDS -> FriendsTabBody(
                state = state,
                friendsState = friendsState,
                friendNameFn = friendNameFn,
                onRefreshFriends = onRefreshFriends,
                onSendFriendRequest = onSendFriendRequest,
                onRespondRequest = onRespondRequest,
                onCancelOutgoing = onCancelOutgoing,
                onRemoveFriend = onRemoveFriend,
                onOpenChat = onOpenChat,
                onSetNowKind = onSetNowKind,
                onSetNowDuration = onSetNowDuration,
                onSelectNowSubject = onSelectNowSubject,
                onSetNowCustomName = onSetNowCustomName,
                onStartNowStatus = onStartNowStatus,
                onEndNowStatus = onEndNowStatus,
                onShowToast = onShowToast,
            )
            HubSubTab.NOTES -> NotesTabBody(
                state = state,
                onOpenSheet = onOpenSheet,
            )
        }
    }
}

@Composable
private fun ScheduleTabBody(
    state: AppState,
    scheduleDayView: Boolean,
    selectedSchedDay: Int,
    onToggleScheduleView: () -> Unit,
    onSelectScheduleDay: (Int) -> Unit,
    onChangePeriods: (Int) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val days = state.sched.days.ifEmpty { listOf(0, 1, 2, 3, 4) }
    val dayNamesShort = if (lang == "en") Strings.daysShortEn else Strings.daysShortAr
    val dayNamesLong = if (lang == "en") Strings.daysLongEn else Strings.daysLongAr
    val subjColors = remember(state.subjects) {
        state.subjects.associate { it.name.trim().lowercase() to parseHexColor(it.color) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            MzCard(contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = mz.field,
                            border = BorderStroke(1.dp, mz.line),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onToggleScheduleView)
                                .testTag("sched_toggle_view"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = if (scheduleDayView) Icons.Rounded.ViewWeek else Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = mz.accentText,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (scheduleDayView) Strings.pk("الأسبوع", "Week", lang) else Strings.pk("يوم بيوم", "Day", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mz.ink,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = mz.field,
                            border = BorderStroke(1.dp, mz.line),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenSheet(ActiveSheet.ScheduleDaysPicker) }
                                .testTag("sched_pick_days"),
                        ) {
                            Text(
                                text = Strings.pk("الأيام", "Days", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = mz.ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }

                    // Periods stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(onClick = { onChangePeriods(-1) }, enabled = state.sched.n > 1) {
                            Text("-", style = MaterialTheme.typography.titleMedium, color = mz.ink)
                        }
                        Text(
                            text = "${DateUtils.applyDigits(state.sched.n.toString(), lang, ar)} ${Strings.pk("حصص", "periods", lang)}",
                            style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                            color = mz.ink,
                        )
                        IconButton(onClick = { onChangePeriods(1) }, enabled = state.sched.n < 12) {
                            Text("+", style = MaterialTheme.typography.titleMedium, color = mz.ink)
                        }
                    }
                }
            }
        }

        if (scheduleDayView) {
            val activeDay = if (days.contains(selectedSchedDay)) selectedSchedDay else days.first()
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    days.forEach { d ->
                        val sel = d == activeDay
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) mz.accent else mz.surface,
                            border = BorderStroke(1.dp, if (sel) mz.accent else mz.line),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectScheduleDay(d) },
                        ) {
                            Text(
                                text = dayNamesShort.getOrElse(d) { "" },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) mz.accentInk else mz.ink,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }

            items(state.sched.n) { pIdx ->
                val key = "$activeDay-$pIdx"
                val cellVal = state.sched.cells[key] ?: ""
                val matchedCol = subjColors[cellVal.trim().lowercase()]
                MzCard(
                    onClick = { onOpenSheet(ActiveSheet.ScheduleCellEditor(key)) },
                    borderColor = matchedCol?.copy(alpha = 0.5f) ?: mz.line,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${Strings.pk("الحصة", "Period", lang)} ${DateUtils.applyDigits((pIdx + 1).toString(), lang, ar)}",
                            style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                            color = mz.muted,
                        )
                        Text(
                            text = cellVal.ifBlank { Strings.pk("اضغط للتعيين", "Tap to assign", lang) },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (cellVal.isBlank()) mz.muted else (matchedCol ?: mz.ink),
                        )
                    }
                }
            }
        } else {
            item {
                MzCard(contentPadding = PaddingValues(10.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Header row
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.width(52.dp))
                            days.forEach { d ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = mz.soft,
                                    modifier = Modifier.width(86.dp),
                                ) {
                                    Text(
                                        text = dayNamesShort.getOrElse(d) { "" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = mz.accentText,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    )
                                }
                            }
                        }

                        for (pIdx in 0 until state.sched.n) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = DateUtils.applyDigits("${pIdx + 1}", lang, ar),
                                    style = rubikNumStyle(MaterialTheme.typography.labelMedium),
                                    color = mz.muted,
                                    modifier = Modifier.width(52.dp).padding(horizontal = 8.dp),
                                )
                                days.forEach { d ->
                                    val key = "$d-$pIdx"
                                    val v = state.sched.cells[key] ?: ""
                                    val c = subjColors[v.trim().lowercase()]
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = c?.copy(alpha = 0.14f) ?: mz.field,
                                        border = BorderStroke(1.dp, c?.copy(alpha = 0.45f) ?: mz.line),
                                        modifier = Modifier
                                            .width(86.dp)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onOpenSheet(ActiveSheet.ScheduleCellEditor(key)) }
                                            .testTag("sched_cell_$key"),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        ) {
                                            Text(
                                                text = v.ifBlank { "—" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = c ?: if (v.isBlank()) mz.muted else mz.ink,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FriendsTabBody(
    state: AppState,
    friendsState: FriendsState,
    friendNameFn: (String, String) -> String,
    onRefreshFriends: () -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onRespondRequest: (String, Boolean) -> Unit,
    onCancelOutgoing: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onOpenChat: (FriendItem) -> Unit,
    onSetNowKind: (String) -> Unit,
    onSetNowDuration: (Int) -> Unit,
    onSelectNowSubject: (String) -> Unit,
    onSetNowCustomName: (String) -> Unit,
    onStartNowStatus: () -> Unit,
    onEndNowStatus: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val clipboard = LocalClipboardManager.current
    var addCodeInput by remember { mutableStateOf("") }

    val myStreak = DateUtils.computeStreak(state)
    val mineDays = remember(state.act, state.logs) { DateUtils.myActiveDays(state).toSet() }
    val myIdFmt = Strings.formatFriendId(state.fid)
    val isNowRunning = DateUtils.isNowActive(state.nowT, state.nowAt)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // Your Streak & Copyable ID card
        item {
            MzCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserAvatar(name = state.profile.name, photoDataUrl = state.profile.photo, size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.profile.name.ifBlank { Strings.pk("أنت", "You", lang) },
                            style = MaterialTheme.typography.titleMedium,
                            color = mz.ink,
                        )
                        if (state.fid.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    clipboard.setText(AnnotatedString(myIdFmt))
                                    onShowToast(Strings.pk("تم نسخ الـ ID", "ID copied", lang))
                                },
                            ) {
                                Text(
                                    text = "ID: $myIdFmt",
                                    style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                    color = mz.muted,
                                )
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AnimatedFlameIcon(streak = myStreak, size = 22.dp)
                        Text(
                            text = DateUtils.applyDigits(myStreak.toString(), lang, ar),
                            style = rubikNumStyle(MaterialTheme.typography.titleMedium),
                            color = mz.ink,
                        )
                    }
                    IconButton(onClick = onRefreshFriends) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = Strings.pk("تحديث", "Refresh", lang),
                            tint = mz.muted,
                        )
                    }
                }
            }
        }

        // What are you studying now card
        item {
            MzCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Strings.pk("ماذا تدرس الآن؟", "What are you studying now?", lang),
                            style = MaterialTheme.typography.titleSmall,
                            color = mz.ink,
                        )
                        if (isNowRunning) {
                            TextButton(onClick = onEndNowStatus) {
                                Text(Strings.pk("إنهاء الحالة", "End status", lang), color = mz.hot)
                            }
                        }
                    }

                    if (isNowRunning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = mz.soft,
                            border = BorderStroke(1.dp, mz.accent.copy(alpha = 0.45f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(state.nowT, style = MaterialTheme.typography.bodyMedium, color = mz.ink)
                                Text(
                                    text = DateUtils.formatAgo(state.nowAt, lang, ar),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mz.accentText,
                                )
                            }
                        }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "lesson" to Strings.pk("درس", "Lesson", lang),
                                "hw" to Strings.pk("واجب", "HW", lang),
                                "book" to Strings.pk("كتاب", "Book", lang),
                                "custom" to Strings.pk("مخصص", "Custom", lang),
                            ).forEach { (k, lbl) ->
                                val sel = friendsState.nowKind == k
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (sel) mz.accent else mz.field,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onSetNowKind(k) },
                                ) {
                                    Text(
                                        text = lbl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sel) mz.accentInk else mz.ink,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    )
                                }
                            }
                        }

                        if (friendsState.nowKind == "custom") {
                            OutlinedTextField(
                                value = friendsState.nowCustomName,
                                onValueChange = onSetNowCustomName,
                                placeholder = { Text(Strings.pk("مثال: مراجعة الفيزياء", "e.g. Physics review", lang)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mz.accent,
                                    unfocusedBorderColor = mz.line,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (state.subjects.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                state.subjects.forEach { sb ->
                                    val sel = friendsState.nowSelectedName == sb.name
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (sel) mz.soft else mz.field,
                                        border = BorderStroke(1.dp, if (sel) mz.accent else mz.line),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectNowSubject(sb.name) },
                                    ) {
                                        Text(
                                            text = sb.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) mz.accentText else mz.ink,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    val sel = friendsState.nowDuration == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sel) mz.soft else mz.field,
                                        border = BorderStroke(1.dp, if (sel) mz.accent else mz.line),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSetNowDuration(mins) },
                                    ) {
                                        Text(
                                            text = DateUtils.applyDigits("$mins", lang, ar),
                                            style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                            color = if (sel) mz.accentText else mz.ink,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                            MzButton(
                                text = Strings.pk("شارك الآن", "Start", lang),
                                onClick = onStartNowStatus,
                                testTag = "friends_start_now",
                            )
                        }
                    }
                }
            }
        }

        // Add Friend Box
        item {
            MzCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = Strings.pk("إضافة صديق عبر الـ ID", "Add friend by ID", lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = mz.ink,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = addCodeInput,
                            onValueChange = { addCodeInput = it.uppercase() },
                            placeholder = { Text("ABCD-1234", color = mz.muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = mz.accent,
                                unfocusedBorderColor = mz.line,
                                focusedContainerColor = mz.field,
                                unfocusedContainerColor = mz.field,
                            ),
                            modifier = Modifier.weight(1f).testTag("friends_add_input"),
                        )
                        MzButton(
                            text = Strings.pk("إرسال", "Send", lang),
                            enabled = !friendsState.busy,
                            onClick = { onSendFriendRequest(addCodeInput) },
                            testTag = "friends_send_request",
                        )
                    }
                    if (friendsState.msg.isNotBlank()) {
                        Text(
                            text = friendsState.msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (friendsState.ok) mz.ok else mz.hot,
                        )
                    }
                }
            }
        }

        // Incoming requests
        if (friendsState.incoming.isNotEmpty()) {
            item {
                Text(
                    text = Strings.pk("طلبات واردة", "Incoming requests", lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = mz.muted,
                )
            }
            items(friendsState.incoming, key = { "inc_${it.code}" }) { req ->
                MzCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        UserAvatar(name = friendNameFn(req.code, req.name), photoDataUrl = req.photo, size = 40.dp)
                        Text(
                            text = friendNameFn(req.code, req.name),
                            style = MaterialTheme.typography.titleSmall,
                            color = mz.ink,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRespondRequest(req.code, true) }) {
                            Icon(Icons.Rounded.Check, contentDescription = Strings.pk("قبول", "Accept", lang), tint = mz.ok)
                        }
                        IconButton(onClick = { onRespondRequest(req.code, false) }) {
                            Icon(Icons.Rounded.Close, contentDescription = Strings.pk("رفض", "Decline", lang), tint = mz.hot)
                        }
                    }
                }
            }
        }

        // Outgoing requests
        if (friendsState.outgoing.isNotEmpty()) {
            item {
                Text(
                    text = Strings.pk("طلبات مرسلة", "Pending outgoing", lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = mz.muted,
                )
            }
            items(friendsState.outgoing, key = { "out_${it.code}" }) { req ->
                MzCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${friendNameFn(req.code, req.name)} (${Strings.formatFriendId(req.code)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = mz.ink,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onCancelOutgoing(req.code) }) {
                            Text(Strings.pk("إلغاء", "Cancel", lang), color = mz.hot)
                        }
                    }
                }
            }
        }

        // Friends List
        if (friendsState.list.isEmpty()) {
            item {
                MzCard {
                    Text(
                        text = if (friendsState.err == "login") {
                            Strings.pk("سجّل بحساب سحابي مؤكّد للتواصل مع الأصدقاء.", "Sign in with a verified cloud account to connect with friends.", lang)
                        } else {
                            Strings.pk("لا يوجد أصدقاء بعد. شارك الـ ID الخاص بك لتبدؤوا المنافسة الهادئة.", "No friends yet. Share your ID to study together.", lang)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = mz.muted,
                    )
                }
            }
        } else {
            items(friendsState.list, key = { it.code }) { f ->
                val fStreak = DateUtils.computeStreakFromSet(f.days)
                val shared = DateUtils.computeSharedStreak(mineDays, f.days)
                val liveNow = DateUtils.isNowActive(f.nowText, f.nowAtMs)
                MzCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            UserAvatar(name = friendNameFn(f.code, f.name), photoDataUrl = f.photo, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friendNameFn(f.code, f.name),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = mz.ink,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        AnimatedFlameIcon(streak = fStreak, size = 16.dp)
                                        Text(
                                            text = DateUtils.applyDigits(fStreak.toString(), lang, ar),
                                            style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                            color = mz.ink,
                                        )
                                    }
                                    if (shared > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Rounded.Group, contentDescription = null, tint = mz.accentText, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = DateUtils.applyDigits(shared.toString(), lang, ar),
                                                style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                                color = mz.accentText,
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onOpenChat(f) },
                                modifier = Modifier.testTag("friend_chat_${f.code}"),
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = Strings.pk("محادثة", "Chat", lang),
                                        tint = mz.accentText,
                                    )
                                    if (f.unread > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(mz.hot),
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onRemoveFriend(f.code) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = Strings.pk("حذف", "Remove", lang),
                                    tint = mz.muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        if (liveNow) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = mz.soft,
                            ) {
                                Text(
                                    text = "${f.nowText} · ${DateUtils.formatAgo(f.nowAtMs, lang, ar)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mz.accentText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
private fun NotesTabBody(
    state: AppState,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ordered = remember(state.notes) {
        state.notes.sortedWith(compareByDescending<com.example.model.StudyNote> { it.pin }.thenByDescending { it.ts })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            MzGhostButton(
                text = Strings.pk("+ ملاحظة جديدة", "+ New note", lang),
                onClick = { onOpenSheet(ActiveSheet.NoteEditor(null)) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "notes_add_button",
            )
        }

        if (ordered.isEmpty()) {
            item {
                MzCard {
                    Text(
                        text = Strings.pk("دوّن ملخصاتك وأفكارك السريعة هنا.", "Capture quick study notes and reminders here.", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mz.muted,
                    )
                }
            }
        } else {
            items(ordered, key = { it.id }) { note ->
                val noteCol = parseHexColor(note.color)
                MzCard(
                    onClick = { onOpenSheet(ActiveSheet.NoteEditor(note.id)) },
                    borderColor = noteCol.copy(alpha = 0.5f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(noteCol),
                                )
                                if (note.title.isNotBlank()) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = mz.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (note.pin) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = Strings.pk("مثبتة", "Pinned", lang),
                                    tint = mz.accentText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text = note.txt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = mz.ink,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
