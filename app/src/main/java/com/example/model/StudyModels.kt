package com.example.model

enum class AppTab(val id: String) {
    HOME("home"),
    TASKS("tasks"),
    SUBJECTS("subjects"),
    TIMER("timer"),
    HUB("hub"),
    PROGRESS("progress");

    companion object {
        fun fromId(id: String?): AppTab = entries.firstOrNull { it.id == id } ?: HOME
    }
}

enum class TaskFilter(val id: String) { OPEN("open"), DONE("done"), ALL("all") }
enum class TaskViewMode(val id: String) { LIST("list"), WEEK("week") }
enum class SortMode(val id: String) { AUTO("auto"), MANUAL("manual") }
enum class ProgressRange(val id: String) { WEEK("week"), MONTH("month") }
enum class HubSubTab(val id: String) { SCHED("sched"), FRIENDS("fr"), NOTES("notes") }
enum class TimerMode(val id: String) { FOCUS("focus"), BREAK("break") }

enum class AuthMode { WELCOME, LOGIN, REGISTER, VERIFY, FORGOT, RESET }

data class SubjectPart(
    val id: String,
    val name: String,
    val hours: Double = 0.0,
    val lec: Int = 0,
    val done: Boolean = false,
)

data class DayAmountMark(
    val date: String,
    val amt: Double,
    val logId: String = "",
)

data class Subject(
    val id: String,
    val name: String,
    val color: String = "#2D5BFF",
    val target: Double = 3.0,
    val parts: List<SubjectPart> = emptyList(),
    val doneH: Double = 0.0,
    val day: DayAmountMark? = null,
    val ex: Boolean = false,
)

data class StudyTask(
    val id: String,
    val title: String,
    val subj: String = "",
    val due: String = "",
    val pri: String = "med", // high | med | low
    val rem: String = "",
    val remLast: String = "",
    val done: Boolean = false,
    val ex: Boolean = false,
)

data class StudyLog(
    val id: String,
    val subj: String = "",
    val min: Int,
    val date: String,
    val ex: Boolean = false,
)

data class ReviewItem(
    val id: String,
    val name: String,
    val hours: Double,
    val days: Int,
    val start: String,
    val chk: String = "",
    val dd: Set<String> = emptySet(),
    val ext: Int = 0,
    val doneH: Double = 0.0,
    val day: DayAmountMark? = null,
    val kind: String = "rev", // rev | misc | hw
    val color: String = "#2D5BFF",
)

data class CheckListItem(
    val id: String,
    val kind: String = "misc", // misc | hw
    val name: String,
    val done: Boolean = false,
)

data class StudyPlan(
    val days: Int,
    val start: String,
    val name: String = "",
)

data class WeeklySchedule(
    val n: Int = 7,
    val sat: Boolean = false,
    val days: List<Int> = listOf(0, 1, 2, 3, 4),
    val cells: Map<String, String> = emptyMap(),
)

data class StudyNote(
    val id: String,
    val title: String = "",
    val txt: String,
    val color: String = "#E8A317",
    val pin: Boolean = false,
    val ts: Long = 0L,
)

data class TimerSettings(
    val focus: Int = 25,
    val brk: Int = 5,
    val goal: Int = 120,
)

data class DailyReminder(
    val on: Boolean = false,
    val time: String = "18:00",
    val subj: String = "",
    val last: String = "",
)

data class UserProfile(
    val name: String = "",
    val photo: String = "", // Base64 JPEG data URL or empty
)

