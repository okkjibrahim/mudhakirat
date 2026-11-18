package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveSheet
import com.example.model.AppState
import com.example.model.SortMode
import com.example.model.StudyTask
import com.example.model.Subject
import com.example.model.TaskFilter
import com.example.model.TaskViewMode
import com.example.ui.components.MzCard
import com.example.ui.components.MzSegmentedTabs
import com.example.ui.components.TaskRowItem
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.rubikNumStyle
import com.example.util.DateUtils
import com.example.util.Strings
import java.util.Calendar

@Composable
fun TasksScreen(
    state: AppState,
    taskFilter: TaskFilter,
    searchQuery: String,
    filterSubj: String,
    filterPri: String,
    filterDate: String,
    weekOffset: Int,
    onSetViewMode: (TaskViewMode) -> Unit,
    onSetFilter: (TaskFilter) -> Unit,
    onToggleSortMode: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterSubjChange: (String) -> Unit,
    onFilterPriChange: (String) -> Unit,
    onFilterDateChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onMoveWeekOffset: (Int, Boolean) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveManual: (String, Int, List<String>) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val td = DateUtils.todayKey()
    val subMap = remember(state.subjects) { state.subjects.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        MzSegmentedTabs(
            items = listOf(
                TaskViewMode.LIST to Strings.pk("قائمة", "List", lang),
                TaskViewMode.WEEK to Strings.pk("أسبوعي", "Week", lang),
            ),
            selected = state.taskView,
            onSelect = onSetViewMode,
            testTagPrefix = "tasks_view_mode",
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (state.taskView == TaskViewMode.LIST) {
            TasksListBody(
                state = state,
                subMap = subMap,
                td = td,
                taskFilter = taskFilter,
                searchQuery = searchQuery,
                filterSubj = filterSubj,
                filterPri = filterPri,
                filterDate = filterDate,
                onSetFilter = onSetFilter,
                onToggleSortMode = onToggleSortMode,
                onSearchChange = onSearchChange,
                onFilterSubjChange = onFilterSubjChange,
                onFilterPriChange = onFilterPriChange,
                onFilterDateChange = onFilterDateChange,
                onClearFilters = onClearFilters,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
                onMoveManual = onMoveManual,
                onOpenSheet = onOpenSheet,
            )
        } else {
            TasksWeekBody(
                state = state,
                subMap = subMap,
                td = td,
                weekOffset = weekOffset,
                onMoveWeekOffset = onMoveWeekOffset,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
                onOpenSheet = onOpenSheet,
            )
        }
    }
}

@Composable
private fun TasksListBody(
    state: AppState,
    subMap: Map<String, Subject>,
    td: String,
    taskFilter: TaskFilter,
    searchQuery: String,
    filterSubj: String,
    filterPri: String,
    filterDate: String,
    onSetFilter: (TaskFilter) -> Unit,
    onToggleSortMode: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterSubjChange: (String) -> Unit,
    onFilterPriChange: (String) -> Unit,
    onFilterDateChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveManual: (String, Int, List<String>) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val qNorm = remember(searchQuery) { DateUtils.normSearch(searchQuery) }
    val wkEnd = remember { DateUtils.addDaysKey(7) }

    val filtered = remember(state.tasks, taskFilter, qNorm, filterSubj, filterPri, filterDate, subMap) {
        state.tasks.filter { tk ->
            val okDone = when (taskFilter) {
                TaskFilter.OPEN -> !tk.done
                TaskFilter.DONE -> tk.done
                TaskFilter.ALL -> true
            }
            val okSubj = filterSubj.isEmpty() || tk.subj == filterSubj
            val okPri = filterPri.isEmpty() || tk.pri == filterPri
            val okDate = when (filterDate) {
                "over" -> tk.due.isNotBlank() && tk.due < td && !tk.done
                "today" -> tk.due == td
                "week" -> tk.due.isNotBlank() && tk.due in td..wkEnd
                "none" -> tk.due.isBlank()
                else -> true
            }
            val subjName = subMap[tk.subj]?.name ?: ""
            val okSearch = qNorm.isEmpty() || DateUtils.normSearch("${tk.title} $subjName").contains(qNorm)
            okDone && okSubj && okPri && okDate && okSearch
        }
    }

    val openFiltered = filtered.filter { !it.done }
    val doneFiltered = filtered.filter { it.done }
    val isManual = state.taskSort == SortMode.MANUAL
    val hasActiveFilters = filterSubj.isNotEmpty() || filterPri.isNotEmpty() || filterDate.isNotEmpty() || searchQuery.isNotBlank()

    var subjMenuOpen by remember { mutableStateOf(false) }
    var priMenuOpen by remember { mutableStateOf(false) }
    var dateMenuOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // Search box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(Strings.pk("بحث في المهام...", "Search tasks...", lang), color = mz.muted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = mz.muted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = Strings.pk("مسح", "Clear", lang), tint = mz.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = mz.accent,
                    unfocusedBorderColor = mz.line,
                    focusedContainerColor = mz.surface,
                    unfocusedContainerColor = mz.surface,
                    focusedTextColor = mz.ink,
                    unfocusedTextColor = mz.ink,
                ),
                modifier = Modifier.fillMaxWidth().testTag("tasks_search_input"),
            )
        }

        // Filter chips + Manual Sort toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Subject filter
                Box {
                    val sLabel = subMap[filterSubj]?.name ?: Strings.pk("المادة", "Subject", lang)
                    FilterPill(label = sLabel, active = filterSubj.isNotEmpty(), onClick = { subjMenuOpen = true })
                    DropdownMenu(expanded = subjMenuOpen, onDismissRequest = { subjMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(Strings.pk("كل المواد", "All subjects", lang)) },
                            onClick = { onFilterSubjChange(""); subjMenuOpen = false },
                        )
                        state.subjects.forEach { sb ->
                            DropdownMenuItem(
                                text = { Text(sb.name) },
                                onClick = { onFilterSubjChange(sb.id); subjMenuOpen = false },
                            )
                        }
                    }
                }

                // Priority filter
                Box {
                    val pLabel = if (filterPri.isNotEmpty()) Strings.priorityLabel(filterPri, lang) else Strings.pk("الأولوية", "Priority", lang)
                    FilterPill(label = pLabel, active = filterPri.isNotEmpty(), onClick = { priMenuOpen = true })
                    DropdownMenu(expanded = priMenuOpen, onDismissRequest = { priMenuOpen = false }) {
                        listOf("" to Strings.pk("الكل", "All", lang), "high" to Strings.priorityLabel("high", lang), "med" to Strings.priorityLabel("med", lang), "low" to Strings.priorityLabel("low", lang)).forEach { (k, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = { onFilterPriChange(k); priMenuOpen = false },
                            )
                        }
                    }
                }

                // Date filter
                Box {
                    val dLabel = when (filterDate) {
                        "over" -> Strings.pk("متأخرة", "Overdue", lang)
                        "today" -> Strings.pk("اليوم", "Today", lang)
                        "week" -> Strings.pk("هذا الأسبوع", "This week", lang)
                        "none" -> Strings.pk("بدون موعد", "No date", lang)
                        else -> Strings.pk("الموعد", "Due date", lang)
                    }
                    FilterPill(label = dLabel, active = filterDate.isNotEmpty(), onClick = { dateMenuOpen = true })
                    DropdownMenu(expanded = dateMenuOpen, onDismissRequest = { dateMenuOpen = false }) {
                        listOf(
                            "" to Strings.pk("كل المواعيد", "All dates", lang),
                            "today" to Strings.pk("اليوم", "Today", lang),
                            "week" to Strings.pk("هذا الأسبوع", "This week", lang),
                            "over" to Strings.pk("متأخرة", "Overdue", lang),
                            "none" to Strings.pk("بدون موعد", "No date", lang),
                        ).forEach { (k, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = { onFilterDateChange(k); dateMenuOpen = false },
                            )
                        }
                    }
                }

                // Manual sort toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isManual) mz.soft else mz.surface,
                    border = BorderStroke(1.dp, if (isManual) mz.accent else mz.line),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleSortMode)
                        .testTag("tasks_sort_toggle"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = if (isManual) mz.accentText else mz.muted, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isManual) Strings.pk("ترتيب يدوي", "Manual", lang) else Strings.pk("تلقائي", "Auto", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isManual) mz.accentText else mz.ink,
                        )
                    }
                }

                if (hasActiveFilters) {
                    TextButton(onClick = onClearFilters) {
                        Text(Strings.pk("مسح الفلاتر", "Clear", lang), color = mz.hot)
                    }
                }
            }
        }

        // Open / Done / All segmented bar
        item {
            MzSegmentedTabs(
                items = listOf(
                    TaskFilter.OPEN to Strings.pk("غير مكتملة", "Open", lang),
                    TaskFilter.DONE to Strings.pk("مكتملة", "Done", lang),
                    TaskFilter.ALL to Strings.pk("الكل", "All", lang),
                ),
                selected = taskFilter,
                onSelect = onSetFilter,
                testTagPrefix = "tasks_status_tab",
            )
        }

        if (filtered.isEmpty()) {
            item {
                MzCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = Strings.pk("لا توجد مهام مطابقة", "No matching tasks", lang),
                            style = MaterialTheme.typography.titleSmall,
                            color = mz.ink,
                        )
                        Text(
                            text = Strings.pk("أضف مهمة جديدة من زر + بالأسفل.", "Add a new task from the + button below.", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = mz.muted,
                        )
                    }
                }
            }
        } else if (isManual && openFiltered.isNotEmpty()) {
            val visibleIds = openFiltered.map { it.id }
            items(openFiltered, key = { it.id }) { tk ->
                TaskRowItem(
                    task = tk,
                    subject = subMap[tk.subj],
                    lang = lang,
                    arDigits = state.arDigits,
                    onToggleDone = { onToggleTask(tk.id) },
                    onEdit = { onOpenSheet(ActiveSheet.TaskEditor(tk.id)) },
                    onDelete = { onDeleteTask(tk.id) },
                    modifier = Modifier.animateItem(),
                    trailingControl = {
                        Row {
                            IconButton(onClick = { onMoveManual(tk.id, -1, visibleIds) }) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = Strings.pk("أعلى", "Up", lang), tint = mz.muted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMoveManual(tk.id, 1, visibleIds) }) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = Strings.pk("أسفل", "Down", lang), tint = mz.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                )
            }
            if (doneFiltered.isNotEmpty() && taskFilter != TaskFilter.OPEN) {
                item {
                    GroupTitle(Strings.pk("مكتملة", "Completed", lang))
                }
                items(doneFiltered, key = { it.id }) { tk ->
                    TaskRowItem(
                        task = tk,
                        subject = subMap[tk.subj],
                        lang = lang,
                        arDigits = state.arDigits,
                        onToggleDone = { onToggleTask(tk.id) },
                        onEdit = { onOpenSheet(ActiveSheet.TaskEditor(tk.id)) },
                        onDelete = { onDeleteTask(tk.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        } else {
            val groups = listOf(
                Strings.pk("متأخرة", "Overdue", lang) to openFiltered.filter { it.due.isNotBlank() && it.due < td },
                Strings.pk("اليوم", "Today", lang) to openFiltered.filter { it.due == td },
                Strings.pk("لاحقًا", "Later", lang) to openFiltered.filter { it.due.isNotBlank() && it.due > td },
                Strings.pk("بدون موعد", "No date", lang) to openFiltered.filter { it.due.isBlank() },
                Strings.pk("مكتملة", "Completed", lang) to doneFiltered,
            ).filter { it.second.isNotEmpty() }

            groups.forEach { (groupTitle, itemsInGroup) ->
                item(key = "hdr_$groupTitle") {
                    GroupTitle("$groupTitle (${DateUtils.applyDigits(itemsInGroup.size.toString(), lang, state.arDigits)})")
                }
                items(itemsInGroup, key = { it.id }) { tk ->
                    TaskRowItem(
                        task = tk,
                        subject = subMap[tk.subj],
                        lang = lang,
                        arDigits = state.arDigits,
                        onToggleDone = { onToggleTask(tk.id) },
                        onEdit = { onOpenSheet(ActiveSheet.TaskEditor(tk.id)) },
                        onDelete = { onDeleteTask(tk.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TasksWeekBody(
    state: AppState,
    subMap: Map<String, Subject>,
    td: String,
    weekOffset: Int,
    onMoveWeekOffset: (Int, Boolean) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    val lang = state.lang
    val ar = state.arDigits
    val startCal = remember(weekOffset, lang) { DateUtils.weekStart(weekOffset, lang) }
    val dayKeys = remember(startCal) {
        (0..6).map { offset ->
            val c = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            DateUtils.dateKey(c)
        }
    }
    val overdueOpen = remember(state.tasks, td) {
        state.tasks.filter { !it.done && it.due.isNotBlank() && it.due < td }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // Week navigator
        item {
            MzCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { onMoveWeekOffset(-1, false) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = Strings.pk("الأسبوع السابق", "Previous week", lang), tint = mz.ink)
                    }
                    TextButton(onClick = { onMoveWeekOffset(0, true) }) {
                        Text(
                            text = if (weekOffset == 0) Strings.pk("هذا الأسبوع", "This week", lang)
                            else "${DateUtils.formatDateShort(dayKeys.first(), lang, ar)} — ${DateUtils.formatDateShort(dayKeys.last(), lang, ar)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = mz.ink,
                        )
                    }
                    IconButton(onClick = { onMoveWeekOffset(1, false) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = Strings.pk("الأسبوع التالي", "Next week", lang), tint = mz.ink)
                    }
                }
            }
        }

        if (weekOffset == 0 && overdueOpen.isNotEmpty()) {
            item {
                MzCard(
                    backgroundColor = mz.hot.copy(alpha = 0.08f),
                    borderColor = mz.hot.copy(alpha = 0.35f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${Strings.pk("مهام متأخرة", "Overdue", lang)} (${DateUtils.applyDigits(overdueOpen.size.toString(), lang, ar)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = mz.hot,
                        )
                        overdueOpen.forEach { tk ->
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

        items(dayKeys, key = { it }) { dayKey ->
            val dayTasks = state.tasks.filter { it.due == dayKey }
            val dayMins = DateUtils.minsOn(state, dayKey)
            val isToday = dayKey == td
            MzCard(
                borderColor = if (isToday) mz.accent else mz.line,
                backgroundColor = if (isToday) mz.soft.copy(alpha = 0.45f) else mz.surface,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = DateUtils.formatWeekdayLong(dayKey, lang, ar),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isToday) mz.accentText else mz.ink,
                            )
                            Text(
                                text = DateUtils.formatDateShort(dayKey, lang, ar),
                                style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                color = mz.muted,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (dayMins > 0) {
                                Text(
                                    text = Strings.fmtMin(dayMins, lang, ar),
                                    style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                    color = mz.accentText,
                                )
                            }
                            if (dayTasks.isNotEmpty()) {
                                val doneC = dayTasks.count { it.done }
                                Text(
                                    text = DateUtils.applyDigits("$doneC/${dayTasks.size}", lang, ar),
                                    style = rubikNumStyle(MaterialTheme.typography.labelSmall),
                                    color = mz.muted,
                                )
                            }
                            IconButton(
                                onClick = { onOpenSheet(ActiveSheet.TaskEditor(null, presetDue = dayKey)) },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = Strings.pk("إضافة مهمة لهذا اليوم", "Add task for day", lang),
                                    tint = mz.accentText,
                                )
                            }
                        }
                    }

                    dayTasks.forEach { tk ->
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
}

@Composable
private fun FilterPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val mz = LocalMudhakiratiColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) mz.soft else mz.surface,
        border = BorderStroke(1.dp, if (active) mz.accent else mz.line),
        modifier = Modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) mz.accentText else mz.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun GroupTitle(text: String) {
    val mz = LocalMudhakiratiColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = mz.muted,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}
