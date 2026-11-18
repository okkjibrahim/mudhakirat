package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.model.AppState
import com.example.model.TimerMode
import com.example.model.TimerRuntime
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class LocalUserRecord(
    val salt: String,
    val hash: String,
)

data class StoredSbSession(
    val refreshToken: String,
)

class LocalRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mudhakirati_native_prefs", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    companion object {
        private const val KEY_DATA = "mudhakirati-v1"
        private const val USERS_K = "mudhakirati-users"
        private const val SESS_K = "mudhakirati-session"
        private const val LANG_K = "mudhakirati-lang"
        private const val FS_K = "mudhakirati-fs"
        private const val SCH_V_K = "mz-schv"
        private const val SB_K = "mudhakirati-sb"
        private const val TH_K = "mudhakirati-th"
        private const val PAL_K = "mudhakirati-pal"
        private const val FCM_SIG_K = "mz-fcm"
        private const val PHS_SIG_K = "mz-phs"
    }

    fun getLanguage(): String = prefs.getString(LANG_K, "ar").let { if (it == "en") "en" else "ar" }
    fun setLanguage(lang: String) {
        prefs.edit().putString(LANG_K, if (lang == "en") "en" else "ar").apply()
    }

    fun getFontScaleIndex(): Int = prefs.getInt(FS_K, 2).coerceIn(0, 4)
    fun setFontScaleIndex(idx: Int) {
        prefs.edit().putInt(FS_K, idx.coerceIn(0, 4)).apply()
    }

    fun getStoredTheme(): Pair<String, String> {
        val th = prefs.getString(TH_K, "custom") ?: "custom"
        val pal = prefs.getString(PAL_K, "white-yellow") ?: "white-yellow"
        return th to pal
    }

    fun saveStoredTheme(theme: String, palette: String) {
        prefs.edit().putString(TH_K, theme).putString(PAL_K, palette).apply()
    }

    fun isScheduleDayView(): Boolean = prefs.getString(SCH_V_K, "week") == "day"
    fun setScheduleDayView(dayView: Boolean) {
        prefs.edit().putString(SCH_V_K, if (dayView) "day" else "week").apply()
    }

    fun saveSession(email: String) {
        prefs.edit().putString(SESS_K, email).apply()
    }

    fun loadSession(): String = prefs.getString(SESS_K, "") ?: ""

    fun clearSession() {
        prefs.edit().remove(SESS_K).apply()
    }

    fun saveSbRefreshToken(rt: String) {
        prefs.edit().putString(SB_K, rt).apply()
    }

    fun loadSbRefreshToken(): String = prefs.getString(SB_K, "") ?: ""

    fun clearSbSession() {
        prefs.edit().remove(SB_K).apply()
    }

    fun getUsers(): MutableMap<String, LocalUserRecord> {
        val out = mutableMapOf<String, LocalUserRecord>()
        val raw = prefs.getString(USERS_K, null) ?: return out
        return try {
            val obj = JSONObject(raw)
            obj.keys().forEach { email ->
                val u = obj.optJSONObject(email)
                if (u != null) {
                    out[email] = LocalUserRecord(u.optString("salt"), u.optString("hash"))
                }
            }
            out
        } catch (_: Exception) {
            out
        }
    }

    fun putUsers(users: Map<String, LocalUserRecord>) {
        val obj = JSONObject()
        users.forEach { (email, rec) ->
            obj.put(email, JSONObject().apply {
                put("salt", rec.salt)
                put("hash", rec.hash)
            })
        }
        prefs.edit().putString(USERS_K, obj.toString()).apply()
    }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(Charsets.UTF_8), 100_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun loadAppState(email: String): AppState? {
        val raw = prefs.getString("$KEY_DATA:$email", null) ?: return null
        return try {
            JsonSerializer.migrate(JSONObject(raw), getLanguage())
        } catch (_: Exception) {
            null
        }
    }

    fun saveAppState(email: String, state: AppState) {
        if (email.isBlank()) return
        val json = JsonSerializer.stateToJson(state).toString()
        prefs.edit()
            .putString("$KEY_DATA:$email", json)
            .putString(TH_K, state.theme)
            .putString(PAL_K, state.palette)
            .apply()
    }

    fun getLocalTimestamp(email: String): Long = prefs.getLong("mudhakirati-ts:$email", 0L)
    fun setLocalTimestamp(email: String, ts: Long) {
        prefs.edit().putLong("mudhakirati-ts:$email", ts).apply()
    }

    fun adoptLegacyAnonymousDataIfAny(email: String) {
        val old = prefs.getString(KEY_DATA, null)
        if (!old.isNullOrBlank() && !prefs.contains("$KEY_DATA:$email")) {
            prefs.edit().putString("$KEY_DATA:$email", old).remove(KEY_DATA).apply()
        }
    }

    fun saveTimer(email: String, tm: TimerRuntime) {
        if (email.isBlank()) return
        val obj = JSONObject().apply {
            put("m", tm.mode.id)
            put("r", if (tm.running) 1 else 0)
            put("e", tm.endAt)
            put("l", tm.leftMs ?: (tm.leftSec * 1000L))
            put("tot", tm.totalSec)
            put("s", tm.subjId)
        }
        prefs.edit().putString("$KEY_DATA:timer:$email", obj.toString()).apply()
    }

    fun loadTimer(email: String, fallbackFocusSec: Int, fallbackBrkSec: Int): TimerRuntime? {
        if (email.isBlank()) return null
        val raw = prefs.getString("$KEY_DATA:timer:$email", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val mode = if (obj.optString("m") == "break") TimerMode.BREAK else TimerMode.FOCUS
            val total = obj.optInt("tot", 0).takeIf { it > 0 } ?: if (mode == TimerMode.FOCUS) fallbackFocusSec else fallbackBrkSec
            val running = obj.optInt("r", 0) == 1
            val endAt = obj.optLong("e", 0L)
            val leftMsRaw = obj.optLong("l", 0L)
            val subj = obj.optString("s", "")
            if (running && endAt > 0L) {
                val left = kotlin.math.min(total, kotlin.math.max(0, kotlin.math.ceil((endAt - System.currentTimeMillis()) / 1000.0).toInt()))
                TimerRuntime(mode = mode, running = true, endAt = endAt, leftSec = left, leftMs = null, totalSec = total, subjId = subj)
            } else {
                val leftMs = if (leftMsRaw > 0L) leftMsRaw else null
                val left = if (leftMs != null) kotlin.math.min(total, kotlin.math.ceil(leftMs / 1000.0).toInt()) else total
                TimerRuntime(mode = mode, running = false, endAt = 0L, leftSec = left, leftMs = leftMs, totalSec = total, subjId = subj)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getUserPref(email: String, key: String, default: String = ""): String =
        prefs.getString("$KEY_DATA:$key:$email", default) ?: default

    fun setUserPref(email: String, key: String, value: String) {
        prefs.edit().putString("$KEY_DATA:$key:$email", value).apply()
    }

    fun getAllNicknames(email: String): MutableMap<String, String> {
        val raw = getUserPref(email, "nick", "{}")
        val out = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(raw)
            obj.keys().forEach { k ->
                val v = obj.optString(k)
                if (v.isNotBlank()) out[k] = v
            }
        } catch (_: Exception) {}
        return out
    }

    fun setNickname(email: String, code: String, nick: String) {
        val map = getAllNicknames(email)
        if (nick.isBlank()) map.remove(code) else map[code] = nick
        setUserPref(email, "nick", JSONObject(map as Map<*, *>).toString())
    }

    fun getFcmSig(): String = prefs.getString(FCM_SIG_K, "") ?: ""
    fun setFcmSig(sig: String) { prefs.edit().putString(FCM_SIG_K, sig).apply() }
    fun clearFcmSig() { prefs.edit().remove(FCM_SIG_K).apply() }

    fun getPhotoSig(): String = prefs.getString(PHS_SIG_K, "") ?: ""
    fun setPhotoSig(sig: String) { prefs.edit().putString(PHS_SIG_K, sig).apply() }

    fun encodeSquareAvatar(context: Context, uri: Uri, sizePx: Int = 256, quality: Int = 82): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val m = minOf(src.width, src.height)
            val x = (src.width - m) / 2
            val y = (src.height - m) / 2
            val cropped = Bitmap.createBitmap(src, x, y, m, m)
            val scaled = Bitmap.createScaledBitmap(cropped, sizePx, sizePx, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (cropped !== src) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            src.recycle()
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun makeThumbnailDataUrl(dataUrl: String, sizePx: Int = 96, quality: Int = 70): String {
        if (dataUrl.isBlank()) return ""
        return try {
            val idx = dataUrl.indexOf("base64,")
            val b64 = if (idx >= 0) dataUrl.substring(idx + 7) else dataUrl
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ""
            val scaled = Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled !== src) scaled.recycle()
            src.recycle()
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}
