package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

import com.example.model.AppState
import com.example.model.ReviewItem
import com.example.model.Subject

data class PlanInfo(
    val days: Int,
    val left: Int,
    val done: Boolean,
    val pct: Int,
    val name: String,
)

data class ReviewInfo(
    val left: Int,
    val rem: Double,
    val done: Boolean,
    val share: Double,
    val fin: Boolean,
    val late: Boolean,
    val elapsed: Int,
)

data class MonthDayEntry(
    val key: String,
    val dayOfMonth: Int,
    val minutes: Int,
    val isFuture: Boolean,
    val isToday: Boolean,
)

data class MonthStats(
    val year: Int,
    val month: Int,
    val numDays: Int,
    val isCurrent: Boolean,
    val elapsedDays: Int,
    val days: List<MonthDayEntry>,
    val totalMin: Int,
    val maxMin: Int,
    val activeDays: Int,
    val goalDays: Int,
    val bestDay: MonthDayEntry?,
    val longestStreak: Int,
    val sessions: Int,
    val bySubj: Map<String, Int>,
    val prevMonthMin: Int,
    val goalMin: Int,
    val label: String,
    val canGoPrev: Boolean,
)

object DateUtils {
    private val keyFmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun uid(): String = UUID.randomUUID().toString().replace("-", "").take(7)

    fun dateKey(cal: Calendar): String = keyFmt.get()!!.format(cal.time)

    fun todayKey(): String = dateKey(Calendar.getInstance())

    fun addDaysKey(offset: Int, baseKey: String? = null): String {
        val cal = Calendar.getInstance()
        if (baseKey != null) {
            parseDateKey(baseKey)?.let { cal.time = it }
        }
        cal.add(Calendar.DAY_OF_YEAR, offset)
        return dateKey(cal)
    }

