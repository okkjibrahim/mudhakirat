package com.example.data

import com.example.model.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SupabaseException(
    val code: Int,
    val errorCode: String,
    override val message: String,
) : Exception(message)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtMs: Long,
    val email: String = "",
)

object SupabaseApi {
    private const val BASE_URL = "https://gojcpksvjhhulvmyzaxf.supabase.co"
    private const val API_KEY = "sb_publishable_iaERyQvI9Rx-vrvTM-A0FA_8lKF0J3r"

    @Volatile
    var session: SupabaseSession? = null

    fun isConfigured(): Boolean = BASE_URL.isNotBlank() && API_KEY.isNotBlank()

    private suspend fun requestRaw(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: Any? = null,
    ): String = withContext(Dispatchers.IO) {
        val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", API_KEY)
            setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) {
                val errObj = try { JSONObject(text) } catch (_: Exception) { null }
                val errCode = errObj?.optString("error_code")?.ifEmpty { errObj.optString("code") } ?: ""
                val msg = errObj?.let {
                    it.optString("msg").ifEmpty {
                        it.optString("error_description").ifEmpty {
                            it.optString("message").ifEmpty { text }
                        }
                    }
                } ?: text
                throw SupabaseException(code, errCode, msg)
            }
            text
        } finally {
            conn.disconnect()
        }
    }

    private fun storeFromAuthJson(j: JSONObject): SupabaseSession? {
        val at = j.optString("access_token")
        val uid = j.optJSONObject("user")?.optString("id") ?: ""
        if (at.isBlank() || uid.isBlank()) return null
        val rt = j.optString("refresh_token")
        val exp = System.currentTimeMillis() + j.optLong("expires_in", 3600L) * 1000L
        val em = j.optJSONObject("user")?.optString("email") ?: ""
        val s = SupabaseSession(accessToken = at, refreshToken = rt, userId = uid, expiresAtMs = exp, email = em)
        session = s
        return s
    }

    suspend fun getValidToken(repo: LocalRepository): String {
        val cur = session
        if (cur != null && cur.expiresAtMs > System.currentTimeMillis() + 60_000L) {
            return cur.accessToken
        }
        val rt = cur?.refreshToken?.ifBlank { null } ?: repo.loadSbRefreshToken()
        if (rt.isBlank()) return ""
        return try {
            val txt = requestRaw(
                "/auth/v1/token?grant_type=refresh_token",
                method = "POST",
                body = JSONObject().put("refresh_token", rt),
            )
            val s = storeFromAuthJson(JSONObject(txt))
            if (s != null && s.refreshToken.isNotBlank()) repo.saveSbRefreshToken(s.refreshToken)
            s?.accessToken ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun signIn(email: String, password: String): JSONObject {
        val txt = requestRaw(
            "/auth/v1/token?grant_type=password",
            method = "POST",
            body = JSONObject().put("email", email).put("password", password),
        )
        return JSONObject(txt).also { storeFromAuthJson(it) }
    }

    suspend fun signUp(email: String, password: String): JSONObject {
        val txt = requestRaw(
            "/auth/v1/signup",
            method = "POST",
            body = JSONObject().put("email", email).put("password", password),
        )
        return JSONObject(txt).also { storeFromAuthJson(it) }
    }

    suspend fun verifyOtp(type: String, email: String, tokenCode: String): JSONObject {
        val txt = requestRaw(
            "/auth/v1/verify",
            method = "POST",
            body = JSONObject().put("type", type).put("email", email).put("token", tokenCode),
        )
        return JSONObject(txt).also { storeFromAuthJson(it) }
    }

    suspend fun resendSignup(email: String) {
        requestRaw(
            "/auth/v1/resend",
            method = "POST",
            body = JSONObject().put("type", "signup").put("email", email),
        )
    }

    suspend fun recover(email: String) {
        requestRaw(
            "/auth/v1/recover",
            method = "POST",
            body = JSONObject().put("email", email),
        )
    }

    suspend fun updatePassword(accessToken: String, newPassword: String): JSONObject {
        val txt = requestRaw(
            "/auth/v1/user",
            method = "PUT",
            token = accessToken,
            body = JSONObject().put("password", newPassword),
        )
        return JSONObject(txt)
    }

    suspend fun pullUserData(repo: LocalRepository, email: String): Boolean {
        val s = session ?: return false
        return try {
            val uidEnc = URLEncoder.encode(s.userId, "UTF-8")
            val txt = requestRaw(
                "/rest/v1/user_data?select=data,ts&user_id=eq.$uidEnc",
                method = "GET",
                token = s.accessToken,
            )
            val arr = JSONArray(txt)
            val row = arr.optJSONObject(0) ?: return false
            val data = row.optJSONObject("data") ?: return false
            val serverTs = row.optLong("ts", 0L)
            val localTs = repo.getLocalTimestamp(email)
            if (serverTs > localTs) {
                val migrated = JsonSerializer.migrate(data, repo.getLanguage())
                repo.saveAppState(email, migrated)
                repo.setLocalTimestamp(email, serverTs)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun pushUserData(repo: LocalRepository, state: AppState, ts: Long): Long? {
        val tok = getValidToken(repo)
        if (tok.isBlank()) return null
        val payload = JSONObject()
            .put("p_data", JsonSerializer.stateToJson(state))
            .put("p_ts", ts)
        val txt = requestRaw("/rest/v1/rpc/save_user_data", method = "POST", token = tok, body = payload)
        return txt.trim().toLongOrNull()
    }

    suspend fun rpc(repo: LocalRepository, fn: String, body: JSONObject = JSONObject()): String {
        val tok = getValidToken(repo)
        if (tok.isBlank()) throw SupabaseException(401, "offline", "Not authenticated")
        return requestRaw("/rest/v1/rpc/$fn", method = "POST", token = tok, body = body)
    }

    fun isMissingRpc(e: Throwable): Boolean {
        if (e !is SupabaseException) return false
        return e.errorCode == "PGRST202" || e.errorCode == "42883" ||
            Regex("could not find the function|does not exist", RegexOption.IGNORE_CASE).containsMatchIn(e.message)
    }
}
