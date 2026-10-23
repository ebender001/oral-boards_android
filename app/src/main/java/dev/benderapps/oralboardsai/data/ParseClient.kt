package dev.benderapps.oralboardsai.data

import dev.benderapps.oralboardsai.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

/** A Parse Server error (`{"code": ..., "error": "..."}`) or a transport failure. */
class ParseException(
    val code: Int,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val OBJECT_NOT_FOUND = 101
        const val CONNECTION_FAILED = 100
    }
}

/**
 * Minimal Parse REST client. The app only ever calls Cloud Code functions, so
 * this talks to `/functions/<name>` directly instead of pulling in the full SDK.
 */
object ParseClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // submitOralResponse waits on an LLM round trip.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = BuildConfig.PARSE_APP_ID.isNotEmpty() &&
            BuildConfig.PARSE_CLIENT_KEY.isNotEmpty() &&
            BuildConfig.PARSE_SERVER_URL.isNotEmpty()

    /** Calls a Cloud Code function and returns its `result` object. */
    suspend fun callFunction(name: String, params: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val url = BuildConfig.PARSE_SERVER_URL.trimEnd('/') + "/functions/" + name
            val request = Request.Builder()
                .url(url)
                .header("X-Parse-Application-Id", BuildConfig.PARSE_APP_ID)
                .header("X-Parse-Client-Key", BuildConfig.PARSE_CLIENT_KEY)
                .post(params.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val json = try {
                        JSONObject(body)
                    } catch (e: JSONException) {
                        throw ParseException(
                            code = response.code,
                            message = "Unexpected server response (HTTP ${response.code})",
                            cause = e,
                        )
                    }

                    if (!response.isSuccessful || json.has("error")) {
                        throw ParseException(
                            code = json.optInt("code", response.code),
                            message = json.optString("error", "HTTP ${response.code}"),
                        )
                    }

                    json.optJSONObject("result") ?: JSONObject()
                }
            } catch (e: IOException) {
                throw ParseException(
                    code = ParseException.CONNECTION_FAILED,
                    message = e.message ?: "Network connection failed",
                    cause = e,
                )
            }
        }
}

/**
 * Whether a failure looks like a transient network/server problem worth one
 * automatic retry, as opposed to a deterministic request error.
 */
fun Throwable.isTransientRequestFailure(): Boolean {
    if (this is java.io.IOException) return true
    if (this is ParseException && code == ParseException.CONNECTION_FAILED) return true

    val text = (message ?: "").lowercase()
    return listOf(
        "timed out", "timeout", "network", "connection", "offline", "internet",
        "temporarily unavailable", "internal server error", "bad gateway",
        "service unavailable", "gateway timeout", "500", "502", "503", "504",
    ).any { text.contains(it) }
}
