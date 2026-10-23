package dev.benderapps.oralboardsai.data

import android.util.Log
import org.json.JSONObject

private const val TAG = "OralExamService"

class OralExamService(private val preferences: AppPreferences) {
    val clientInstanceId: String
        get() = preferences.clientInstanceId

    suspend fun startCase(
        specialty: String,
        caseDomain: String?,
        caseId: String?,
        requiredMustCoverPoints: Int,
        allowedMajorErrors: Int,
        allowedMinorErrors: Int,
        maxTurns: Int,
    ): StartCaseResponse {
        // The persisted client identifier lets the server avoid repeating
        // recently served cases for this installation.
        val params = JSONObject()
            .put("specialty", specialty)
            .put("clientInstanceId", clientInstanceId)
            .put("requiredMustCoverPoints", requiredMustCoverPoints)
            .put("allowedMajorErrors", allowedMajorErrors)
            .put("allowedMinorErrors", allowedMinorErrors)
            .put("maxTurns", maxTurns)
        caseDomain?.let { params.put("caseDomain", it) }
        caseId?.let { params.put("caseId", it) }

        try {
            return StartCaseResponse.from(ParseClient.callFunction("startOralCase", params))
        } catch (e: Exception) {
            Log.w(TAG, "startCase failed", e)
            throw e
        }
    }

    suspend fun submitResponse(sessionId: String, response: String): SubmitResponseResult {
        // The server owns cumulative grading state; the client sends only the
        // latest response text and receives the updated case state back.
        val params = JSONObject()
            .put("sessionId", sessionId)
            .put("responseText", response)

        try {
            return SubmitResponseResult.from(ParseClient.callFunction("submitOralResponse", params))
        } catch (e: Exception) {
            Log.w(TAG, "submitResponse failed", e)
            throw e
        }
    }

    suspend fun abortCase(sessionId: String): AbortCaseResult {
        // Abort clears the server-side session so a partially completed case does
        // not remain eligible for follow-up submissions.
        val params = JSONObject().put("sessionId", sessionId)
        try {
            return AbortCaseResult.from(ParseClient.callFunction("abortOralCase", params))
        } catch (e: Exception) {
            Log.w(TAG, "abortCase failed", e)
            throw e
        }
    }

    suspend fun getDebrief(sessionId: String): OralCaseDebriefResponse {
        // Debrief loading is deferred until the debrief screen appears so the
        // active case flow is not blocked on summary generation.
        val params = JSONObject().put("sessionId", sessionId)
        try {
            return OralCaseDebriefResponse.from(
                ParseClient.callFunction("downloadOralCaseDebrief", params)
            )
        } catch (e: Exception) {
            Log.w(TAG, "getDebrief failed", e)
            throw e
        }
    }

    suspend fun logPerformance(
        deviceId: String,
        caseId: String,
        specialty: String,
        completionReason: String,
        turnCount: Int,
        majorErrorsCount: Int,
        minorErrorsCount: Int,
        coveredPointsCount: Int,
    ) {
        val params = JSONObject()
            .put("deviceId", deviceId)
            .put("caseId", caseId)
            .put("specialty", specialty)
            .put("completionReason", completionReason)
            .put("turnCount", turnCount)
            .put("majorErrorsCount", majorErrorsCount)
            .put("minorErrorsCount", minorErrorsCount)
            .put("coveredPointsCount", coveredPointsCount)
        ParseClient.callFunction("logPerformance", params)
    }
}
