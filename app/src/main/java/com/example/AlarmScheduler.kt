package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules system alarms so the timer-finished and daily-reminder notifications
 * arrive even when the app is in the background or closed.
 */
object AlarmScheduler {
    const val KIND_TIMER = "timer"
    const val KIND_DAILY = "daily"
    const val TAG_TIMER = "session-end"
    const val TAG_DAILY = "daily-reminder"
    const val KIND_BREAK = "break"
    const val KIND_NEWDAY = "newday"
    const val TAG_BREAK = "break-end"
    const val TAG_NEWDAY = "new-day"
    const val KIND_STREAK = "streak"
    const val TAG_STREAK = "streak-warn"

    private const val PREFS = "mz_alarms"
    private const val REQ_TIMER = 7101
    private const val REQ_DAILY = 7102
    private const val REQ_BREAK = 7103
    private const val REQ_NEWDAY = 7104
    private const val REQ_STREAK = 7105
    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun pending(c: Context, req: Int, kind: String, title: String, body: String): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).apply {
            action = "com.example.ALARM.$kind"
            putExtra("kind", kind)
            putExtra("title", title)
            putExtra("body", body)
        }
        return PendingIntent.getBroadcast(
            c, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(c: Context, req: Int, at: Long, kind: String, title: String, body: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(c, req, kind, title, body)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(c: Context, req: Int, kind: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(c, req, kind, "", ""))
    }

    // ---- focus / break timer ----
    fun scheduleTimer(c: Context, endAt: Long, title: String, body: String) {
        if (endAt <= System.currentTimeMillis()) {
            cancelTimer(c)
            return
        }
        cancelBreak(c)
        prefs(c).edit().putLong("timer_at", endAt).putString("timer_title", title)
            .putString("timer_body", body).apply()
        setAlarm(c, REQ_TIMER, endAt, KIND_TIMER, title, body)
    }

    fun cancelTimer(c: Context) {
        prefs(c).edit().remove("timer_at").remove("chain_ms").apply()
        cancelAlarm(c, REQ_TIMER, KIND_TIMER)
        cancelBreak(c)
    }

    fun clearTimer(c: Context) {
        prefs(c).edit().remove("timer_at").apply()
    }

    // ---- break that starts by itself when a focus session ends in the background ----
    /** Remember how long the break after the running focus session is (0 = none). */
    fun setBreakChain(c: Context, ms: Long, title: String, body: String) {
        prefs(c).edit().putLong("chain_ms", ms).putString("chain_title", title)
            .putString("chain_body", body).apply()
    }

    /** Focus alarm fired: queue the "break is over, go study" alarm. */
    fun onTimerFired(c: Context) {
        val p = prefs(c)
        val ms = p.getLong("chain_ms", 0L)
        p.edit().remove("timer_at").remove("chain_ms").apply()
        if (ms <= 0L) return
        val at = System.currentTimeMillis() + ms
        val title = p.getString("chain_title", "") ?: ""
        val body = p.getString("chain_body", "") ?: ""
        p.edit().putLong("break_at", at).putString("break_title", title).putString("break_body", body).apply()
        setAlarm(c, REQ_BREAK, at, KIND_BREAK, title, body)
    }

    fun clearBreak(c: Context) {
        prefs(c).edit().remove("break_at").apply()
    }

    private fun cancelBreak(c: Context) {
        clearBreak(c)
        cancelAlarm(c, REQ_BREAK, KIND_BREAK)
    }

    // ---- new day (every midnight) ----
    fun scheduleNewDay(c: Context, title: String, body: String) {
        prefs(c).edit().putBoolean("newday_on", true).putString("newday_title", title)
            .putString("newday_body", body).apply()
        setAlarm(c, REQ_NEWDAY, nextDaily(0, 0), KIND_NEWDAY, title, body)
    }

    fun cancelNewDay(c: Context) {
        prefs(c).edit().putBoolean("newday_on", false).apply()
        cancelAlarm(c, REQ_NEWDAY, KIND_NEWDAY)
    }

    /** Called after the midnight alarm fires: queue the next one. */
    fun onNewDayFired(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean("newday_on", false)) return
        scheduleNewDay(c, p.getString("newday_title", "") ?: "", p.getString("newday_body", "") ?: "")
    }

    // ---- streak warning ("you will lose your progress") ----
    /**
     * One alarm at [at]. If the page is not opened again the alarm repeats on the next
     * 2 days with [body2] ("you missed a day"), then stops until the app is opened.
     */
    fun scheduleStreakWarn(c: Context, at: Long, title: String, body: String, body2: String) {
        if (at <= System.currentTimeMillis()) return
        prefs(c).edit().putLong("streak_at", at).putString("streak_title", title)
            .putString("streak_body", body).putString("streak_body2", body2)
            .putInt("streak_chain", 0).apply()
        setAlarm(c, REQ_STREAK, at, KIND_STREAK, title, body)
    }

    fun cancelStreakWarn(c: Context) {
        prefs(c).edit().remove("streak_at").putInt("streak_chain", 0).apply()
        cancelAlarm(c, REQ_STREAK, KIND_STREAK)
    }

    /** The warning fired: queue the next day's reminder (max 2 follow-ups). */
    fun onStreakFired(c: Context) {
        val p = prefs(c)
        val at = p.getLong("streak_at", 0L)
        val chain = p.getInt("streak_chain", 0) + 1
        if (at <= 0L || chain > 2) {
            p.edit().remove("streak_at").putInt("streak_chain", 0).apply()
            return
        }
        var next = at + DAY_MS
        while (next <= System.currentTimeMillis()) next += DAY_MS
        p.edit().putLong("streak_at", next).putInt("streak_chain", chain).apply()
        val body2 = p.getString("streak_body2", "") ?: ""
        setAlarm(c, REQ_STREAK, next, KIND_STREAK, p.getString("streak_title", "") ?: "", body2)
    }

    // ---- daily reminder ----
    fun scheduleDaily(c: Context, hour: Int, minute: Int, title: String, body: String) {
        prefs(c).edit().putBoolean("daily_on", true).putInt("daily_h", hour).putInt("daily_m", minute)
            .putString("daily_title", title).putString("daily_body", body).apply()
        setAlarm(c, REQ_DAILY, nextDaily(hour, minute), KIND_DAILY, title, body)
    }

    fun cancelDaily(c: Context) {
        prefs(c).edit().putBoolean("daily_on", false).apply()
        cancelAlarm(c, REQ_DAILY, KIND_DAILY)
    }

    /** Called after the daily alarm fires: queue tomorrow's. */
    fun onDailyFired(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean("daily_on", false)) return
        scheduleDaily(
            c, p.getInt("daily_h", 18), p.getInt("daily_m", 0),
            p.getString("daily_title", "") ?: "", p.getString("daily_body", "") ?: ""
        )
    }

    private fun nextDaily(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** After reboot or app update: put the saved alarms back. */
    fun restoreAll(c: Context) {
        val p = prefs(c)
        val at = p.getLong("timer_at", 0L)
        if (at > System.currentTimeMillis()) {
            setAlarm(
                c, REQ_TIMER, at, KIND_TIMER,
                p.getString("timer_title", "") ?: "", p.getString("timer_body", "") ?: ""
            )
        } else {
            p.edit().remove("timer_at").apply()
        }
        val bAt = p.getLong("break_at", 0L)
        if (bAt > System.currentTimeMillis()) {
            setAlarm(
                c, REQ_BREAK, bAt, KIND_BREAK,
                p.getString("break_title", "") ?: "", p.getString("break_body", "") ?: ""
            )
        } else {
            p.edit().remove("break_at").apply()
        }
        val sAt = p.getLong("streak_at", 0L)
        if (sAt > System.currentTimeMillis()) {
            val chain = p.getInt("streak_chain", 0)
            setAlarm(
                c, REQ_STREAK, sAt, KIND_STREAK, p.getString("streak_title", "") ?: "",
                (if (chain == 0) p.getString("streak_body", "") else p.getString("streak_body2", "")) ?: ""
            )
        }
        if (p.getBoolean("daily_on", false)) onDailyFired(c)
        onNewDayFired(c)
    }
}
