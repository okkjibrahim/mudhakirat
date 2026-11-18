package com.example.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.AlarmScheduler
import com.example.MainActivity
import com.example.Notifier
import com.example.R
import com.example.model.AppState
import com.example.util.DateUtils
import com.example.util.Strings
import java.util.Calendar

object FeedbackHelper {

    fun playAssetFlipSound(context: Context) {
        try {
            val afd = context.assets.openFd("flip.mp3")
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { player, _, _ ->
                player.release()
                true
            }
            mp.prepareAsync()
            mp.setOnPreparedListener { it.start() }
        } catch (_: Exception) {
        }
    }

    fun playAlertSound(context: Context) {
        try {
            val mp = MediaPlayer.create(context, R.raw.notification_sound) ?: return
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (_: Exception) {
        }
    }

    fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }

    fun showLocalNotification(context: Context, title: String, body: String, tag: String) {
        Notifier.ensureChannel(context)
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                tag.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                .setSound(Notifier.soundUri(context))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(context).notify(tag.hashCode(), notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    fun syncStreakWarningAlarm(context: Context, state: AppState, userLoggedIn: Boolean) {
        if (!userLoggedIn) return
        val n = DateUtils.computeStreak(state)
        if (n <= 0) {
            AlarmScheduler.cancelStreakWarn(context)
            return
        }
        val cal = Calendar.getInstance()
        val nowMs = cal.timeInMillis
        val doneToday = DateUtils.actOn(state, DateUtils.todayKey())
        val at21 = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val at23 = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetAt = when {
            doneToday -> at21.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            nowMs < at21.timeInMillis -> at21.timeInMillis
            nowMs < at23.timeInMillis -> at23.timeInMillis
            else -> at21.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        }
        val dw = Strings.daysWord(n, state.lang)
        val body1 = Strings.pk(
            "ستفقد تقدمك 🔥 ستريكك $n $dw بيضيع عند منتصف الليل. أنجز مهمة أو جلسة قصيرة وتنحفظ.",
            "You are about to lose your progress 🔥 your $n-$dw streak ends at midnight. Finish a task or a short session to keep it.",
            state.lang,
        )
        val body2 = Strings.pk(
            "فاتك يوم وستفقد تقدمك 🔥 ارجع وابدأ ستريك جديد اليوم.",
            "You missed a day and are losing your progress 🔥 come back and start a new streak today.",
            state.lang,
        )
        AlarmScheduler.scheduleStreakWarn(
            context,
            targetAt,
            Strings.pk("مذاكرتي", "Mudhakirati", state.lang),
            body1,
            body2,
        )
    }

    fun syncNewDayAlarm(context: Context, state: AppState) {
        AlarmScheduler.scheduleNewDay(
            context,
            Strings.pk("مذاكرتي", "Mudhakirati", state.lang),
            Strings.pk("يوم جديد 🌅 ابدأ يومك بمذاكرة", "New day 🌅 start it with some studying", state.lang),
        )
    }

    fun syncDailyReminderAlarm(context: Context, state: AppState) {
        if (state.remind.on) {
            val parts = state.remind.time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 18
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            AlarmScheduler.scheduleDaily(
                context,
                h,
                m,
                Strings.pk("مذاكرتي", "Mudhakirati", state.lang),
                Strings.remindBody(state),
            )
        } else {
            AlarmScheduler.cancelDaily(context)
        }
    }
}
