package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ActiveSheet
import com.example.model.AppTab
import com.example.ui.components.HeaderBar
import com.example.ui.components.MiniTimerPill
import com.example.ui.components.MzBottomNavigationBar
import com.example.ui.components.MzConfirmDialog
import com.example.ui.components.MzSideNavigationRail
import com.example.ui.components.UndoToastBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.sheets.FriendChatSheetContent
import com.example.ui.sheets.ManualLogSheetContent
import com.example.ui.sheets.NoteEditorSheetContent
import com.example.ui.sheets.PlanEditorSheetContent
import com.example.ui.sheets.ProfileSettingsSheetContent
import com.example.ui.sheets.ReviewEditorSheetContent
import com.example.ui.sheets.ScheduleCellSheetContent
import com.example.ui.sheets.ScheduleDaysSheetContent
import com.example.ui.sheets.StreakDetailsSheetContent
import com.example.ui.sheets.SubjectEditorSheetContent
import com.example.ui.sheets.SubjectTypePickerSheetContent
import com.example.ui.sheets.TaskEditorSheetContent
import com.example.ui.sheets.ThemePickerSheetContent
import com.example.ui.sheets.TimerSettingsSheetContent
import com.example.ui.sheets.TopicsEditorSheetContent
import com.example.ui.sheets.WelcomeSheetContent
import com.example.ui.theme.FontScaleSteps
import com.example.ui.theme.LocalMudhakiratiColors
import com.example.ui.theme.MudhakiratiTheme
import com.example.util.DateUtils
import com.example.util.Strings
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    companion object {
        const val CHANNEL_ID = "mudhakirati_alerts_v2"
        const val OLD_CHANNEL_ID = "mudhakirati_alerts"

        @Volatile
        var inForeground: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        FcmService.fetchToken(applicationContext)

        setContent {
            MudhakiratiAppRoot()
        }
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
    }

    override fun onPause() {
        super.onPause()
        inForeground = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MudhakiratiAppRoot(
    vm: MainViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val auth by vm.auth.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val friends by vm.friends.collectAsStateWithLifecycle()
    val chat by vm.chat.collectAsStateWithLifecycle()
    val activeSheet by vm.activeSheet.collectAsStateWithLifecycle()
    val fontScaleIdx by vm.fontScaleIndex.collectAsStateWithLifecycle()
    val schedDayView by vm.scheduleDayView.collectAsStateWithLifecycle()
    val selectedSchedDay by vm.selectedSchedDay.collectAsStateWithLifecycle()
    val hubTab by vm.hubTab.collectAsStateWithLifecycle()
    val taskFilter by vm.taskFilter.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val filterSubj by vm.filterSubj.collectAsStateWithLifecycle()
    val filterPri by vm.filterPri.collectAsStateWithLifecycle()
    val filterDate by vm.filterDate.collectAsStateWithLifecycle()
    val weekOffset by vm.weekOffset.collectAsStateWithLifecycle()
    val monthOffset by vm.monthOffset.collectAsStateWithLifecycle()
    val selectedProgDay by vm.selectedProgressDay.collectAsStateWithLifecycle()
    val confirmDelSubjId by vm.confirmDeleteSubjectId.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val confirmDialog by vm.confirmDialog.collectAsStateWithLifecycle()
    val clockText by vm.clockText.collectAsStateWithLifecycle()
    val headerDate by vm.headerDateText.collectAsStateWithLifecycle()
    val greetingIdx by vm.greetingIdx.collectAsStateWithLifecycle()
    val unreadTotal by vm.unreadTotal.collectAsStateWithLifecycle()
    val streakFriendsAlert by vm.streakFriendsAlert.collectAsStateWithLifecycle()

    val fontScale = FontScaleSteps[fontScaleIdx.coerceIn(0, FontScaleSteps.lastIndex)]

    // Notification permission on Android 13+
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    LaunchedEffect(auth.isLoggedIn) {
        if (auth.isLoggedIn && Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Photo Picker launcher (Zero-Permission Android Photo Picker)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) vm.handlePickedPhotoUri(uri)
    }

    // Backup JSON Import launcher
    val backupImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.importBackupUri(uri)
    }

    MudhakiratiTheme(
        themeMode = state.theme,
        paletteId = state.palette,
        fontScale = fontScale,
        lang = state.lang,
    ) {
        val mz = LocalMudhakiratiColors.current
        val lang = state.lang

        // BackHandler hierarchy
        BackHandler(enabled = confirmDialog != null || activeSheet != ActiveSheet.None || (auth.isLoggedIn && state.view != AppTab.HOME)) {
            when {
                confirmDialog != null -> vm.dismissConfirm()
                activeSheet != ActiveSheet.None -> vm.closeSheet()
                auth.isLoggedIn && state.view != AppTab.HOME -> vm.selectTab(AppTab.HOME)
            }
        }

        if (!auth.isLoggedIn) {
            AuthScreen(
                auth = auth,
                lang = lang,
                fontScaleIdx = fontScaleIdx,
                onDismissIntro = vm::dismissWelcomeSplash,
                onOpenSheet = vm::openAuthSheet,
                onCloseSheet = vm::closeAuthSheet,
                onSetMode = vm::setAuthMode,
                onSubmitAuth = vm::submitAuth,
                onSubmitVerify = vm::submitVerifyCode,
                onSkipLocal = vm::skipVerificationLocalMode,
                onResendCode = vm::resendVerification,
                onSubmitForgot = vm::submitForgotPassword,
                onSubmitReset = vm::submitResetPassword,
                onToggleLang = vm::toggleAuthLanguage,
                onChangeFontScale = vm::setFontScaleIndex,
            )
            return@MudhakiratiTheme
        }

        val streak = DateUtils.computeStreak(state)
        val openTasksCount = state.tasks.count { !it.done }
        val subtitle = when (state.view) {
            AppTab.HOME -> Strings.openTasksMsg(openTasksCount, lang, state.arDigits)
            AppTab.TASKS -> if (taskFilter == com.example.model.TaskFilter.DONE) {
                Strings.doneTasksMsg(state.tasks.count { it.done }, lang, state.arDigits)
            } else {
                Strings.openTasksMsg(openTasksCount, lang, state.arDigits)
            }
            AppTab.SUBJECTS -> Strings.pk("المواد والمراجعات", "Subjects & Reviews", lang)
            AppTab.TIMER -> Strings.pk("مؤقت التركيز", "Focus Timer", lang)
            AppTab.HUB -> Strings.pk("الجدول، الأصدقاء والملاحظات", "Schedule, Friends & Notes", lang)
            AppTab.PROGRESS -> Strings.pk("تحليل تقدّمك الدراسي", "Study Progress", lang)
        }

        val timerSubjName = state.subjects.firstOrNull { it.id == timer.subjId }?.name ?: ""

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(mz.bg),
        ) {
            val useRail = (maxWidth >= 840.dp || state.layout == "laptop") && state.layout != "phone"

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    MzSideNavigationRail(
                        currentTab = state.view,
                        lang = lang,
                        unreadFriends = unreadTotal,
                        onSelectTab = vm::selectTab,
                        onOpenSettings = { vm.openSheet(ActiveSheet.ProfileSettings) },
                    )
                }

                Scaffold(
                    containerColor = mz.bg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HeaderBar(
                                userName = state.profile.name,
                                photoDataUrl = state.profile.photo,
                                dateText = headerDate,
                                clockText = clockText,
                                subtitle = subtitle,
                                greetingIdx = greetingIdx,
                                streak = streak,
                                unreadFriendsCount = unreadTotal,
                                themeMode = state.theme,
                                lang = lang,
                                arDigits = state.arDigits,
                                onOpenProfile = { vm.openSheet(ActiveSheet.ProfileSettings) },
                                onOpenStreak = { vm.openSheet(ActiveSheet.StreakDetails) },
                                onOpenTheme = vm::toggleQuickTheme,
                            )
                            MiniTimerPill(
                                timer = timer,
                                currentTab = state.view,
                                subjectName = timerSubjName,
                                lang = lang,
                                arDigits = state.arDigits,
                                onOpenTimer = { vm.selectTab(AppTab.TIMER) },
                                onTogglePause = vm::toggleTimerRunning,
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail) {
                            MzBottomNavigationBar(
                                currentTab = state.view,
                                lang = lang,
                                unreadFriends = unreadTotal,
                                onSelectTab = vm::selectTab,
                            )
                        }
                    },
                    floatingActionButton = {
                        if (state.view == AppTab.TASKS || state.view == AppTab.SUBJECTS) {
                            FloatingActionButton(
                                onClick = {
                                    if (state.view == AppTab.TASKS) {
                                        vm.openSheet(ActiveSheet.TaskEditor())
                                    } else {
                                        vm.openSheet(ActiveSheet.SubjectTypePicker)
                                    }
                                },
                                containerColor = mz.accent,
                                contentColor = mz.accentInk,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.testTag("main_fab"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = Strings.pk("إضافة", "Add", lang),
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 880.dp),
                        ) {
                            AnimatedContent(
                                targetState = state.view,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                                label = "main_tab_switch",
                            ) { tab ->
                                when (tab) {
                                    AppTab.HOME -> HomeScreen(
                                        state = state,
                                        onClearExamples = vm::clearExampleData,
                                        onStartTimer = { vm.selectTab(AppTab.TIMER) },
                                        onFocusSubject = vm::focusOnSubjectFromHome,
                                        onOpenSheet = vm::openSheet,
                                        onNavigateTab = vm::selectTab,
                                        onToggleTask = vm::toggleTaskDone,
                                        onDeleteTask = vm::deleteTask,
                                        onToggleSubjectDaily = vm::toggleSubjectDailySplit,
                                        onToggleReviewDaily = vm::toggleReviewDaily,
                                        onCleanReviews = vm::cleanCompletedReviews,
                                        onToggleCheckItem = vm::toggleChecklistItem,
                                        onDeleteCheckItem = vm::deleteChecklistItem,
                                        onCleanCheckItems = vm::cleanChecklistItems,
                                    )

                                    AppTab.TASKS -> TasksScreen(
                                        state = state,
                                        taskFilter = taskFilter,
                                        searchQuery = searchQuery,
                                        filterSubj = filterSubj,
                                        filterPri = filterPri,
                                        filterDate = filterDate,
                                        weekOffset = weekOffset,
                                        onSetViewMode = vm::setTaskViewMode,
                                        onSetFilter = vm::setTaskFilter,
                                        onToggleSortMode = vm::toggleTaskSortMode,
                                        onSearchChange = vm::setSearchQuery,
                                        onFilterSubjChange = vm::setFilterSubj,
                                        onFilterPriChange = vm::setFilterPri,
                                        onFilterDateChange = vm::setFilterDate,
                                        onClearFilters = vm::clearFilters,
                                        onMoveWeekOffset = vm::moveWeekOffset,
                                        onToggleTask = vm::toggleTaskDone,
                                        onDeleteTask = vm::deleteTask,
                                        onMoveManual = vm::moveTaskManual,
                                        onOpenSheet = vm::openSheet,
                                    )

                                    AppTab.SUBJECTS -> SubjectsScreen(
                                        state = state,
                                        confirmDeleteSubjectId = confirmDelSubjId,
                                        onOpenSheet = vm::openSheet,
                                        onTogglePartDone = vm::toggleSubjectPartDone,
                                        onDeleteSubject = vm::requestOrConfirmDeleteSubject,
                                        onAddDefault6thSubjects = vm::addDefault6thSubjects,
                                        onToggleReviewDaily = vm::toggleReviewDaily,
                                        onCleanReviews = vm::cleanCompletedReviews,
                                        onToggleCheckItem = vm::toggleChecklistItem,
                                        onDeleteCheckItem = vm::deleteChecklistItem,
                                        onCleanCheckItems = vm::cleanChecklistItems,
                                    )

                                    AppTab.TIMER -> TimerScreen(
                                        state = state,
                                        timer = timer,
                                        onSetMode = vm::setTimerMode,
                                        onSelectSubject = vm::selectTimerSubject,
                                        onToggleRunning = vm::toggleTimerRunning,
                                        onReset = vm::resetTimer,
                                        onOpenSheet = vm::openSheet,
                                    )

                                    AppTab.HUB -> HubScreen(
                                        state = state,
                                        hubTab = hubTab,
                                        scheduleDayView = schedDayView,
                                        selectedSchedDay = selectedSchedDay,
                                        friendsState = friends,
                                        friendNameFn = vm::friendDisplayName,
                                        onSelectHubTab = vm::selectHubTab,
                                        onToggleScheduleView = vm::toggleScheduleView,
                                        onSelectScheduleDay = vm::selectScheduleDay,
                                        onChangePeriods = vm::changeSchedulePeriods,
                                        onOpenSheet = vm::openSheet,
                                        onRefreshFriends = vm::refreshFriends,
                                        onSendFriendRequest = vm::sendFriendRequest,
                                        onRespondRequest = vm::respondFriendRequest,
                                        onCancelOutgoing = vm::cancelOutgoingRequest,
                                        onRemoveFriend = vm::removeFriend,
                                        onOpenChat = vm::openFriendChat,
                                        onSetNowKind = vm::setNowKind,
                                        onSetNowDuration = vm::setNowDuration,
                                        onSelectNowSubject = vm::selectNowSubjectName,
                                        onSetNowCustomName = vm::setNowCustomName,
                                        onStartNowStatus = vm::startNowStatus,
                                        onEndNowStatus = vm::endNowStatus,
                                        onShowToast = vm::showToast,
                                    )

                                    AppTab.PROGRESS -> ProgressScreen(
                                        state = state,
                                        monthOffset = monthOffset,
                                        selectedDayKey = selectedProgDay,
                                        onSetRange = vm::setProgressRange,
                                        onMoveMonthOffset = vm::moveMonthOffset,
                                        onSelectDay = vm::selectProgressDay,
                                    )
                                }
                            }

                            UndoToastBanner(
                                toast = toast,
                                lang = lang,
                                onUndo = vm::triggerUndo,
                                onDismiss = vm::dismissToast,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 14.dp),
                            )
                        }
                    }
                }
            }

            // Active Modal Bottom Sheet Router
            if (activeSheet != ActiveSheet.None) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = vm::closeSheet,
                    sheetState = sheetState,
                    containerColor = mz.surface,
                    contentColor = mz.ink,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ) {
                    when (val sh = activeSheet) {
                        ActiveSheet.Welcome -> WelcomeSheetContent(
                            state = state,
                            onSave = vm::saveWelcome,
                            onDismiss = vm::closeSheet,
                        )
                        is ActiveSheet.TaskEditor -> TaskEditorSheetContent(
                            state = state,
                            taskId = sh.taskId,
                            presetDue = sh.presetDue,
                            onSave = vm::saveTask,
                            onDelete = vm::deleteTask,
                            onClose = vm::closeSheet,
                        )
                        ActiveSheet.SubjectTypePicker -> SubjectTypePickerSheetContent(
                            lang = lang,
                            onPick = vm::openSheet,
                        )
                        is ActiveSheet.SubjectEditor -> SubjectEditorSheetContent(
                            state = state,
                            subjectId = sh.subjectId,
                            onSave = vm::saveSubject,
                        )
                        is ActiveSheet.ReviewEditor -> ReviewEditorSheetContent(
                            state = state,
                            kind = sh.kind,
                            reviewId = sh.reviewId,
                            onSave = vm::saveReview,
                            onDelete = vm::deleteReview,
                        )
                        is ActiveSheet.TopicsEditor -> TopicsEditorSheetContent(
                            kind = sh.kind,
                            lang = lang,
                            onAddTopics = vm::addChecklistTopics,
                        )
                        ActiveSheet.PlanEditor -> PlanEditorSheetContent(
                            state = state,
                            onSave = vm::savePlan,
                            onClear = vm::clearPlan,
                        )
                        ActiveSheet.ManualLog -> ManualLogSheetContent(
                            state = state,
                            defaultSubjId = timer.subjId,
                            onSave = vm::logManualStudyTime,
                        )
                        ActiveSheet.TimerSettingsSheet -> TimerSettingsSheetContent(
                            state = state,
                            onSave = vm::updateTimerSettings,
                        )
                        is ActiveSheet.ScheduleCellEditor -> ScheduleCellSheetContent(
                            state = state,
                            cellKey = sh.cellKey,
                            onSave = vm::saveScheduleCell,
                        )
                        ActiveSheet.ScheduleDaysPicker -> ScheduleDaysSheetContent(
                            state = state,
                            onToggleDay = vm::toggleScheduleVisibleDay,
                            onClose = vm::closeSheet,
                        )
                        is ActiveSheet.NoteEditor -> NoteEditorSheetContent(
                            state = state,
                            noteId = sh.noteId,
                            onSave = vm::saveNote,
                            onDelete = vm::deleteNote,
                        )
                        ActiveSheet.StreakDetails -> StreakDetailsSheetContent(
                            state = state,
                            streakFriendsAlert = streakFriendsAlert,
                            onToggleStreakAlert = vm::setStreakFriendsAlert,
                            onShowToast = vm::showToast,
                        )
                        ActiveSheet.FriendChat -> FriendChatSheetContent(
                            chat = chat,
                            lang = lang,
                            arDigits = state.arDigits,
                            onToggleNickname = vm::toggleChatNicknameBar,
                            onSaveNickname = vm::saveChatNickname,
                            onBlockFriend = vm::blockChatFriend,
                            onSendMessage = vm::sendChatMessage,
                        )
                        ActiveSheet.ProfileSettings -> ProfileSettingsSheetContent(
                            state = state,
                            userEmail = auth.userEmail,
                            fontScaleIdx = fontScaleIdx,
                            onPickPhoto = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onDeletePhoto = { vm.updateProfile(state.profile.name, "") },
                            onUpdateProfileName = { newName ->
                                vm.updateProfile(newName, state.profile.photo)
                                vm.showToast(vm.t("تم حفظ الاسم", "Name saved"))
                            },
                            onChangeFontScale = vm::setFontScaleIndex,
                            onSetLayout = vm::setLayoutOverride,
                            onOpenThemePicker = { vm.openSheet(ActiveSheet.ThemePicker) },
                            onSetLanguage = vm::setLanguage,
                            onSetArabicDigits = vm::setArabicDigits,
                            onSetLockDone = vm::setLockDone,
                            onUpdateDailyReminder = vm::updateDailyReminder,
                            onExportFile = vm::exportFile,
                            onImportBackup = {
                                backupImporter.launch(arrayOf("application/json", "*/*"))
                            },
                            onChangePassword = vm::changePassword,
                            onLogout = vm::logout,
                            onShowToast = vm::showToast,
                        )
                        ActiveSheet.ThemePicker -> ThemePickerSheetContent(
                            state = state,
                            onSelectTheme = vm::setTheme,
                        )
                        ActiveSheet.None -> Unit
                    }
                }
            }

            MzConfirmDialog(
                state = confirmDialog,
                onDismiss = vm::dismissConfirm,
            )
        }
    }
}
