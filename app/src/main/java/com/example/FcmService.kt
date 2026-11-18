package com.example

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push messages sent by the Supabase Edge Function (data-only messages,
 * so this code always decides how the notification looks, app open or closed).
 */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        save(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val title = d["title"] ?: getString(R.string.app_name)
        val body = d["body"] ?: return
        // While the app is visible the page shows its own toast, so skip the system notification.
        if (MainActivity.inForeground) return
        Notifier.show(applicationContext, title, body, d["tag"] ?: "push")
    }

    companion object {
        private const val PREFS = "fcm"
        private const val KEY = "token"

        private fun save(ctx: Context, token: String) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, token).apply()
        }

        fun savedToken(ctx: Context): String =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

        /** Asks Firebase for the current token and caches it for the web page to read. */
        fun fetchToken(ctx: Context) {
            try {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
                    if (!t.isNullOrEmpty()) save(ctx, t)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