data class AppState(
    val v: Int = 3,
    val lang: String = "ar",
    val view: AppTab = AppTab.HOME,
    val sched: WeeklySchedule = WeeklySchedule(),
    val act: Set<String> = emptySet(),
    val lockDone: Boolean = true,
    val notes: List<StudyNote> = emptyList(),
    val fid: String = "",
    val nowT: String = "",
    val nowAt: Long = 0L,
    val settings: TimerSettings = TimerSettings(),
    val subjects: List<Subject> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val logs: List<StudyLog> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
    val lists: List<CheckListItem> = emptyList(),
    val plan: StudyPlan? = null,
    val profile: UserProfile = UserProfile(),
    val remind: DailyReminder = DailyReminder(),
    val theme: String = "custom",
    val palette: String = "white-yellow",
    val layout: String = "auto",
    val taskSort: SortMode = SortMode.AUTO,
    val taskView: TaskViewMode = TaskViewMode.LIST,
    val progRange: ProgressRange = ProgressRange.WEEK,
    val arDigits: Boolean = false,
    val onboarded: Boolean = false,
)

data class TimerRuntime(
    val mode: TimerMode = TimerMode.FOCUS,
    val running: Boolean = false,
    val endAt: Long = 0L,
    val leftSec: Int = 25 * 60,
    val leftMs: Long? = null,
    val totalSec: Int = 25 * 60,
    val subjId: String = "",
)

data class FriendItem(
    val code: String,
    val name: String = "",
    val photo: String = "",
    val days: Set<String> = emptySet(),
    val nowText: String = "",
    val nowAtMs: Long = 0L,
    val unread: Int = 0,
    val lastMsg: String = "",
)

data class FriendRequest(
    val code: String,
    val name: String = "",
    val photo: String = "",
)

data class ChatMessage(
    val id: Long,
    val mine: Boolean,
    val kind: String = "text",
    val text: String,
    val at: String = "",
)

data class FriendsState(
    val list: List<FriendItem> = emptyList(),
    val incoming: List<FriendRequest> = emptyList(),
    val outgoing: List<FriendRequest> = emptyList(),
    val err: String = "", // "" | "setup" | "login" | "net"
    val busy: Boolean = false,
    val msg: String = "",
    val ok: Boolean = false,
    val loadedOnce: Boolean = false,
    val nowKind: String = "lesson",
    val nowDuration: Int = 30,
    val nowSelectedName: String = "",
    val nowCustomName: String = "",
)

data class ChatState(
    val code: String = "",
    val name: String = "",
    val photo: String = "",
    val msgs: List<ChatMessage> = emptyList(),
    val readUpto: Long = 0L,
    val busy: Boolean = false,
    val err: String = "",
    val nicknameInputOpen: Boolean = false,
)

sealed class ActiveSheet {
    data object None : ActiveSheet()
    data object Welcome : ActiveSheet()
    data class TaskEditor(val taskId: String? = null, val presetDue: String? = null) : ActiveSheet()
    data object SubjectTypePicker : ActiveSheet()
    data class SubjectEditor(val subjectId: String? = null) : ActiveSheet()
    data class ReviewEditor(val kind: String = "rev", val reviewId: String? = null) : ActiveSheet()
    data class TopicsEditor(val kind: String = "misc") : ActiveSheet()
    data object PlanEditor : ActiveSheet()
    data object ManualLog : ActiveSheet()
    data object TimerSettingsSheet : ActiveSheet()
    data object ProfileSettings : ActiveSheet()
    data object ThemePicker : ActiveSheet()
    data object StreakDetails : ActiveSheet()
    data class ScheduleCellEditor(val cellKey: String) : ActiveSheet()
    data object ScheduleDaysPicker : ActiveSheet()
    data class NoteEditor(val noteId: String? = null) : ActiveSheet()
    data object FriendChat : ActiveSheet()
}

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val showWelcomeIntro: Boolean = true,
    val sheetOpen: Boolean = false,
    val mode: AuthMode = AuthMode.LOGIN,
    val emailInput: String = "",
    val message: String = "",
    val isSuccessMessage: Boolean = false,
    val isLoading: Boolean = false,
    val cooldownUntilMs: Long = 0L,
    val pendingPassword: String = "",
)

data class ToastMessage(
    val text: String,
    val canUndo: Boolean = false,
    val id: Long = System.currentTimeMillis(),
)

data class ConfirmDialogState(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val cancelLabel: String,
    val isDanger: Boolean = false,
    val onConfirm: () -> Unit,
)
