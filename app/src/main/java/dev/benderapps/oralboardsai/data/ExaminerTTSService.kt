package dev.benderapps.oralboardsai.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Hosted speech service that caches stem audio so repeated openings do not
 * require regenerating the same file on every session.
 */
class ExaminerTTSService {
    enum class Kind(val raw: String) {
        Stem("stem"),
        Prompt("prompt"),
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // A cold-started Render instance plus synthesis can take a while.
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun synthesize(
        text: String,
        kind: Kind = Kind.Prompt,
        caseId: String? = null,
    ): ByteArray = withContext(Dispatchers.IO) {
        // `caseId` is only sent for stem audio so the server can cache the
        // opening case narration independently from transient follow-up prompts.
        val body = JSONObject()
            .put("text", text)
            .put("kind", kind.raw)
        if (kind == Kind.Stem && caseId != null) {
            body.put("caseId", caseId)
        }

        val request = Request.Builder()
            .url(BASE_URL + "tts")
            .post(body.toString().toRequestBody(JSON))
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("TTS failed with HTTP ${response.code}")
            }
            response.body?.bytes() ?: throw IOException("TTS returned an empty body")
        }
    }

    /** Pre-warms the Render instance to avoid a slow first case. Failures are ignored. */
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        val client = http.newBuilder().callTimeout(5, TimeUnit.SECONDS).build()
        try {
            client.newCall(Request.Builder().url(BASE_URL + "healthz").build())
                .execute()
                .close()
        } catch (_: Exception) {
            // Just a warm-up ping.
        }
    }

    private companion object {
        const val BASE_URL = "https://oralboards-tts.onrender.com/"
        val JSON = "application/json".toMediaType()
    }
}