    fun parseDateKey(key: String): Date? {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(key)) return null
        return try {
            keyFmt.get()!!.parse(key)
        } catch (_: Exception) {
            null
        }
    }

    fun isValidDateKey(key: String): Boolean = parseDateKey(key) != null

    fun daysBetween(startKey: String, endKey: String): Int {
        val s = parseDateKey(startKey) ?: return 0
        val e = parseDateKey(endKey) ?: return 0
        return ((e.time - s.time) / 86_400_000.0).roundToInt()
    }

    fun localeFor(lang: String, arDigits: Boolean): Locale {
        return if (lang == "ar") {
            if (arDigits) Locale.forLanguageTag("ar-IQ-u-nu-arab")
            else Locale.forLanguageTag("ar-IQ-u-nu-latn")
        } else {
            Locale.UK
        }
    }

    fun formatDateShort(key: String, lang: String, arDigits: Boolean): String {
        val d = parseDateKey(key) ?: return key
        val fmt = SimpleDateFormat("d MMM", localeFor(lang, arDigits))
        return applyDigits(fmt.format(d), lang, arDigits)
    }

    fun formatDateLong(key: String, lang: String, arDigits: Boolean): String {
        val d = parseDateKey(key) ?: return key
        val fmt = SimpleDateFormat("d MMM yyyy", localeFor(lang, arDigits))
        return applyDigits(fmt.format(d), lang, arDigits)
    }

    fun formatHeaderDate(lang: String, arDigits: Boolean): String {
        val fmt = SimpleDateFormat("EEEE، d MMMM", localeFor(lang, arDigits))
        val raw = if (lang == "ar") fmt.format(Date()) else SimpleDateFormat("EEEE, d MMMM", Locale.UK).format(Date())
        return applyDigits(raw, lang, arDigits)
    }

    fun formatFullPrintDate(lang: String, arDigits: Boolean): String {
        val pattern = if (lang == "ar") "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
        val fmt = SimpleDateFormat(pattern, localeFor(lang, arDigits))
        return applyDigits(fmt.format(Date()), lang, arDigits)
    }

    fun formatWeekdayLong(key: String, lang: String, arDigits: Boolean): String {
        val d = parseDateKey(key) ?: return key
        return SimpleDateFormat("EEEE", localeFor(lang, arDigits)).format(d)
    }

    fun formatWeekdayShort(key: String, lang: String, arDigits: Boolean): String {
        val d = parseDateKey(key) ?: return key
        return SimpleDateFormat("EEE", localeFor(lang, arDigits)).format(d)
    }

    fun formatDayMonthNum(key: String, lang: String, arDigits: Boolean): String {
        val d = parseDateKey(key) ?: return key
        return applyDigits(SimpleDateFormat("d/M", localeFor(lang, arDigits)).format(d), lang, arDigits)
    }

    fun formatClock12(lang: String, arDigits: Boolean): String {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val pm = h >= 12
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (lang == "ar") (if (pm) "م" else "ص") else (if (pm) "pm" else "am")
        return applyDigits("$h12:${m.toString().padStart(2, '0')} $suffix", lang, arDigits)
    }

    fun formatTime12(hhmm: String, lang: String, arDigits: Boolean): String {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 18
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val pm = h >= 12
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (lang == "ar") (if (pm) "م" else "ص") else (if (pm) "PM" else "AM")
        return applyDigits("$h12:${m.toString().padStart(2, '0')} $suffix", lang, arDigits)
    }

    fun mmss(sec: Int): String {
        val s = max(0, sec)
        return "%02d:%02d".format(Locale.US, s / 60, s % 60)
    }

    fun minsOn(state: AppState, dateKey: String, subjId: String = ""): Int {
        var sum = 0
        for (l in state.logs) {
            if (l.date == dateKey && (subjId.isEmpty() || l.subj == subjId)) sum += l.min
        }
        return sum
    }

    fun weekMins(state: AppState, subjId: String = ""): Int {
        var sum = 0
        for (i in 0..6) sum += minsOn(state, addDaysKey(-i), subjId)
        return sum
    }

    fun actOn(state: AppState, dateKey: String): Boolean =
        minsOn(state, dateKey) > 0 || state.act.contains(dateKey)

    fun computeStreak(state: AppState): Int {
        var n = 0
        var i = if (actOn(state, todayKey())) 0 else 1
        while (i < 450 && actOn(state, addDaysKey(-i))) {
            n++
            i++
        }
        return n
    }

    fun computeStreakFromSet(days: Set<String>): Int {
        val td = todayKey()
        var n = 0
        var i = if (days.contains(td)) 0 else 1
        while (i < 450 && days.contains(addDaysKey(-i))) {
            n++
            i++
        }
        return n
    }

    fun computeSharedStreak(mine: Set<String>, other: Set<String>): Int {
        val td = todayKey()
        var n = 0
        var i = if (mine.contains(td) && other.contains(td)) 0 else 1
        while (i < 450) {
            val d = addDaysKey(-i)
            if (mine.contains(d) && other.contains(d)) {
                n++
                i++
            } else break
        }
        return n
    }

    fun myActiveDays(state: AppState): List<String> {
        val set = HashSet<String>(state.act)
        for (l in state.logs) {
            if (l.min > 0) set.add(l.date)
        }
        return set.sorted().takeLast(120)
    }

    fun computeLongestStreak(state: AppState): Int {
        val set = HashSet<String>(state.act)
        for (l in state.logs) {
            if (l.min > 0) set.add(l.date)
        }
        val dayNums = set.mapNotNull { key ->
            parseDateKey(key)?.let { (it.time / 86_400_000L).toInt() }
        }.sorted()
        var best = 0
        var run = 0
        for (i in dayNums.indices) {
            run = if (i > 0 && dayNums[i] - dayNums[i - 1] == 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    fun planInfo(state: AppState): PlanInfo? {
        val p = state.plan ?: return null
        if (p.days <= 0) return null
        val elapsed = max(0, daysBetween(p.start, todayKey()))
        val left = max(0, p.days - elapsed)
        val pct = min(100, ((elapsed.toDouble() / p.days) * 100.0).roundToInt())
        return PlanInfo(p.days, left, left == 0, pct, p.name)
    }

    fun partsTotalHours(subject: Subject): Pair<Double, Int> {
        var h = 0.0
        var lec = 0
        for (p in subject.parts) {
            h += p.hours
            lec += p.lec
        }
        return h to lec
    }

    fun subjectRemainingHours(subject: Subject): Double =
        max(0.0, partsTotalHours(subject).first - subject.doneH)

    fun subjectDayShare(subject: Subject, daysLeft: Int): Double {
        val td = todayKey()
        if (subject.day != null && subject.day.date == td) return subject.day.amt
        return subjectRemainingHours(subject) / max(1, daysLeft)
    }

    fun reviewInfo(r: ReviewItem): ReviewInfo {
        val td = todayKey()
        val elapsed = max(0, daysBetween(r.start, td))
        val rawLeft = r.days + r.ext - elapsed
        val rem = max(0.0, r.hours - r.doneH)
        val doneToday = r.day != null && r.day.date == td
        val share = if (doneToday) r.day!!.amt else min(rem, rem / max(1, rawLeft))
        return ReviewInfo(
            left = max(0, rawLeft),
            rem = rem,
            done = doneToday,
            share = share,
            fin = rem <= 0.0 && !doneToday,
            late = rawLeft <= 0 && rem > 0.0,
            elapsed = elapsed,
        )
    }

    fun monthStats(state: AppState, monthOffset: Int): MonthStats {
        val now = Calendar.getInstance()
        val first = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val y = first.get(Calendar.YEAR)
        val m = first.get(Calendar.MONTH)
        val nd = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cur = monthOffset == 0
        val elapsed = if (cur) now.get(Calendar.DAY_OF_MONTH) else nd
        val td = todayKey()
        val lo = dateKey(first)
        val hiCal = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, nd) }
        val hi = dateKey(hiCal)

        val byDay = HashMap<String, Int>()
        val bySub = HashMap<String, Int>()
        var sess = 0
        for (l in state.logs) {
            if (l.date in lo..hi) {
                byDay[l.date] = (byDay[l.date] ?: 0) + l.min
                bySub[l.subj] = (bySub[l.subj] ?: 0) + l.min
                sess++
            }
        }

        val days = ArrayList<MonthDayEntry>(nd)
        var tot = 0
        var maxM = 0
        var act = 0
        var gd = 0
        var best: MonthDayEntry? = null
        var run = 0
        var longest = 0
        val goal = state.settings.goal

        for (d in 1..nd) {
            val c = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
            val k = dateKey(c)
            val v = byDay[k] ?: 0
            val entry = MonthDayEntry(k, d, v, cur && d > elapsed, k == td)
            days.add(entry)
            tot += v
            if (v > maxM) maxM = v
            if (v > 0) {
                act++
                run++
                if (run > longest) longest = run
                if (best == null || v > best.minutes) best = entry
            } else {
                run = 0
            }
            if (goal > 0 && v >= goal) gd++
        }

        val prevFirst = (first.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val pnd = prevFirst.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lim = min(elapsed, pnd)
        val plo = dateKey(prevFirst)
        val phi = dateKey((prevFirst.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, lim) })
        var prevTot = 0
        for (l in state.logs) {
            if (l.date in plo..phi) prevTot += l.min
        }

        val early = state.logs.minOfOrNull { it.date }
        val ey = early?.take(4)?.toIntOrNull() ?: now.get(Calendar.YEAR)
        val em = (early?.substring(5, 7)?.toIntOrNull()?.minus(1)) ?: now.get(Calendar.MONTH)
        val loc = localeFor(state.lang, state.arDigits)
        val label = applyDigits(SimpleDateFormat("MMMM yyyy", loc).format(first.time), state.lang, state.arDigits)

        return MonthStats(
            year = y,
            month = m,
            numDays = nd,
            isCurrent = cur,
            elapsedDays = elapsed,
            days = days,
            totalMin = tot,
            maxMin = maxM,
            activeDays = act,
            goalDays = gd,
            bestDay = best,
            longestStreak = longest,
            sessions = sess,
            bySubj = bySub,
            prevMonthMin = prevTot,
            goalMin = goal,
            label = label,
            canGoPrev = (y * 12 + m) > (ey * 12 + em),
        )
    }

    fun weekStart(offset: Int, lang: String): Calendar {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun .. 6=Sat
        val diff = if (lang == "ar") (dow + 1) % 7 else (dow + 6) % 7
        cal.add(Calendar.DAY_OF_YEAR, -diff + 7 * offset)
        return cal
    }

    fun applyDigits(input: String, lang: String, arDigits: Boolean): String {
        if (lang != "ar" || !arDigits) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') sb.append(('٠'.code + (ch - '0')).toChar()) else sb.append(ch)
        }
        return sb.toString()
    }

    fun normSearch(s: String): String {
        return s.lowercase(Locale.ROOT)
            .replace(Regex("[\u064B-\u065F\u0670\u0640]"), "")
            .replace(Regex("[\u0623\u0625\u0622]"), "\u0627")
            .replace('\u0649', '\u064A')
            .replace('\u0629', '\u0647')
            .trim()
    }

    fun todayShareTotal(state: AppState): Double {
        val info = planInfo(state) ?: return 0.0
        val days = max(1, info.left)
        val td = todayKey()
        var tot = 0.0
        for (s in state.subjects) {
            if (partsTotalHours(s).first > 0.0 && !(s.day != null && s.day.date == td) && subjectRemainingHours(s) > 0.0) {
                tot += subjectRemainingHours(s) / days
            }
        }
        return tot
    }

    fun nowActivityDuration(text: String): Int {
        val m = Regex("""(\d+)\s*(?:د|min)\s*$""").find(text)
        return m?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    fun isNowActive(text: String, atMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (text.isBlank() || atMs <= 0L) return false
        val d = nowActivityDuration(text)
        val windowMs = if (d > 0) (d + 30) * 60_000L else 10_800_000L
        return abs(nowMs - atMs) >= 0 && (nowMs - atMs) < windowMs
    }

    fun formatAgo(atMs: Long, lang: String, arDigits: Boolean, nowMs: Long = System.currentTimeMillis()): String {
        val mins = max(0, ((nowMs - atMs) / 60_000.0).roundToInt())
        val raw = when {
            mins < 1 -> if (lang == "ar") "الآن" else "now"
            mins < 60 -> if (lang == "ar") "قبل $mins د" else "$mins min ago"
            else -> {
                val h = mins / 60
                if (lang == "ar") "قبل $h س" else "$h h ago"
            }
        }
        return applyDigits(raw, lang, arDigits)
    }

    fun greetingIndex(): Int {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h in 5..11 -> 0
            h in 12..16 -> 1
            h in 17..20 -> 2
            else -> 3
        }
    }
}
