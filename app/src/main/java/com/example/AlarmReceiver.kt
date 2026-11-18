package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val title = intent.getStringExtra("title") ?: ""
        val body = intent.getStringExtra("body") ?: ""

        when (kind) {
            AlarmScheduler.KIND_DAILY -> AlarmScheduler.onDailyFired(context)     // queue tomorrow's reminder
            AlarmScheduler.KIND_NEWDAY -> AlarmScheduler.onNewDayFired(context)   // queue the next midnight
            AlarmScheduler.KIND_BREAK -> AlarmScheduler.clearBreak(context)
            AlarmScheduler.KIND_STREAK -> AlarmScheduler.onStreakFired(context)   // queue the next follow-up
            else -> AlarmScheduler.onTimerFired(context)                          // focus ended: start the break countdown
        }

        // While the app is open the page shows its own toast/notification.
        if (MainActivity.inForeground) return

        when (kind) {
            AlarmScheduler.KIND_NEWDAY -> Notifier.showQuiet(context, title, body, AlarmScheduler.TAG_NEWDAY)
            AlarmScheduler.KIND_DAILY -> Notifier.show(context, title, body, AlarmScheduler.TAG_DAILY)
            AlarmScheduler.KIND_BREAK -> Notifier.show(context, title, body, AlarmScheduler.TAG_BREAK)
            AlarmScheduler.KIND_STREAK -> Notifier.show(context, title, body, AlarmScheduler.TAG_STREAK)
            else -> Notifier.show(context, title, body, AlarmScheduler.TAG_TIMER)
        }
    }
}
