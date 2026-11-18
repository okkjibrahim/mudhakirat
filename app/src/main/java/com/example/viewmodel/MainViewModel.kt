package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmScheduler
import com.example.FcmService
import com.example.data.ExportUtils
import com.example.data.JsonSerializer
import com.example.data.LocalRepository
import com.example.data.LocalUserRecord
import com.example.data.SupabaseApi
import com.example.data.SupabaseException
import com.example.model.*
import com.example.ui.theme.FontScaleSteps
import com.example.ui.theme.SubjectColors
import com.example.ui.theme.colorToHex
import com.example.util.DateUtils
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appCtx: Context = application.applicationContext
    val repo = LocalRepository(appCtx)

    private val _state = MutableStateFlow(JsonSerializer.seed(repo.getLanguage()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _auth = MutableStateFlow(AuthUiState())
    val auth: StateFlow<AuthUiState> = _auth.asStateFlow()

    private val _timer = MutableStateFlow(TimerRuntime())
    val timer: StateFlow<TimerRuntime> = _timer.asStateFlow()

    private val _friends = MutableStateFlow(FriendsState())
    val friends: StateFlow<FriendsState> = _friends.asStateFlow()

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet>(ActiveSheet.None)
    val activeSheet: StateFlow<ActiveSheet> = _activeSheet.asStateFlow()

    private val _fontScaleIndex = MutableStateFlow(repo.getFontScaleIndex())
    val fontScaleIndex: StateFlow<Int> = _fontScaleIndex.asStateFlow()

    val currentFontScale: Float
        get() = FontScaleSteps[_fontScaleIndex.value.coerceIn(0, FontScaleSteps.lastIndex)]

    private val _scheduleDayView = MutableStateFlow(repo.isScheduleDayView())
    val scheduleDayView: StateFlow<Boolean> = _scheduleDayView.asStateFlow()

    private val _selectedSchedDay = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1)
    val selectedSchedDay: StateFlow<Int> = _selectedSchedDay.asStateFlow()

    private val _hubTab = MutableStateFlow(HubSubTab.SCHED)
    val hubTab: StateFlow<HubSubTab> = _hubTab.asStateFlow()

    private val _taskFilter = MutableStateFlow(TaskFilter.OPEN)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterSubj = MutableStateFlow("")
    val filterSubj: StateFlow<String> = _filterSubj.asStateFlow()

    private val _filterPri = MutableStateFlow("")
    val filterPri: StateFlow<String> = _filterPri.asStateFlow()

    private val _filterDate = MutableStateFlow("")
    val filterDate: StateFlow<String> = _filterDate.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    private val _monthOffset = MutableStateFlow(0)
    val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

    private val _selectedProgressDay = MutableStateFlow(DateUtils.todayKey())
    val selectedProgressDay: StateFlow<String> = _selectedProgressDay.asStateFlow()

    private val _confirmDeleteSubjectId = MutableStateFlow("")
    val confirmDeleteSubjectId: StateFlow<String> = _confirmDeleteSubjectId.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _confirmDialog = MutableStateFlow<ConfirmDialogState?>(null)
    val confirmDialog: StateFlow<ConfirmDialogState?> = _confirmDialog.asStateFlow()

    private val _clockText = MutableStateFlow("")
    val clockText: StateFlow<String> = _clockText.asStateFlow()

    private val _headerDateText = MutableStateFlow("")
    val headerDateText: StateFlow<String> = _headerDateText.asStateFlow()

    private val _greetingIdx = MutableStateFlow(DateUtils.greetingIndex())
    val greetingIdx: StateFlow<Int> = _greetingIdx.asStateFlow()

    private val _unreadTotal = MutableStateFlow(0)
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    private val _streakFriendsAlert = MutableStateFlow(repo.getUserPref("", "stkfn", "1") != "0")
    val streakFriendsAlert: StateFlow<Boolean> = _streakFriendsAlert.asStateFlow()

    private var undoAction: (() -> Unit)? = null
    private var saveJob: Job? = null
    private var pushJob: Job? = null
    private var toastJob: Job? = null
    private var lastSyncedSig = ""
    private var lastDayChecked = DateUtils.todayKey()
    private var autoStatusTimestamp = ""
    private var selfNotifiedAt = ""
    private val seenFriendEnds = mutableMapOf<String, Pair<Long, Long>>() // code -> (atMs, endMs)
    private val seenFriendMsgs = mutableMapOf<String, Int>()
    private var seenReqCodes: Set<String>? = null

    init {
        val savedTh = repo.getStoredTheme()
        _state.update { it.copy(theme = savedTh.first, palette = savedTh.second) }
        restoreSessionOnStart()
        startClockAndTickerLoops()
    }

    private fun restoreSessionOnStart() {
        val sess = repo.loadSession()
        val users = repo.getUsers()
        if (sess.isNotBlank() && users.containsKey(sess)) {
            enterApp(sess)
        } else {
            _auth.update { it.copy(isLoggedIn = false, showWelcomeIntro = true) }
        }
    }

    fun dismissWelcomeSplash() {
        _auth.update { it.copy(showWelcomeIntro = false) }
    }

    private fun startClockAndTickerLoops() {
        viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                _clockText.value = DateUtils.formatClock12(s.lang, s.arDigits)
                _headerDateText.value = DateUtils.formatHeaderDate(s.lang, s.arDigits)
                _greetingIdx.value = DateUtils.greetingIndex()

                if (_auth.value.isLoggedIn) {
                    tickTimer()
                    checkDayRollover()
                    checkTaskAndDailyReminders()
                    checkSelfStatusEnd()
                }
                delay(1000L)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                if (_auth.value.isLoggedIn && SupabaseApi.isConfigured()) {
                    syncFcmTokenInternal()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                if (_auth.value.isLoggedIn && SupabaseApi.isConfigured()) {
                    pollFriendsQuietly()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                if (_auth.value.isLoggedIn && _activeSheet.value == ActiveSheet.FriendChat && _chat.value.code.isNotBlank()) {
                    loadChatMessages()
                }
            }
        }
    }

    // ---------- Helper Localization ----------
    fun t(ar: String, en: String): String = Strings.pk(ar, en, _state.value.lang)
    fun n(s: String): String = DateUtils.applyDigits(s, _state.value.lang, _state.value.arDigits)

    // ---------- Toast / Confirm ----------
    fun showToast(msg: String) {
        undoAction = null
        _toast.value = ToastMessage(msg, canUndo = false)
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(5000L)
            _toast.value = null
        }
    }

    fun showUndoToast(msg: String, onUndo: () -> Unit) {
        undoAction = onUndo
        _toast.value = ToastMessage(msg, canUndo = true)
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(7000L)
            _toast.value = null
            undoAction = null
        }
    }

    fun triggerUndo() {
        val fn = undoAction
        undoAction = null
        _toast.value = null
        fn?.invoke()
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun askConfirm(
        title: String,
        message: String,
        confirmLabel: String = t("تأكيد", "Confirm"),
        cancelLabel: String = t("إلغاء", "Cancel"),
        isDanger: Boolean = false,
        onConfirm: () -> Unit,
    ) {
        _confirmDialog.value = ConfirmDialogState(title, message, confirmLabel, cancelLabel, isDanger, onConfirm)
    }

    fun dismissConfirm() {
        _confirmDialog.value = null
    }

    // ---------- State Update & Persistence ----------
    private fun updateState(
        markAct: Boolean = false,
        transform: (AppState) -> AppState,
    ) {
        val email = _auth.value.userEmail
        val prev = _state.value
        var next = transform(prev)
        if (markAct) {
            val td = DateUtils.todayKey()
            if (!next.act.contains(td)) {
                val updated = (next.act + td).sorted()
                val trimmed = if (updated.size > 400) updated.takeLast(400).toSet() else updated.toSet()
                next = next.copy(act = trimmed)
            }
        }
        _state.value = next
        if (_auth.value.isLoggedIn && email.isNotBlank()) {
            scheduleSave(email, next)
            FeedbackHelper.syncStreakWarningAlarm(appCtx, next, true)
        }
    }

    private fun scheduleSave(email: String, stateToSave: AppState) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(200L)
            repo.saveAppState(email, stateToSave)
            scheduleCloudPush(email, stateToSave)
        }
    }

    private fun scheduleCloudPush(email: String, stateToSave: AppState) {
        if (!SupabaseApi.isConfigured() || !_auth.value.isLoggedIn) return
        val ts = System.currentTimeMillis()
        repo.setLocalTimestamp(email, ts)
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(2500L)
            syncProfileQuietly()
            val serverTs = SupabaseApi.pushUserData(repo, stateToSave, ts)
            if (serverTs != null && serverTs > ts && _auth.value.userEmail == email) {
                val pulled = SupabaseApi.pullUserData(repo, email)
                if (pulled) {
                    val fresh = repo.loadAppState(email)
                    if (fresh != null) {
                        _state.value = fresh.copy(lang = _state.value.lang)
                    }
                }
            }
        }
    }

    // ---------- Authentication ----------
    fun openAuthSheet(mode: AuthMode) {
        _auth.update { it.copy(sheetOpen = true, mode = mode, message = "", isSuccessMessage = false) }
    }

    fun closeAuthSheet() {
        _auth.update { it.copy(sheetOpen = false, message = "") }
    }

    fun setAuthMode(mode: AuthMode) {
        _auth.update { it.copy(mode = mode, message = "", isSuccessMessage = false) }
    }

    fun setAuthEmailInput(email: String) {
        _auth.update { it.copy(emailInput = email) }
    }

    fun toggleAuthLanguage() {
        val next = if (_state.value.lang == "ar") "en" else "ar"
        repo.setLanguage(next)
        _state.update { it.copy(lang = next) }
    }

    fun submitAuth(emailRaw: String, password: String) {
        val email = emailRaw.trim().lowercase()
        if (!Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(email)) {
            _auth.update { it.copy(message = t("أدخل بريدًا إلكترونيًا صحيحًا", "Enter a valid email"), isSuccessMessage = false) }
            return
        }
        if (password.length < 6) {
            _auth.update { it.copy(message = t("كلمة المرور 6 أحرف على الأقل", "Password must be at least 6 characters"), isSuccessMessage = false) }
            return
        }
        val isReg = _auth.value.mode == AuthMode.REGISTER
        _auth.update { it.copy(isLoading = true, message = "", emailInput = email) }

        viewModelScope.launch {
            if (SupabaseApi.isConfigured()) {
                try {
                    val res = if (isReg) SupabaseApi.signUp(email, password) else SupabaseApi.signIn(email, password)
                    val at = res.optString("access_token")
                    val identities = res.optJSONArray("identities")
                    if (isReg && identities != null && identities.length() == 0) {
                        _auth.update { it.copy(isLoading = false, message = t("هذا البريد مسجّل مسبقًا، سجّل الدخول", "This email is already registered, log in")) }
                        return@launch
                    }
                    if (at.isBlank()) {
                        _auth.update {
                            it.copy(
                                isLoading = false,
                                mode = AuthMode.VERIFY,
                                pendingPassword = password,
                                cooldownUntilMs = System.currentTimeMillis() + 60_000L,
                                isSuccessMessage = true,
                                message = t("أرسلنا رمز التحقق إلى بريدك", "We sent a verification code to your email"),
                            )
                        }
                        return@launch
                    }
                    val rt = res.optString("refresh_token")
                    if (rt.isNotBlank()) repo.saveSbRefreshToken(rt)
                    saveLocalUserAndEnter(email, password)
                    return@launch
                } catch (e: Exception) {
                    val m = e.message ?: ""
                    if (e !is SupabaseException) {
                        // Offline network fallback to local auth
                        if (tryLocalAuthFallback(email, password, isReg)) return@launch
                    }
                    val errCode = (e as? SupabaseException)?.errorCode ?: ""
                    val statusCode = (e as? SupabaseException)?.code ?: 0
                    val mapped = mapSupabaseAuthErr(statusCode, errCode, m)
                    if (isReg && mapped != null) {
                        _auth.update { it.copy(isLoading = false, message = mapped) }
                        return@launch
                    }
                    if (isReg && m.contains("already", ignoreCase = true)) {
                        _auth.update { it.copy(isLoading = false, message = t("هذا البريد مسجّل مسبقًا، سجّل الدخول", "This email is already registered, log in")) }
                        return@launch
                    }
                    if (!isReg && m.contains("not confirmed", ignoreCase = true)) {
                        try { SupabaseApi.resendSignup(email) } catch (_: Exception) {}
                        _auth.update {
                            it.copy(
                                isLoading = false,
                                mode = AuthMode.VERIFY,
                                pendingPassword = password,
                                isSuccessMessage = true,
                                message = t("أرسلنا رمز التحقق إلى بريدك", "We sent a verification code to your email"),
                            )
                        }
                        return@launch
                    }
                    val users = repo.getUsers()
                    val existing = users[email]
                    if (!isReg && existing != null) {
                        val h = withContext(Dispatchers.Default) { repo.hashPassword(password, existing.salt) }
                        if (h == existing.hash) {
                            // Local match: if "invalid" try signing up online silently, else enter locally
                            if (m.contains("invalid", ignoreCase = true)) {
                                try {
                                    val r2 = SupabaseApi.signUp(email, password)
                                    if (r2.optString("access_token").isNotBlank()) {
                                        val rt = r2.optString("refresh_token")
                                        if (rt.isNotBlank()) repo.saveSbRefreshToken(rt)
                                        saveLocalUserAndEnter(email, password)
                                        return@launch
                                    }
                                } catch (_: Exception) {}
                            }
                            enterApp(email)
                            return@launch
                        }
                    }
                    _auth.update {
                        it.copy(
                            isLoading = false,
                            message = t("البريد أو كلمة المرور غير صحيحة", "Wrong email or password") + if (m.isNotBlank()) " ($m)" else "",
                        )
                    }
                    return@launch
                }
            } else {
                tryLocalAuthFallback(email, password, isReg)
            }
        }
    }

    private suspend fun tryLocalAuthFallback(email: String, password: String, isReg: Boolean): Boolean {
        val users = repo.getUsers()
        val u = users[email]
        if (isReg) {
            if (u != null) {
                _auth.update { it.copy(isLoading = false, message = t("هذا البريد مسجّل مسبقًا، سجّل الدخول", "This email is already registered, log in")) }
                return true
            }
            val salt = repo.newSalt()
            val hash = withContext(Dispatchers.Default) { repo.hashPassword(password, salt) }
            users[email] = LocalUserRecord(salt, hash)
            repo.putUsers(users)
            if (users.size == 1) repo.adoptLegacyAnonymousDataIfAny(email)
            enterApp(email)
            return true
        } else {
            if (u == null) {
                _auth.update { it.copy(isLoading = false, message = t("البريد أو كلمة المرور غير صحيحة", "Wrong email or password")) }
                return true
            }
            val hash = withContext(Dispatchers.Default) { repo.hashPassword(password, u.salt) }
            if (hash != u.hash) {
                _auth.update { it.copy(isLoading = false, message = t("البريد أو كلمة المرور غير صحيحة", "Wrong email or password")) }
                return true
            }
            enterApp(email)
            return true
        }
    }

    private suspend fun saveLocalUserAndEnter(email: String, password: String) {
        val users = repo.getUsers()
        val salt = repo.newSalt()
        val hash = withContext(Dispatchers.Default) { repo.hashPassword(password, salt) }
        users[email] = LocalUserRecord(salt, hash)
        repo.putUsers(users)
        SupabaseApi.pullUserData(repo, email)
        enterApp(email)
        scheduleCloudPush(email, _state.value)
    }

    fun submitVerifyCode(codeRaw: String) {
        val code = codeRaw.replace(Regex("""\s+"""), "")
        if (!Regex("""^\d{6,8}$""").matches(code)) {
            _auth.update { it.copy(message = t("اكتب الرمز المكوّن من 6 أرقام", "Enter the 6-digit code"), isSuccessMessage = false) }
            return
        }
        val email = _auth.value.emailInput
        val pw = _auth.value.pendingPassword
        _auth.update { it.copy(isLoading = true, message = "") }
        viewModelScope.launch {
            try {
                val res = SupabaseApi.verifyOtp("signup", email, code)
                val rt = res.optString("refresh_token")
                if (rt.isNotBlank()) repo.saveSbRefreshToken(rt)
                val users = repo.getUsers()
                val salt = repo.newSalt()
                val hash = withContext(Dispatchers.Default) { repo.hashPassword(pw.ifEmpty { code }, salt) }
                if (pw.isNotEmpty()) {
                    users[email] = LocalUserRecord(salt, hash)
                    repo.putUsers(users)
                }
                SupabaseApi.pullUserData(repo, email)
                enterApp(email)
                scheduleCloudPush(email, _state.value)
            } catch (_: Exception) {
                _auth.update {
                    it.copy(
                        isLoading = false,
                        isSuccessMessage = false,
                        message = t("الرمز غير صحيح أو منتهي، اطلب رمزًا جديدًا", "Wrong or expired code, request a new one"),
                    )
                }
            }
        }
    }

    fun skipVerificationLocalMode() {
        val email = _auth.value.emailInput.ifBlank { "student@mudhakirati.app" }
        val pw = _auth.value.pendingPassword.ifBlank { "123456" }
        viewModelScope.launch {
            val users = repo.getUsers()
            val salt = repo.newSalt()
            val hash = withContext(Dispatchers.Default) { repo.hashPassword(pw, salt) }
            users[email] = LocalUserRecord(salt, hash)
            repo.putUsers(users)
            enterApp(email)
            showToast(t("تم الدخول محليًا. المزامنة والأصدقاء تحتاج حسابًا سحابيًا مؤكّدًا.", "Local mode enabled. Sync and friends require a verified cloud account."))
        }
    }

    fun resendVerification(isRecovery: Boolean = false) {
        val now = System.currentTimeMillis()
        val remSec = ceil((_auth.value.cooldownUntilMs - now) / 1000.0).toInt()
        if (remSec > 0) {
            _auth.update { it.copy(isSuccessMessage = false, message = t("انتظر $remSec ثانية قبل إعادة الإرسال", "Wait ${remSec}s before resending")) }
            return
        }
        val email = _auth.value.emailInput
        viewModelScope.launch {
            try {
                if (isRecovery) SupabaseApi.recover(email) else SupabaseApi.resendSignup(email)
                _auth.update {
                    it.copy(
                        cooldownUntilMs = System.currentTimeMillis() + 60_000L,
                        isSuccessMessage = true,
                        message = t("أعدنا إرسال الرمز، تحقق من بريدك (وملف السبام)", "Code sent again, check your email (and spam)"),
                    )
                }
            } catch (e: Exception) {
                val se = e as? SupabaseException
                val mapped = mapSupabaseAuthErr(se?.code ?: 0, se?.errorCode ?: "", e.message ?: "")
                _auth.update {
                    it.copy(
                        isSuccessMessage = false,
                        message = mapped ?: t("تعذّر الإرسال، حاول بعد قليل", "Could not send, try again shortly"),
                    )
                }
            }
        }
    }

    fun submitForgotPassword(emailRaw: String) {
        val email = emailRaw.trim().lowercase()
        if (!Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(email)) {
            _auth.update { it.copy(message = t("أدخل بريدًا إلكترونيًا صحيحًا", "Enter a valid email"), isSuccessMessage = false) }
            return
        }
        _auth.update { it.copy(isLoading = true, emailInput = email, message = "") }
        viewModelScope.launch {
            try {
                SupabaseApi.recover(email)
                _auth.update {
                    it.copy(
                        isLoading = false,
                        mode = AuthMode.RESET,
                        cooldownUntilMs = System.currentTimeMillis() + 60_000L,
                        isSuccessMessage = true,
                        message = t("إذا كان البريد مسجّلًا، أرسلنا لك رمزًا لإعادة التعيين", "If this email is registered, we sent you a code to reset it"),
                    )
                }
            } catch (e: Exception) {
                val se = e as? SupabaseException
                val mapped = mapSupabaseAuthErr(se?.code ?: 0, se?.errorCode ?: "", e.message ?: "")
                _auth.update {
                    it.copy(
                        isLoading = false,
                        isSuccessMessage = false,
                        message = mapped ?: t("تعذّر الإرسال، حاول لاحقًا", "Could not send, try again later"),
                    )
                }
            }
        }
    }

    fun submitResetPassword(codeRaw: String, newPw: String, confirmPw: String) {
        val code = codeRaw.replace(Regex("""\s+"""), "")
        if (!Regex("""^\d{6,8}$""").matches(code)) {
            _auth.update { it.copy(message = t("اكتب الرمز المكوّن من 6 أرقام", "Enter the 6-digit code"), isSuccessMessage = false) }
            return
        }
        if (newPw.length < 6) {
            _auth.update { it.copy(message = t("كلمة المرور 6 أحرف على الأقل", "Password must be at least 6 characters"), isSuccessMessage = false) }
            return
        }
        if (newPw != confirmPw) {
            _auth.update { it.copy(message = t("كلمتا السر غير متطابقتين", "Passwords do not match"), isSuccessMessage = false) }
            return
        }
        val email = _auth.value.emailInput
        _auth.update { it.copy(isLoading = true, message = "") }
        viewModelScope.launch {
            try {
                val res = SupabaseApi.verifyOtp("recovery", email, code)
                val tok = res.optString("access_token")
                if (tok.isBlank()) throw Exception("no_token")
                SupabaseApi.updatePassword(tok, newPw)
                val users = repo.getUsers()
                val salt = repo.newSalt()
                val hash = withContext(Dispatchers.Default) { repo.hashPassword(newPw, salt) }
                users[email] = LocalUserRecord(salt, hash)
                repo.putUsers(users)
                _auth.update {
                    it.copy(
                        isLoading = false,
                        mode = AuthMode.LOGIN,
                        cooldownUntilMs = 0L,
                        isSuccessMessage = true,
                        message = t("تم تغيير كلمة السر، سجّل الدخول الآن", "Password changed, log in now"),
                    )
                }
            } catch (e: Exception) {
                val m = e.message ?: ""
                val msg = when {
                    m.contains("different", true) || m.contains("same", true) -> t("اختر كلمة سر مختلفة عن القديمة", "Choose a password different from the old one")
                    m.contains("weak", true) || m.contains("short", true) -> t("كلمة المرور 6 أحرف على الأقل", "Password must be at least 6 characters")
                    else -> t("الرمز غير صحيح أو منتهي، اطلب رمزًا جديدًا", "Wrong or expired code, request a new one")
                }
                _auth.update { it.copy(isLoading = false, isSuccessMessage = false, message = msg) }
            }
        }
    }

    private fun mapSupabaseAuthErr(code: Int, errCode: String, msg: String): String? {
        if (errCode == "email_address_not_authorized" || msg.contains("not authorized", true)) {
            return t("السيرفر لا يرسل إيميلات لهذا البريد بعد", "The server can’t email this address yet")
        }
        if (code == 429 || errCode == "over_email_send_rate_limit" || msg.contains("rate limit", true) || msg.contains("too many", true)) {
            return t("طلبات كثيرة، انتظر دقيقة وحاول مرة ثانية", "Too many requests, wait a minute and try again")
        }
        if (msg.contains("error sending", true) || msg.contains("smtp", true)) {
            return t("تعذّر إرسال الإيميل من السيرفر", "The server couldn’t send the email")
        }
        return null
    }

    private fun enterApp(email: String) {
        repo.saveSession(email)
        val langPref = repo.getLanguage()
        var loaded = repo.loadAppState(email) ?: JsonSerializer.seed(langPref)
        if (loaded.subjects.isNotEmpty() && loaded.subjects.all { it.ex }) {
            loaded = loaded.copy(
                subjects = emptyList(),
                tasks = loaded.tasks.filter { !it.ex },
                logs = loaded.logs.filter { !it.ex },
            )
        }
        loaded = loaded.copy(
            logs = JsonSerializer.compactLogs(loaded.logs),
            reviews = JsonSerializer.rollReviews(loaded.reviews),
        )
        _state.value = loaded
        repo.saveAppState(email, loaded)

        val focusSec = loaded.settings.focus * 60
        val brkSec = loaded.settings.brk * 60
        val trec = repo.loadTimer(email, focusSec, brkSec)
        val defaultSubj = loaded.subjects.firstOrNull()?.id ?: ""
        _timer.value = trec?.copy(subjId = trec.subjId.ifEmpty { defaultSubj })
            ?: TimerRuntime(mode = TimerMode.FOCUS, totalSec = focusSec, leftSec = focusSec, subjId = defaultSubj)

        _auth.value = AuthUiState(
            isLoggedIn = true,
            userEmail = email,
            showWelcomeIntro = false,
            sheetOpen = false,
        )
        _streakFriendsAlert.value = repo.getUserPref(email, "stkfn", "1") != "0"
        _taskFilter.value = TaskFilter.OPEN
        _confirmDeleteSubjectId.value = ""

        FeedbackHelper.syncNewDayAlarm(appCtx, loaded)
        FeedbackHelper.syncDailyReminderAlarm(appCtx, loaded)
        FeedbackHelper.syncStreakWarningAlarm(appCtx, loaded, true)

        if (!loaded.onboarded && loaded.subjects.isEmpty() && loaded.tasks.isEmpty()) {
            viewModelScope.launch {
                delay(500L)
                if (_auth.value.isLoggedIn && _activeSheet.value == ActiveSheet.None) {
                    _activeSheet.value = ActiveSheet.Welcome
                }
            }
        } else if (!loaded.onboarded) {
            updateState { it.copy(onboarded = true) }
        }

        viewModelScope.launch {
            syncProfileQuietly()
            syncFcmTokenInternal()
        }
    }

    fun logout() {
        val email = _auth.value.userEmail
        AlarmScheduler.cancelTimer(appCtx)
        AlarmScheduler.cancelDaily(appCtx)
        AlarmScheduler.cancelNewDay(appCtx)
        AlarmScheduler.cancelStreakWarn(appCtx)
        val fcmToken = FcmService.savedToken(appCtx)
        viewModelScope.launch {
            if (fcmToken.isNotBlank() && SupabaseApi.isConfigured()) {
                try { SupabaseApi.rpc(repo, "delete_fcm", JSONObject().put("p_token", fcmToken)) } catch (_: Exception) {}
            }
            if (email.isNotBlank()) repo.saveAppState(email, _state.value)
        }
        repo.clearFcmSig()
        repo.clearSession()
        repo.clearSbSession()
        SupabaseApi.session = null
        val lang = _state.value.lang
        val th = _state.value.theme
        val pal = _state.value.palette
        _state.value = JsonSerializer.seed(lang).copy(theme = th, palette = pal)
        _activeSheet.value = ActiveSheet.None
        _friends.value = FriendsState()
        _chat.value = ChatState()
        _unreadTotal.value = 0
        _timer.value = TimerRuntime()
        _auth.value = AuthUiState(isLoggedIn = false, showWelcomeIntro = false, sheetOpen = false, mode = AuthMode.LOGIN)
    }

    // ---------- Navigation & UI controls ----------
    fun selectTab(tab: AppTab) {
        _confirmDeleteSubjectId.value = ""
        updateState { it.copy(view = tab) }
        if (tab == AppTab.HUB) loadFriendsIfNeeded()
    }

    fun openSheet(sheet: ActiveSheet) {
        if (sheet == ActiveSheet.ProfileSettings && _state.value.fid.isEmpty()) {
            viewModelScope.launch { syncProfileQuietly(force = true) }
        }
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        if (_activeSheet.value == ActiveSheet.Welcome && !_state.value.onboarded) {
            updateState { it.copy(onboarded = true) }
        }
        _activeSheet.value = ActiveSheet.None
        if (_chat.value.code.isNotBlank()) {
            _chat.update { it.copy(code = "") }
        }
    }

    fun selectHubTab(tab: HubSubTab) {
        _hubTab.value = tab
        if (tab == HubSubTab.FRIENDS) loadFriendsIfNeeded()
    }

    fun setFontScaleIndex(idx: Int) {
        val c = idx.coerceIn(0, FontScaleSteps.lastIndex)
        _fontScaleIndex.value = c
        repo.setFontScaleIndex(c)
    }

    fun setTheme(theme: String, paletteId: String? = null) {
        val pal = paletteId ?: _state.value.palette
        repo.saveStoredTheme(theme, pal)
        updateState { it.copy(theme = theme, palette = pal) }
    }

    fun toggleQuickTheme() {
        // Opens theme picker popup so user can pick White & Yellow, all 12 palettes, Light, Dark, or Auto!
        openSheet(ActiveSheet.ThemePicker)
    }

    fun setLanguage(lang: String) {
        val l = if (lang == "en") "en" else "ar"
        repo.setLanguage(l)
        updateState { it.copy(lang = l) }
    }

    fun setArabicDigits(enabled: Boolean) {
        updateState { it.copy(arDigits = enabled) }
    }

    fun setLockDone(enabled: Boolean) {
        updateState { it.copy(lockDone = enabled) }
    }

    fun setLayoutOverride(layout: String) {
        updateState { it.copy(layout = layout) }
    }

    fun updateProfile(name: String, photoDataUrl: String) {
        updateState { it.copy(profile = UserProfile(name.trim().take(30), photoDataUrl)) }
        viewModelScope.launch {
            syncProfileQuietly(force = true)
            uploadAvatarThumb()
        }
    }

    fun handlePickedPhotoUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val encoded = repo.encodeSquareAvatar(appCtx, uri)
            if (encoded != null) {
                withContext(Dispatchers.Main) {
                    updateProfile(_state.value.profile.name, encoded)
                    showToast(t("تم تحديث الصورة", "Photo updated"))
                }
            }
        }
    }

    // ---------- Onboarding & Example cleanup ----------
    fun saveWelcome(name: String, addDefaults: Boolean) {
        updateState { s ->
            var next = s.copy(
                onboarded = true,
                profile = if (name.isNotBlank()) s.profile.copy(name = name.trim().take(30)) else s.profile,
                view = if (addDefaults) AppTab.SUBJECTS else s.view,
            )
            if (addDefaults) next = addMissing6thSubjectsToState(next)
            next
        }
        closeSheet()
    }

    fun clearExampleData() {
        updateState { s ->
            val remSubj = s.subjects.filter { !it.ex }
            s.copy(
                subjects = remSubj,
                tasks = s.tasks.filter { !it.ex },
                logs = s.logs.filter { !it.ex },
            )
        }
        if (_state.value.subjects.none { it.id == _timer.value.subjId }) {
            _timer.update { it.copy(subjId = _state.value.subjects.firstOrNull()?.id ?: "") }
        }
    }

    fun addDefault6thSubjects() {
        updateState { addMissing6thSubjectsToState(it) }
        if (_timer.value.subjId.isEmpty() && _state.value.subjects.isNotEmpty()) {
            _timer.update { it.copy(subjId = _state.value.subjects.first().id) }
        }
    }

    private fun addMissing6thSubjectsToState(s: AppState): AppState {
        val missing = Strings.missingDefaultSubjects(s.subjects, s.lang)
        if (missing.isEmpty()) return s
        val newList = s.subjects.toMutableList()
        for (preset in missing) {
            val col = colorToHex(SubjectColors[newList.size % SubjectColors.size])
            newList.add(
                Subject(
                    id = DateUtils.uid(),
                    name = if (s.lang == "ar") preset.ar else preset.en,
                    color = col,
                    target = preset.targetHours,
                )
            )
        }
        return s.copy(subjects = newList)
    }

    // ---------- Tasks ----------
    fun setTaskFilter(f: TaskFilter) { _taskFilter.value = f }
    fun setTaskViewMode(m: TaskViewMode) {
        _weekOffset.value = 0
        updateState { it.copy(taskView = m) }
    }
    fun toggleTaskSortMode() {
        updateState { it.copy(taskSort = if (it.taskSort == SortMode.MANUAL) SortMode.AUTO else SortMode.MANUAL) }
    }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilterSubj(id: String) { _filterSubj.value = id }
    fun setFilterPri(p: String) { _filterPri.value = p }
    fun setFilterDate(d: String) { _filterDate.value = d }
    fun clearFilters() {
        _searchQuery.value = ""
        _filterSubj.value = ""
        _filterPri.value = ""
        _filterDate.value = ""
    }
    fun moveWeekOffset(delta: Int, reset: Boolean = false) {
        _weekOffset.value = if (reset) 0 else _weekOffset.value + delta
    }

    fun saveTask(editId: String?, title: String, subjId: String, due: String, pri: String, rem: String) {
        val cleanTitle = title.trim().take(120)
        if (cleanTitle.isEmpty()) return
        updateState { s ->
            val updated = if (editId != null) {
                s.tasks.map { tk ->
                    if (tk.id == editId) {
                        val remChanged = tk.rem != rem || tk.due != due
                        tk.copy(
                            title = cleanTitle,
                            subj = subjId,
                            due = due,
                            pri = pri,
                            rem = rem,
                            remLast = if (remChanged) "" else tk.remLast,
                        )
                    } else tk
                }
            } else {
                s.tasks + StudyTask(
                    id = DateUtils.uid(),
                    title = cleanTitle,
                    subj = subjId,
                    due = due,
                    pri = pri,
                    rem = rem,
                )
            }
            s.copy(tasks = updated)
        }
        if (editId == null) {
            _taskFilter.value = TaskFilter.OPEN
            clearFilters()
        }
        closeSheet()
    }

    fun toggleTaskDone(taskId: String) {
        val current = _state.value.tasks.firstOrNull { it.id == taskId } ?: return
        if (current.done && _state.value.lockDone) {
            showToast(t("الإنجاز مقفل 🔒 ما تكدر تتراجع عنه", "Achievement locked 🔒 it can’t be undone"))
            return
        }
        val becomingDone = !current.done
        updateState(markAct = becomingDone) { s ->
            s.copy(tasks = s.tasks.map { if (it.id == taskId) it.copy(done = becomingDone) else it })
        }
    }

    fun deleteTask(taskId: String) {
        val list = _state.value.tasks
        val idx = list.indexOfFirst { it.id == taskId }
        if (idx < 0) return
        val removed = list[idx]
        updateState { s -> s.copy(tasks = s.tasks.filterNot { it.id == taskId }) }
        showUndoToast(t("تم حذف المهمة", "Task deleted")) {
            updateState { s ->
                val cur = s.tasks.toMutableList()
                cur.add(idx.coerceAtMost(cur.size), removed)
                s.copy(tasks = cur)
            }
        }
    }

    fun moveTaskManual(taskId: String, dir: Int, visibleOpenIds: List<String>) {
        val i = visibleOpenIds.indexOf(taskId)
        val j = i + dir
        if (i < 0 || j !in visibleOpenIds.indices) return
        val swappedIds = visibleOpenIds.toMutableList()
        val tmp = swappedIds[i]
        swappedIds[i] = swappedIds[j]
        swappedIds[j] = tmp
        reorderTasksManual(swappedIds)
    }

    fun reorderTasksManual(orderedVisibleIds: List<String>) {
        updateState { s ->
            val set = orderedVisibleIds.toSet()
            val byId = s.tasks.associateBy { it.id }
            val slots = mutableListOf<Int>()
            s.tasks.forEachIndexed { index, tk -> if (set.contains(tk.id)) slots.add(index) }
            if (slots.size != orderedVisibleIds.size) return@updateState s
            val arr = s.tasks.toMutableList()
            orderedVisibleIds.forEachIndexed { pos, id ->
                byId[id]?.let { arr[slots[pos]] = it }
            }
            s.copy(tasks = arr)
        }
    }

    // ---------- Subjects ----------
    fun saveSubject(editId: String?, name: String, targetHours: Double, colorHex: String, parts: List<SubjectPart>) {
        val cleanName = name.trim().take(40)
        if (cleanName.isEmpty()) return
        val tgt = max(0.0, targetHours)
        var createdId = ""
        updateState { s ->
            val list = if (editId != null) {
                s.subjects.map { sb ->
                    if (sb.id == editId) sb.copy(name = cleanName, target = tgt, color = colorHex, parts = parts) else sb
                }
            } else {
                createdId = DateUtils.uid()
                s.subjects + Subject(id = createdId, name = cleanName, color = colorHex, target = tgt, parts = parts)
            }
            s.copy(subjects = list)
        }
        if (createdId.isNotEmpty() && _timer.value.subjId.isEmpty()) {
            _timer.update { it.copy(subjId = createdId) }
        }
        closeSheet()
    }

    fun toggleSubjectPartDone(subjectId: String, partId: String) {
        val sb = _state.value.subjects.firstOrNull { it.id == subjectId } ?: return
        val part = sb.parts.firstOrNull { it.id == partId } ?: return
        if (part.done && _state.value.lockDone) {
            showToast(t("الإنجاز مقفل 🔒 ما تكدر تتراجع عنه", "Achievement locked 🔒 it can’t be undone"))
            return
        }
        val nextDone = !part.done
        updateState(markAct = nextDone) { s ->
            s.copy(subjects = s.subjects.map { sub ->
                if (sub.id == subjectId) {
                    sub.copy(parts = sub.parts.map { if (it.id == partId) it.copy(done = nextDone) else it })
                } else sub
            })
        }
    }

    fun requestOrConfirmDeleteSubject(subjectId: String) {
        if (_confirmDeleteSubjectId.value != subjectId) {
            _confirmDeleteSubjectId.value = subjectId
            return
        }
        _confirmDeleteSubjectId.value = ""
        val list = _state.value.subjects
        val idx = list.indexOfFirst { it.id == subjectId }
        if (idx < 0) return
        val removed = list[idx]
        val affectedTaskIds = _state.value.tasks.filter { it.subj == subjectId }.map { it.id }.toSet()
        val affectedLogIds = _state.value.logs.filter { it.subj == subjectId }.map { it.id }.toSet()

        updateState { s ->
            s.copy(
                subjects = s.subjects.filterNot { it.id == subjectId },
                tasks = s.tasks.map { if (it.subj == subjectId) it.copy(subj = "") else it },
                logs = s.logs.map { if (it.subj == subjectId) it.copy(subj = "") else it },
            )
        }
        if (_timer.value.subjId == subjectId) {
            _timer.update { it.copy(subjId = _state.value.subjects.firstOrNull()?.id ?: "") }
        }
        showUndoToast(t("تم حذف المادة", "Subject deleted")) {
            updateState { s ->
                val cur = s.subjects.toMutableList()
                cur.add(idx.coerceAtMost(cur.size), removed)
                s.copy(
                    subjects = cur,
                    tasks = s.tasks.map { if (affectedTaskIds.contains(it.id)) it.copy(subj = subjectId) else it },
                    logs = s.logs.map { if (affectedLogIds.contains(it.id)) it.copy(subj = subjectId) else it },
                )
            }
        }
    }

    fun clearConfirmDeleteSubject() {
        _confirmDeleteSubjectId.value = ""
    }

    fun toggleSubjectDailySplit(subjectId: String) {
        val s = _state.value
        val sb = s.subjects.firstOrNull { it.id == subjectId } ?: return
        val td = DateUtils.todayKey()
        if (sb.day != null && sb.day.date == td) {
            if (s.lockDone) {
                showToast(t("الإنجاز مقفل 🔒 ما تكدر تتراجع عنه", "Achievement locked 🔒 it can’t be undone"))
                return
            }
            val mark = sb.day
            updateState { st ->
                st.copy(
                    subjects = st.subjects.map {
                        if (it.id == subjectId) it.copy(doneH = max(0.0, it.doneH - mark.amt), day = null) else it
                    },
                    logs = st.logs.filterNot { it.id == mark.logId },
                )
            }
        } else {
            val plan = DateUtils.planInfo(s)
            val rem = DateUtils.subjectRemainingHours(sb)
            val raw = min(rem, rem / max(1, plan?.left ?: 1))
            var amt = (((raw * 60.0) / 5.0).roundToInt() * 5) / 60.0
            if (amt > rem) amt = rem
            if (amt <= 0.0) return
            val logId = DateUtils.uid()
            val mins = (amt * 60.0).roundToInt().coerceAtLeast(1)
            updateState(markAct = true) { st ->
                st.copy(
                    subjects = st.subjects.map {
                        if (it.id == subjectId) {
                            it.copy(doneH = it.doneH + amt, day = DayAmountMark(td, amt, logId))
                        } else it
                    },
                    logs = st.logs + StudyLog(logId, sb.id, mins, td),
                )
            }
        }
    }

    // ---------- Plan, Reviews & Checklists ----------
    fun savePlan(name: String, days: Int) {
        val d = days.coerceIn(1, 365)
        updateState { it.copy(plan = StudyPlan(d, DateUtils.todayKey(), name.trim().take(40))) }
        closeSheet()
    }

    fun clearPlan() {
        updateState { it.copy(plan = null) }
        closeSheet()
    }

    fun saveReview(editId: String?, kind: String, name: String, hours: Double, days: Int) {
        val cleanName = name.trim().take(40)
        if (cleanName.isEmpty() || hours <= 0.0 || days <= 0) return
        val td = DateUtils.todayKey()
        val d = days.coerceIn(1, 365)
        updateState { s ->
            val list = if (editId != null) {
                s.reviews.map { r ->
                    if (r.id == editId) r.copy(name = cleanName, hours = hours, days = d, start = td, ext = 0, chk = td, dd = emptySet())
                    else r
                }
            } else {
                val col = colorToHex(SubjectColors[s.reviews.size % SubjectColors.size])
                s.reviews + ReviewItem(
                    id = DateUtils.uid(),
                    name = cleanName,
                    hours = hours,
                    days = d,
                    start = td,
                    chk = td,
                    kind = kind,
                    color = col,
                )
            }
            s.copy(reviews = list)
        }
        closeSheet()
    }

    fun deleteReview(reviewId: String) {
        val list = _state.value.reviews
        val idx = list.indexOfFirst { it.id == reviewId }
        if (idx < 0) return
        val removed = list[idx]
        updateState { s -> s.copy(reviews = s.reviews.filterNot { it.id == reviewId }) }
        closeSheet()
        showUndoToast(t("تم حذف المراجعة", "Review deleted")) {
            updateState { s ->
                val cur = s.reviews.toMutableList()
                cur.add(idx.coerceAtMost(cur.size), removed)
                s.copy(reviews = cur)
            }
        }
    }

    fun toggleReviewDaily(reviewId: String) {
        val s = _state.value
        val rv = s.reviews.firstOrNull { it.id == reviewId } ?: return
        val td = DateUtils.todayKey()
        if (rv.day != null && rv.day.date == td) {
            if (s.lockDone) {
                showToast(t("الإنجاز مقفل 🔒 ما تكدر تتراجع عنه", "Achievement locked 🔒 it can’t be undone"))
                return
            }
            val mark = rv.day
            updateState { st ->
                st.copy(
                    reviews = st.reviews.map {
                        if (it.id == reviewId) {
                            it.copy(doneH = max(0.0, it.doneH - mark.amt), day = null, dd = it.dd - td)
                        } else it
                    },
                    logs = st.logs.filterNot { it.id == mark.logId },
                )
            }
        } else {
            val info = DateUtils.reviewInfo(rv)
            var am = (((info.share * 60.0) / 5.0).roundToInt() * 5) / 60.0
            if (am > info.rem) am = info.rem
            if (am <= 0.0) return
            val logId = DateUtils.uid()
            val mins = (am * 60.0).roundToInt().coerceAtLeast(1)
            updateState(markAct = true) { st ->
                st.copy(
                    reviews = st.reviews.map {
                        if (it.id == reviewId) {
                            it.copy(doneH = it.doneH + am, day = DayAmountMark(td, am, logId), dd = it.dd + td)
                        } else it
                    },
                    logs = st.logs + StudyLog(logId, "", mins, td),
                )
            }
        }
    }

    fun cleanCompletedReviews(kind: String) {
        val gone = _state.value.reviews.filter { DateUtils.reviewInfo(it).fin && it.kind == kind }
        if (gone.isEmpty()) return
        val goneIds = gone.map { it.id }.toSet()
        updateState { s -> s.copy(reviews = s.reviews.filterNot { goneIds.contains(it.id) }) }
        showUndoToast(
            t("تم تنظيف ${gone.size} من المراجعات المنجزة", "Cleared ${gone.size} completed review(s)")
        ) {
            updateState { s -> s.copy(reviews = s.reviews + gone) }
        }
    }

    fun addChecklistTopics(kind: String, names: List<String>) {
        val cleaned = names.map { it.trim().take(80) }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        updateState { s ->
            s.copy(lists = s.lists + cleaned.map { CheckListItem(DateUtils.uid(), kind, it, false) })
        }
        closeSheet()
    }

    fun toggleChecklistItem(itemId: String) {
        val item = _state.value.lists.firstOrNull { it.id == itemId } ?: return
        if (item.done && _state.value.lockDone) {
            showToast(t("الإنجاز مقفل 🔒 ما تكدر تتراجع عنه", "Achievement locked 🔒 it can’t be undone"))
            return
        }
        val becomingDone = !item.done
        updateState(markAct = becomingDone) { s ->
            s.copy(lists = s.lists.map { if (it.id == itemId) it.copy(done = becomingDone) else it })
        }
    }

    fun deleteChecklistItem(itemId: String) {
        val list = _state.value.lists
        val idx = list.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val removed = list[idx]
        updateState { s -> s.copy(lists = s.lists.filterNot { it.id == itemId }) }
        showUndoToast(t("تم الحذف", "Deleted")) {
            updateState { s ->
                val cur = s.lists.toMutableList()
                cur.add(idx.coerceAtMost(cur.size), removed)
                s.copy(lists = cur)
            }
        }
    }

    fun cleanChecklistItems(kind: String) {
        val gone = _state.value.lists.filter { it.done && it.kind == kind }
        if (gone.isEmpty()) return
        val goneIds = gone.map { it.id }.toSet()
        updateState { s -> s.copy(lists = s.lists.filterNot { goneIds.contains(it.id) }) }
        showUndoToast(t("تم تنظيف ${gone.size} منجز", "Cleared ${gone.size} completed")) {
            updateState { s -> s.copy(lists = s.lists + gone) }
        }
    }

    // ---------- Timer ----------
    fun focusOnSubjectFromHome(subjectId: String) {
        if (_timer.value.mode != TimerMode.FOCUS || _timer.value.running) {
            setTimerMode(TimerMode.FOCUS)
        }
        _timer.update { it.copy(subjId = subjectId) }
        repo.saveTimer(_auth.value.userEmail, _timer.value)
        selectTab(AppTab.TIMER)
    }

    fun selectTimerSubject(subjectId: String) {
        _timer.update { it.copy(subjId = subjectId) }
        repo.saveTimer(_auth.value.userEmail, _timer.value)
    }

    fun setTimerMode(mode: TimerMode) {
        unpublishAutoNowStatus()
        val s = _state.value.settings
        val sec = (if (mode == TimerMode.FOCUS) s.focus else s.brk) * 60
        val next = _timer.value.copy(mode = mode, running = false, endAt = 0L, leftMs = null, leftSec = sec, totalSec = sec)
        _timer.value = next
        repo.saveTimer(_auth.value.userEmail, next)
        AlarmScheduler.cancelTimer(appCtx)
    }

    fun resetTimer() {
        setTimerMode(_timer.value.mode)
    }

    fun toggleTimerRunning() {
        val cur = _timer.value
        val now = System.currentTimeMillis()
        val next = if (cur.running) {
            val ms = max(0L, cur.endAt - now)
            cur.copy(running = false, leftMs = ms, leftSec = ceil(ms / 1000.0).toInt())
        } else {
            val remMs = cur.leftMs ?: (cur.leftSec * 1000L)
            cur.copy(running = true, endAt = now + remMs, leftMs = null)
        }
        _timer.value = next
        repo.saveTimer(_auth.value.userEmail, next)
        if (next.running) {
            val s = _state.value
            val body = if (next.mode == TimerMode.FOCUS) {
                t("انتهت جلسة التركيز، خذ استراحة 🎉", "Focus session finished, take a break 🎉")
            } else {
                t("انتهت الاستراحة ⏰ ابدأ التايمر وروح ادرس 📚", "Break is over ⏰ start the timer and get back to studying 📚")
            }
            val title = t("مذاكرتي", "Mudhakirati")
            AlarmScheduler.scheduleTimer(appCtx, next.endAt, title, body)
            val chainMs = if (next.mode == TimerMode.FOCUS) s.settings.brk * 60_000L else 0L
            AlarmScheduler.setBreakChain(
                appCtx,
                chainMs,
                title,
                t("انتهت الاستراحة ⏰ ابدأ التايمر وروح ادرس 📚", "Break is over ⏰ start the timer and get back to studying 📚"),
            )
            publishAutoTimerNowStatus(next)
        } else {
            AlarmScheduler.cancelTimer(appCtx)
            unpublishAutoNowStatus()
        }
    }

    fun updateTimerSettings(focusMin: Int, breakMin: Int, goalMin: Int) {
        val f = focusMin.coerceAtLeast(1)
        val b = breakMin.coerceIn(1, 60)
        val g = goalMin.coerceIn(10, 900)
        updateState { it.copy(settings = TimerSettings(f, b, g)) }
        if (!_timer.value.running) {
            val sec = (if (_timer.value.mode == TimerMode.FOCUS) f else b) * 60
            val next = _timer.value.copy(leftSec = sec, totalSec = sec, leftMs = null)
            _timer.value = next
            repo.saveTimer(_auth.value.userEmail, next)
        }
        FeedbackHelper.syncDailyReminderAlarm(appCtx, _state.value)
        closeSheet()
    }

    fun logManualStudyTime(minutes: Int, subjId: String) {
        if (minutes <= 0) return
        updateState(markAct = true) { s ->
            s.copy(logs = s.logs + StudyLog(DateUtils.uid(), subjId, minutes, DateUtils.todayKey()))
        }
        closeSheet()
    }

    private fun tickTimer() {
        val cur = _timer.value
        if (!cur.running) return
        val left = min(cur.totalSec, ceil((cur.endAt - System.currentTimeMillis()) / 1000.0).toInt())
        if (left <= 0) {
            val wasFocus = cur.mode == TimerMode.FOCUS
            val focusMin = _state.value.settings.focus
            if (wasFocus) {
                updateState(markAct = true) { s ->
                    s.copy(logs = s.logs + StudyLog(DateUtils.uid(), cur.subjId, focusMin, DateUtils.todayKey()))
                }
            }
            FeedbackHelper.vibratePattern(appCtx, longArrayOf(0, 200, 100, 200))
            FeedbackHelper.playAlertSound(appCtx)
            val subjName = _state.value.subjects.firstOrNull { it.id == cur.subjId }?.name
            val msg = if (wasFocus) {
                Strings.streakDoneMessage(subjName, DateUtils.computeStreak(_state.value), _state.value.lang)
            } else {
                t("انتهت الاستراحة ⏰ ابدأ التايمر وروح ادرس 📚", "Break is over ⏰ start the timer and get back to studying 📚")
            }
            showToast(msg)
            FeedbackHelper.showLocalNotification(appCtx, t("مذاكرتي", "Mudhakirati"), msg, "session-end")
            setTimerMode(if (wasFocus) TimerMode.BREAK else TimerMode.FOCUS)
        } else if (left != cur.leftSec) {
            _timer.value = cur.copy(leftSec = left)
        }
    }

    // ---------- Progress Charts ----------
    fun setProgressRange(range: ProgressRange) {
        _monthOffset.value = 0
        _selectedProgressDay.value = DateUtils.todayKey()
        updateState { it.copy(progRange = range) }
    }

    fun moveMonthOffset(delta: Int) {
        _monthOffset.value = (_monthOffset.value + delta).coerceAtMost(0)
        _selectedProgressDay.value = ""
    }

    fun selectProgressDay(dateKey: String) {
        _selectedProgressDay.value = dateKey
    }

    // ---------- Reminders ----------
    fun updateDailyReminder(on: Boolean? = null, time: String? = null, subjId: String? = null) {
        updateState { s ->
            val r = s.remind
            s.copy(
                remind = r.copy(
                    on = on ?: r.on,
                    time = time ?: r.time,
                    subj = subjId ?: r.subj,
                    last = if (on != null || time != null) "" else r.last,
                )
            )
        }
        FeedbackHelper.syncDailyReminderAlarm(appCtx, _state.value)
    }

    private fun checkDayRollover() {
        val td = DateUtils.todayKey()
        if (td != lastDayChecked) {
            lastDayChecked = td
            updateState { it.copy(reviews = JsonSerializer.rollReviews(it.reviews)) }
        }
    }

    private fun checkTaskAndDailyReminders() {
        val s = _state.value
        val cal = java.util.Calendar.getInstance()
        val nowHHMM = "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        val td = DateUtils.todayKey()
        val title = t("مذاكرتي", "Mudhakirati")

        if (s.remind.on && nowHHMM >= s.remind.time && s.remind.last != td) {
            val body = Strings.remindBody(s)
            updateState { it.copy(remind = it.remind.copy(last = td)) }
            FeedbackHelper.vibratePattern(appCtx, longArrayOf(0, 200, 100, 200))
            showToast(body)
            FeedbackHelper.showLocalNotification(appCtx, title, body, AlarmScheduler.TAG_DAILY)
        }

        var changed = false
        val updatedTasks = s.tasks.map { tk ->
            if (!tk.done && tk.rem.isNotBlank() && tk.due == td && tk.remLast != td && nowHHMM >= tk.rem) {
                changed = true
                val msg = "${tk.title} · ${DateUtils.formatTime12(tk.rem, s.lang, s.arDigits)}"
                FeedbackHelper.vibratePattern(appCtx, longArrayOf(0, 200, 100, 200))
                showToast(msg)
                FeedbackHelper.showLocalNotification(appCtx, title, msg, "task-${tk.id}")
                tk.copy(remLast = td)
            } else tk
        }
        if (changed) {
            updateState { it.copy(tasks = updatedTasks) }
        }
    }

    // ---------- Hub: Weekly Schedule ----------
    fun toggleScheduleView() {
        val next = !_scheduleDayView.value
        _scheduleDayView.value = next
        repo.setScheduleDayView(next)
    }

    fun selectScheduleDay(day: Int) {
        _selectedSchedDay.value = day
    }

    fun changeSchedulePeriods(delta: Int) {
        updateState { s ->
            val newN = (s.sched.n + delta).coerceIn(1, 12)
            val filtered = s.sched.cells.filterKeys { ck ->
                (ck.split("-").getOrNull(1)?.toIntOrNull() ?: 0) < newN
            }
            s.copy(sched = s.sched.copy(n = newN, cells = filtered))
        }
    }

    fun toggleScheduleVisibleDay(day: Int) {
        updateState { s ->
            val cur = s.sched.days.toMutableList()
            if (cur.contains(day)) {
                if (cur.size > 1) cur.remove(day)
            } else {
                cur.add(day)
            }
            val ordered = Strings.scheduleDayOrder.filter { cur.contains(it) }
            s.copy(sched = s.sched.copy(days = ordered, sat = ordered.contains(6)))
        }
    }

    fun saveScheduleCell(cellKey: String, value: String) {
        val clean = value.trim().take(30)
        updateState { s ->
            val nextCells = s.sched.cells.toMutableMap()
            if (clean.isEmpty()) nextCells.remove(cellKey) else nextCells[cellKey] = clean
            s.copy(sched = s.sched.copy(cells = nextCells))
        }
        closeSheet()
    }

    // ---------- Hub: Notes ----------
    fun saveNote(editId: String?, title: String, txt: String, colorHex: String, pin: Boolean) {
        val cleanTxt = txt.trim().take(600)
        if (cleanTxt.isEmpty()) return
        val cleanTitle = title.trim().take(60)
        updateState { s ->
            val list = if (editId != null) {
                s.notes.map { n ->
                    if (n.id == editId) n.copy(title = cleanTitle, txt = cleanTxt, color = colorHex, pin = pin) else n
                }
            } else {
                s.notes + StudyNote(DateUtils.uid(), cleanTitle, cleanTxt, colorHex, pin, System.currentTimeMillis())
            }
            s.copy(notes = list)
        }
        closeSheet()
    }

    fun deleteNote(noteId: String) {
        val list = _state.value.notes
        val idx = list.indexOfFirst { it.id == noteId }
        if (idx < 0) return
        val removed = list[idx]
        updateState { s -> s.copy(notes = s.notes.filterNot { it.id == noteId }) }
        closeSheet()
        showUndoToast(t("تم حذف الملاحظة", "Note deleted")) {
            updateState { s ->
                val cur = s.notes.toMutableList()
                cur.add(idx.coerceAtMost(cur.size), removed)
                s.copy(notes = cur)
            }
        }
    }

    // ---------- Hub: Friends, Studying Now Status & Chat ----------
    fun setStreakFriendsAlert(enabled: Boolean) {
        _streakFriendsAlert.value = enabled
        val email = _auth.value.userEmail
        if (email.isNotBlank()) repo.setUserPref(email, "stkfn", if (enabled) "1" else "0")
        if (!enabled) unpublishAutoNowStatus()
    }

    fun setNowKind(kind: String) { _friends.update { it.copy(nowKind = kind, nowSelectedName = "") } }
    fun setNowDuration(mins: Int) { _friends.update { it.copy(nowDuration = mins) } }
    fun selectNowSubjectName(name: String) { _friends.update { it.copy(nowSelectedName = name) } }
    fun setNowCustomName(name: String) { _friends.update { it.copy(nowCustomName = name) } }

    fun startNowStatus() {
        val fr = _friends.value
        val name = if (fr.nowKind == "custom") fr.nowCustomName.trim() else fr.nowSelectedName.trim()
        if (name.isEmpty()) {
            _friends.update {
                it.copy(
                    ok = false,
                    msg = if (fr.nowKind == "custom") t("اكتب اسم الشي اللي راح تدرسه", "Type what you will study")
                    else t("اختر المادة أولاً", "Pick a subject first"),
                )
            }
            return
        }
        val kindLbl = when (fr.nowKind) {
            "hw" -> t("واجب", "Homework")
            "book" -> t("كتاب", "Book")
            "custom" -> t("مخصص", "Custom")
            else -> t("درس", "Lesson")
        }
        val text = "$kindLbl · ${name.take(34)} · ${fr.nowDuration} ${t("د", "min")}"
        val nowMs = System.currentTimeMillis()
        updateState { it.copy(nowT = text, nowAt = nowMs) }
        autoStatusTimestamp = ""
        _friends.update { it.copy(nowSelectedName = "", nowCustomName = "", msg = "") }
        viewModelScope.launch {
            try {
                syncProfileQuietly(force = true)
                SupabaseApi.rpc(repo, "set_status", JSONObject().put("p_text", text))
                refreshFriends()
            } catch (e: Exception) {
                handleFriendsError(e)
            }
        }
    }

    fun endNowStatus() {
        updateState { it.copy(nowT = "", nowAt = 0L) }
        autoStatusTimestamp = ""
        viewModelScope.launch {
            try {
                SupabaseApi.rpc(repo, "set_status", JSONObject().put("p_text", ""))
                refreshFriends()
            } catch (e: Exception) {
                handleFriendsError(e)
            }
        }
    }

    private fun publishAutoTimerNowStatus(tm: TimerRuntime) {
        if (tm.mode != TimerMode.FOCUS || !SupabaseApi.isConfigured() || !_auth.value.isLoggedIn || !_streakFriendsAlert.value) return
        val s = _state.value
        if (DateUtils.isNowActive(s.nowT, s.nowAt) && autoStatusTimestamp != s.nowAt.toString()) return
        val sb = s.subjects.firstOrNull { it.id == tm.subjId } ?: return
        val mins = max(1, ceil((tm.endAt - System.currentTimeMillis()) / 60_000.0).toInt())
        val txt = "${t("مؤقت", "Timer")} · ${sb.name.take(34)} · $mins ${t("د", "min")}"
        val nowMs = System.currentTimeMillis()
        autoStatusTimestamp = nowMs.toString()
        updateState { it.copy(nowT = txt, nowAt = nowMs) }
        viewModelScope.launch {
            try {
                syncProfileQuietly(force = true)
                SupabaseApi.rpc(repo, "set_status", JSONObject().put("p_text", txt))
            } catch (_: Exception) {}
        }
    }

    private fun unpublishAutoNowStatus() {
        val s = _state.value
        if (s.nowT.isBlank() || autoStatusTimestamp != s.nowAt.toString()) return
        autoStatusTimestamp = ""
        updateState { it.copy(nowT = "", nowAt = 0L) }
        viewModelScope.launch {
            try {
                if (SupabaseApi.isConfigured()) {
                    SupabaseApi.rpc(repo, "set_status", JSONObject().put("p_text", ""))
                }
            } catch (_: Exception) {}
        }
    }

    private fun checkSelfStatusEnd() {
        val s = _state.value
        if (s.nowT.isBlank() || s.nowAt <= 0L) return
        val d = DateUtils.nowActivityDuration(s.nowT)
        if (d <= 0) return
        val now = System.currentTimeMillis()
        val endMs = s.nowAt + d * 60_000L
        if (now < endMs || now > s.nowAt + (d + 30) * 60_000L) return
        val atKey = s.nowAt.toString()
        if (selfNotifiedAt == atKey) return
        selfNotifiedAt = atKey
        val parts = s.nowT.split(" · ")
        val lbl = if (parts.size > 2) parts[1] else s.nowT
        val msg = Strings.streakDoneMessage(lbl, DateUtils.computeStreak(s), s.lang)
        showToast(msg)
        FeedbackHelper.vibratePattern(appCtx, longArrayOf(0, 200, 100, 200))
        FeedbackHelper.showLocalNotification(appCtx, t("مذاكرتي", "Mudhakirati"), msg, "session-end")
    }

    private suspend fun syncProfileQuietly(force: Boolean = false) {
        if (!SupabaseApi.isConfigured() || !_auth.value.isLoggedIn) return
        val s = _state.value
        val days = DateUtils.myActiveDays(s)
        val nm = s.profile.name
        val sig = "$nm|${days.joinToString(",")}"
        if (!force && sig == lastSyncedSig && s.fid.isNotEmpty()) return
        try {
            val raw = SupabaseApi.rpc(
                repo,
                "sync_profile",
                JSONObject().put("p_name", nm).put("p_days", JSONArray(days)),
            )
            val code = raw.trim().removeSurrounding("\"")
            lastSyncedSig = sig
            if (Regex("""^[A-Z0-9]{8}$""").matches(code) && code != s.fid) {
                updateState { it.copy(fid = code) }
            }
            if (_friends.value.err == "setup") _friends.update { it.copy(err = "") }
            uploadAvatarThumb()
        } catch (e: Exception) {
            handleFriendsError(e)
        }
    }

    private suspend fun uploadAvatarThumb() {
        val uid = SupabaseApi.session?.userId ?: return
        val photo = _state.value.profile.photo
        val sig = "$uid|${photo.length}|${photo.takeLast(32)}"
        if (repo.getPhotoSig() == sig) return
        val thumb = withContext(Dispatchers.Default) { repo.makeThumbnailDataUrl(photo) }
        if (photo.isNotEmpty() && thumb.isEmpty()) return
        try {
            SupabaseApi.rpc(repo, "set_photo", JSONObject().put("p_photo", thumb))
            repo.setPhotoSig(sig)
        } catch (_: Exception) {}
    }

    private suspend fun syncFcmTokenInternal() {
        val uid = SupabaseApi.session?.userId ?: return
        val tk = FcmService.savedToken(appCtx)
        if (tk.isBlank()) return
        val sig = "$uid|$tk"
        if (repo.getFcmSig() == sig) return
        try {
            SupabaseApi.rpc(repo, "save_fcm", JSONObject().put("p_token", tk))
            repo.setFcmSig(sig)
        } catch (_: Exception) {}
    }

    fun loadFriendsIfNeeded() {
        if (!_friends.value.loadedOnce) refreshFriends()
    }

    fun refreshFriends() {
        if (!SupabaseApi.isConfigured() || !_auth.value.isLoggedIn) return
        _friends.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                syncProfileQuietly(force = true)
                val listRaw = SupabaseApi.rpc(repo, "get_friends")
                val friendsList = parseFriends(listRaw)
                val reqRaw = SupabaseApi.rpc(repo, "get_requests")
                val (inc, out) = parseRequests(reqRaw)
                _friends.update {
                    it.copy(
                        list = friendsList,
                        incoming = inc,
                        outgoing = out,
                        err = "",
                        busy = false,
                        loadedOnce = true,
                    )
                }
                updateUnreadCount(friendsList, inc)
            } catch (e: Exception) {
                handleFriendsError(e)
                _friends.update { it.copy(busy = false, loadedOnce = true) }
            }
        }
    }

    private suspend fun pollFriendsQuietly() {
        try {
            val raw = SupabaseApi.rpc(repo, "get_friends")
            val friendsList = parseFriends(raw)
            val now = System.currentTimeMillis()
            val mineSet = DateUtils.myActiveDays(_state.value).toSet()

            for (f in friendsList) {
                val d = DateUtils.nowActivityDuration(f.nowText)
                val cur = if (f.nowText.isNotBlank() && f.nowAtMs > 0L && d > 0) {
                    f.nowAtMs to (f.nowAtMs + d * 60_000L)
                } else null
                val prev = seenFriendEnds[f.code]
                val finishedPair = when {
                    cur != null && now >= cur.second && (now - cur.second) < 600_000L -> cur
                    prev != null && (cur == null || cur.first != prev.first) && now >= prev.second - 70_000L && (now - prev.second) < 600_000L -> prev
                    else -> null
                }
                if (cur != null) seenFriendEnds[f.code] = cur else seenFriendEnds.remove(f.code)

                if (finishedPair != null) {
                    val prefKey = "stkfd:${f.code}:${finishedPair.first}"
                    val email = _auth.value.userEmail
                    if (repo.getUserPref(email, prefKey).isEmpty()) {
                        repo.setUserPref(email, prefKey, "1")
                        val nm = friendDisplayName(f.code, f.name)
                        val lbl = f.nowText.split(" · ").getOrNull(1) ?: f.nowText
                        val sh = DateUtils.computeSharedStreak(mineSet, f.days)
                        val msg = t("$nm خلّص وقت $lbl 🎉", "$nm finished $lbl 🎉") +
                            if (sh > 0) " · ${t("ستريككم 🔥 $sh", "shared 🔥 $sh")}" else ""
                        showToast(msg)
                        FeedbackHelper.showLocalNotification(appCtx, t("مذاكرتي", "Mudhakirati"), msg, "fr-${f.code}")
                    }
                }
                val prevUn = seenFriendMsgs[f.code]
                if (!(_activeSheet.value == ActiveSheet.FriendChat && _chat.value.code == f.code)) {
                    if (prevUn != null && f.unread > prevUn) {
                        val nm = friendDisplayName(f.code, f.name)
                        val msg = "💬 $nm" + if (f.lastMsg.isNotBlank()) ": ${f.lastMsg}" else ""
                        showToast(msg)
                        FeedbackHelper.showLocalNotification(appCtx, t("مذاكرتي", "Mudhakirati"), msg, "msg-${f.code}")
                    }
                }
                seenFriendMsgs[f.code] = f.unread
            }

            val reqRaw = SupabaseApi.rpc(repo, "get_requests")
            val (inc, out) = parseRequests(reqRaw)
            seenReqCodes?.let { old ->
                for (q in inc) {
                    if (!old.contains(q.code)) {
                        val nm = friendDisplayName(q.code, q.name)
                        val msg = "👋 $nm${t(" أرسل لك طلب صداقة", " sent you a friend request")}"
                        showToast(msg)
                        FeedbackHelper.showLocalNotification(appCtx, t("مذاكرتي", "Mudhakirati"), msg, "rq-${q.code}")
                    }
                }
            }
            seenReqCodes = inc.map { it.code }.toSet()

            _friends.update { it.copy(list = friendsList, incoming = inc, outgoing = out) }
            updateUnreadCount(friendsList, inc)
        } catch (_: Exception) {}
    }

    private fun updateUnreadCount(list: List<FriendItem>, inc: List<FriendRequest>) {
        _unreadTotal.value = list.sumOf { it.unread } + inc.size
    }

    fun friendDisplayName(code: String, profileName: String): String {
        val nick = repo.getAllNicknames(_auth.value.userEmail)[code]
        if (!nick.isNullOrBlank()) return nick
        if (profileName.isNotBlank()) return profileName
        return "${t("صديق ", "Friend ")}${Strings.formatFriendId(code)}"
    }

    fun sendFriendRequest(rawCode: String) {
        val code = rawCode.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (code.length != 8) {
            _friends.update { it.copy(ok = false, msg = t("الـ ID يتكوّن من 8 خانات (أحرف وأرقام).", "The ID has 8 characters (letters and digits).")) }
            return
        }
        if (code == _state.value.fid) {
            _friends.update { it.copy(ok = false, msg = t("هذا الـ ID حقك أنت 😄", "That’s your own ID")) }
            return
        }
        _friends.update { it.copy(busy = true, msg = "", ok = false) }
        viewModelScope.launch {
            try {
                syncProfileQuietly(force = true)
                val r = SupabaseApi.rpc(repo, "send_friend_request", JSONObject().put("p_code", code))
                    .trim().removeSurrounding("\"")
                val (ok, msg) = when (r) {
                    "sent" -> true to t("تم إرسال الطلب ✓ بانتظار موافقته", "Request sent ✓ waiting for approval")
                    "accepted" -> true to t("صرتوا أصدقاء ✓ (هو كان مرسل لك طلب)", "You’re now friends ✓ (they had sent you a request)")
                    "pending" -> true to t("أرسلت له طلب من قبل، بانتظار موافقته", "Request already sent, waiting for approval")
                    "already" -> false to t("هذا صديقك بالفعل", "Already your friend")
                    "self" -> false to t("هذا الـ ID حقك أنت", "That’s your own ID")
                    else -> false to t("ما لقينا حساب بهذا الـ ID", "No account with this ID")
                }
                _friends.update { it.copy(ok = ok, msg = msg) }
                refreshFriends()
            } catch (e: Exception) {
                val m = e.message ?: ""
                val msg = when {
                    m == "rate_limited" -> t("محاولات كثيرة، انتظر شوي وجرب مرة ثانية", "Too many attempts, wait a bit and try again")
                    m == "too_many_requests" -> t("عندك طلبات معلّقة كثيرة، الغِ بعضها أولاً", "Too many pending requests, cancel some first")
                    else -> {
                        handleFriendsError(e)
                        ""
                    }
                }
                _friends.update { it.copy(busy = false, ok = false, msg = msg) }
            }
        }
    }

    fun respondFriendRequest(code: String, accept: Boolean) {
        _friends.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                SupabaseApi.rpc(repo, "respond_request", JSONObject().put("p_code", code).put("p_accept", accept))
                if (accept) _friends.update { it.copy(ok = true, msg = t("تمت إضافة الصديق ✓", "Friend added ✓")) }
            } catch (e: Exception) {
                handleFriendsError(e)
            }
            refreshFriends()
        }
    }

    fun cancelOutgoingRequest(code: String) {
        _friends.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                SupabaseApi.rpc(repo, "cancel_request", JSONObject().put("p_code", code))
            } catch (e: Exception) {
                handleFriendsError(e)
            }
            refreshFriends()
        }
    }

    fun removeFriend(code: String) {
        askConfirm(
            title = t("حذف صديق", "Remove friend"),
            message = t("تحذف هذا الصديق من القائمة؟", "Remove this friend?"),
            confirmLabel = t("حذف", "Remove"),
            isDanger = true,
        ) {
            _friends.update { it.copy(busy = true) }
            viewModelScope.launch {
                try {
                    SupabaseApi.rpc(repo, "remove_friend", JSONObject().put("p_code", code))
                } catch (e: Exception) {
                    handleFriendsError(e)
                }
                refreshFriends()
            }
        }
    }

    fun openFriendChat(friend: FriendItem) {
        _chat.value = ChatState(
            code = friend.code,
            name = friendDisplayName(friend.code, friend.name),
            photo = friend.photo,
            busy = true,
        )
        _activeSheet.value = ActiveSheet.FriendChat
        loadChatMessages()
    }

    fun toggleChatNicknameBar() {
        _chat.update { it.copy(nicknameInputOpen = !it.nicknameInputOpen) }
    }

    fun saveChatNickname(nickname: String) {
        val code = _chat.value.code
        repo.setNickname(_auth.value.userEmail, code, nickname.trim().take(20))
        val profName = _friends.value.list.firstOrNull { it.code == code }?.name ?: ""
        _chat.update { it.copy(nicknameInputOpen = false, name = friendDisplayName(code, profName)) }
    }

    fun blockChatFriend() {
        val code = _chat.value.code
        askConfirm(
            title = t("حظر", "Block"),
            message = t("تحظر هذا الصديق؟ تنحذف الصداقة والرسائل، وما يكدر يضيفك مرة ثانية.", "Block this friend? The friendship and messages are removed and they can’t add you again."),
            confirmLabel = t("حظر", "Block"),
            isDanger = true,
        ) {
            viewModelScope.launch {
                try { SupabaseApi.rpc(repo, "block_friend", JSONObject().put("p_code", code)) } catch (_: Exception) {}
                closeSheet()
                refreshFriends()
            }
        }
    }

    fun loadChatMessages() {
        val cur = _chat.value
        val code = cur.code
        if (code.isBlank()) return
        val afterId = cur.msgs.maxOfOrNull { it.id } ?: 0L
        viewModelScope.launch {
            try {
                val raw = SupabaseApi.rpc(
                    repo,
                    "get_messages",
                    JSONObject().put("p_code", code).put("p_after", afterId),
                )
                val obj = JSONObject(raw)
                val arr = obj.optJSONArray("msgs")
                val incoming = mutableListOf<ChatMessage>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val m = arr.optJSONObject(i) ?: continue
                        incoming.add(
                            ChatMessage(
                                id = m.optLong("id"),
                                mine = m.optBoolean("mine"),
                                kind = m.optString("kind", "text"),
                                text = m.optString("text"),
                                at = m.optString("at"),
                            )
                        )
                    }
                }
                val ru = obj.optLong("read_upto", 0L)
                if (_chat.value.code == code) {
                    val existingIds = _chat.value.msgs.map { it.id }.toSet()
                    val merged = (_chat.value.msgs + incoming.filter { !existingIds.contains(it.id) }).sortedBy { it.id }
                    _chat.update {
                        it.copy(msgs = merged, readUpto = max(it.readUpto, ru), err = "", busy = false)
                    }
                    _friends.update { fs ->
                        fs.copy(list = fs.list.map { if (it.code == code) it.copy(unread = 0) else it })
                    }
                    updateUnreadCount(_friends.value.list, _friends.value.incoming)
                }
            } catch (e: Exception) {
                if (_chat.value.code == code) {
                    _chat.update { it.copy(err = if (SupabaseApi.isMissingRpc(e)) "setup" else "net", busy = false) }
                }
            }
        }
    }

    fun sendChatMessage(rawText: String, kind: String = "text") {
        val clean = rawText.trim().take(120)
        val code = _chat.value.code
        if (clean.isEmpty() || code.isBlank()) return
        viewModelScope.launch {
            try {
                val raw = SupabaseApi.rpc(
                    repo,
                    "send_message",
                    JSONObject().put("p_code", code).put("p_text", clean).put("p_kind", kind),
                )
                val obj = JSONObject(raw)
                val msg = ChatMessage(
                    id = obj.optLong("id"),
                    mine = true,
                    kind = kind,
                    text = clean,
                    at = obj.optString("at"),
                )
                if (_chat.value.code == code) {
                    val existing = _chat.value.msgs.map { it.id }.toSet()
                    if (!existing.contains(msg.id)) {
                        _chat.update { it.copy(msgs = (it.msgs + msg).sortedBy { x -> x.id }, err = "") }
                    }
                }
            } catch (e: Exception) {
                val m = e.message ?: ""
                if (SupabaseApi.isMissingRpc(e)) {
                    _chat.update { it.copy(err = "setup") }
                } else if (m.contains("rate_limited")) {
                    showToast(t("انتظر شوي قبل ما ترسل رسالة ثانية", "Wait a moment before sending again"))
                } else if (m.contains("not_friends")) {
                    showToast(t("ما تكدر ترسل لهذا الصديق", "You can’t message this friend"))
                } else {
                    showToast(t("تعذّر الإرسال، حاول مرة ثانية", "Could not send, try again"))
                }
            }
        }
    }

    private fun handleFriendsError(e: Exception) {
        val err = when {
            SupabaseApi.isMissingRpc(e) -> "setup"
            (e as? SupabaseException)?.errorCode == "offline" -> "login"
            else -> "net"
        }
        _friends.update { it.copy(err = err) }
    }

    private fun parseFriends(raw: String): List<FriendItem> {
        val arr = try { JSONArray(raw) } catch (_: Exception) { return emptyList() }
        val out = mutableListOf<FriendItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val daysArr = obj.optJSONArray("days")
            val daysSet = mutableSetOf<String>()
            if (daysArr != null) {
                for (j in 0 until daysArr.length()) {
                    val d = daysArr.optString(j)
                    if (d.isNotBlank()) daysSet.add(d)
                }
            }
            val nowAtStr = obj.optString("now_at", "")
            val nowAtMs = parseIsoTimestampMs(nowAtStr)
            out.add(
                FriendItem(
                    code = obj.optString("code"),
                    name = obj.optString("name", ""),
                    photo = obj.optString("photo", ""),
                    days = daysSet,
                    nowText = obj.optString("now_text", ""),
                    nowAtMs = nowAtMs,
                    unread = obj.optInt("unread", 0),
                    lastMsg = obj.optString("last", ""),
                )
            )
        }
        return out
    }

    private fun parseRequests(raw: String): Pair<List<FriendRequest>, List<FriendRequest>> {
        val obj = try { JSONObject(raw) } catch (_: Exception) { return emptyList<FriendRequest>() to emptyList() }
        fun parseArr(arr: JSONArray?): List<FriendRequest> {
            val res = mutableListOf<FriendRequest>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    res.add(FriendRequest(o.optString("code"), o.optString("name", ""), o.optString("photo", "")))
                }
            }
            return res
        }
        return parseArr(obj.optJSONArray("incoming")) to parseArr(obj.optJSONArray("outgoing"))
    }

    private fun parseIsoTimestampMs(iso: String): Long {
        if (iso.isBlank()) return 0L
        return try {
            val clean = iso.replace(Regex("""\.\d+"""), "").replace("Z", "+0000")
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).parse(clean)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    // ---------- Export / Import / Password Change ----------
    fun exportFile(kind: String) {
        val s = _state.value
        val td = DateUtils.todayKey()
        val (fileName, mime, bytes) = when (kind) {
            "json" -> Triple("mudhakirati-backup-$td.json", "application/json", ExportUtils.buildBackupJson(_auth.value.userEmail, s).toByteArray(Charsets.UTF_8))
            "csv_tasks" -> Triple("mudhakirati-tasks-$td.csv", "text/csv", ExportUtils.buildCsvTasks(s).toByteArray(Charsets.UTF_8))
            "csv_logs" -> Triple("mudhakirati-study-log-$td.csv", "text/csv", ExportUtils.buildCsvLogs(s).toByteArray(Charsets.UTF_8))
            "xls" -> Triple("mudhakirati-$td.xls", "application/vnd.ms-excel", ExportUtils.buildExcelXml(s).toByteArray(Charsets.UTF_8))
            "ics" -> {
                val ics = ExportUtils.buildIcs(s)
                if (ics == null) {
                    showToast(t("لا توجد مهام غير مكتملة بموعد لتصديرها", "No incomplete tasks with a due date to export"))
                    return
                }
                Triple("mudhakirati-$td.ics", "text/calendar", ics.toByteArray(Charsets.UTF_8))
            }
            else -> return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val uri = ExportUtils.saveToDownloads(appCtx, fileName, mime, bytes)
            withContext(Dispatchers.Main) {
                if (uri != null) showToast("${t("تم تجهيز الملف", "File ready")}: $fileName")
                else showToast(t("تعذّر حفظ الملف", "Could not save file"))
            }
        }
    }

    fun importBackupUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawText = try {
                appCtx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (_: Exception) {
                null
            }
            val parsedObj = try {
                val j = JSONObject(rawText ?: "")
                val d = j.optJSONObject("data") ?: j
                if (!d.has("subjects") || !d.has("tasks") || !d.has("logs")) null else d
            } catch (_: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                if (parsedObj == null) {
                    showToast(t("الملف غير صالح", "Invalid file"))
                    return@withContext
                }
                askConfirm(
                    title = t("نسخة احتياطية", "Backup"),
                    message = t("ستُستبدل بياناتك الحالية بالنسخة. متأكد؟", "Your current data will be replaced by the backup. Sure?"),
                    confirmLabel = t("استبدال البيانات", "Replace data"),
                    isDanger = true,
                ) {
                    val curLang = _state.value.lang
                    val imported = JsonSerializer.migrate(parsedObj, curLang).copy(
                        view = AppTab.HOME,
                        onboarded = true,
                    )
                    _state.value = imported
                    repo.saveAppState(_auth.value.userEmail, imported)
                    setTimerMode(TimerMode.FOCUS)
                    _timer.update { it.copy(subjId = imported.subjects.firstOrNull()?.id ?: "") }
                    closeSheet()
                    showToast(t("تم استيراد النسخة", "Backup imported"))
                }
            }
        }
    }

    fun changePassword(curPw: String, newPw: String, onResult: (Boolean, String) -> Unit) {
        if (newPw.length < 6) {
            onResult(false, t("كلمة المرور 6 أحرف على الأقل", "Password must be at least 6 characters"))
            return
        }
        val email = _auth.value.userEmail
        val users = repo.getUsers()
        val rec = users[email]
        if (rec == null) {
            onResult(false, t("كلمة المرور الحالية غير صحيحة", "Current password is wrong"))
            return
        }
        viewModelScope.launch {
            val h = withContext(Dispatchers.Default) { repo.hashPassword(curPw, rec.salt) }
            if (h != rec.hash) {
                onResult(false, t("كلمة المرور الحالية غير صحيحة", "Current password is wrong"))
                return@launch
            }
            val hasCloud = SupabaseApi.session != null || repo.loadSbRefreshToken().isNotBlank()
            if (hasCloud) {
                val tok = SupabaseApi.getValidToken(repo)
                if (tok.isBlank()) {
                    onResult(false, t("تعذّر تغيير كلمة السر على الحساب السحابي", "Could not change cloud password"))
                    return@launch
                }
                try {
                    SupabaseApi.updatePassword(tok, newPw)
                } catch (e: Exception) {
                    val m = e.message ?: ""
                    val msg = when {
                        m.contains("different", true) || m.contains("same", true) -> t("اختر كلمة سر مختلفة عن القديمة", "Choose a password different from the old one")
                        m.contains("weak", true) || m.contains("short", true) -> t("كلمة المرور 6 أحرف على الأقل", "Password must be at least 6 characters")
                        else -> t("تعذّر تغيير كلمة السر على الحساب السحابي. تأكد من الإنترنت وحاول مرة ثانية.", "Could not change cloud password. Check connection.")
                    }
                    onResult(false, msg)
                    return@launch
                }
            }
            val newSalt = repo.newSalt()
            val newHash = withContext(Dispatchers.Default) { repo.hashPassword(newPw, newSalt) }
            users[email] = LocalUserRecord(newSalt, newHash)
            repo.putUsers(users)
            onResult(true, t("تم تغيير كلمة المرور", "Password changed"))
        }
    }
}
