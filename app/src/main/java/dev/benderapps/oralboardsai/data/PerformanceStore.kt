package dev.benderapps.oralboardsai.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** A completed case's summary, kept on-device for the progress dashboard. */
data class LocalCasePerformance(
    val sessionId: String,
    val status: String,
    val completionReason: String?,
    val caseId: String,
    val caseTitle: String,
    val caseDomain: String?,
    val caseStem: String?,
    val requiredMustCoverPoints: Int,
    val allowedMajorErrors: Int,
    val allowedMinorErrors: Int,
    val maxTurns: Int?,
    val mustCoverPointsRequired: Int,
    val mustCoverPointsCovered: Int,
    val totalMajorErrors: Int,
    val totalMinorErrors: Int,
    val passed: Boolean,
    val outcome: String,
    val failureReasons: List<String>,
    val turnCount: Int,
    val evaluationEventCount: Int,
    val createdAtMillis: Long,
    val turns: List<OralCaseDebriefResponse.Turn>,
    val evaluationEvents: List<OralCaseDebriefResponse.EvaluationEvent>,
) {
    companion object {
        fun from(debrief: OralCaseDebriefResponse, nowMillis: Long = System.currentTimeMillis()) =
            LocalCasePerformance(
                sessionId = debrief.sessionId,
                status = debrief.status,
                completionReason = debrief.completionReason,
                caseId = debrief.caseInfo.id,
                caseTitle = debrief.caseInfo.title,
                caseDomain = debrief.caseInfo.caseDomain,
                caseStem = debrief.caseInfo.stem,
                requiredMustCoverPoints = debrief.thresholds.requiredMustCoverPoints,
                allowedMajorErrors = debrief.thresholds.allowedMajorErrors,
                allowedMinorErrors = debrief.thresholds.allowedMinorErrors,
                maxTurns = debrief.thresholds.maxTurns,
                mustCoverPointsRequired = debrief.performanceSummary.mustCoverPointsRequired,
                mustCoverPointsCovered = debrief.performanceSummary.mustCoverPointsCovered,
                totalMajorErrors = debrief.performanceSummary.totalMajorErrors,
                totalMinorErrors = debrief.performanceSummary.totalMinorErrors,
                passed = debrief.performanceSummary.passed,
                outcome = debrief.performanceSummary.outcome,
                failureReasons = debrief.performanceSummary.failureReasons,
                turnCount = debrief.turns.size,
                evaluationEventCount = debrief.evaluationEvents.size,
                createdAtMillis = nowMillis,
                turns = debrief.turns,
                evaluationEvents = debrief.evaluationEvents,
            )
    }
}

/**
 * Persists completed-case summaries as a JSON file in app-private storage.
 * Everything stays on the device; nothing here is uploaded.
 */
class PerformanceStore(context: Context) {
    private val file = File(context.filesDir, "case_performance.json")
    private val mutex = Mutex()
    private val _cases = MutableStateFlow<List<LocalCasePerformance>>(emptyList())

    /** Newest first. */
    val cases: StateFlow<List<LocalCasePerformance>> = _cases.asStateFlow()

    suspend fun load() = mutex.withLock {
        _cases.value = withContext(Dispatchers.IO) { readFromDisk() }
    }

    /** Saves the debrief once per session; repeat calls for the same session are ignored. */
    suspend fun saveDebrief(debrief: OralCaseDebriefResponse) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val existing = if (_cases.value.isEmpty()) readFromDisk() else _cases.value
            if (existing.any { it.sessionId == debrief.sessionId }) {
                _cases.value = existing
                return@withContext
            }
            val updated = listOf(LocalCasePerformance.from(debrief)) + existing
            writeToDisk(updated)
            _cases.value = updated
        }
    }

    fun find(sessionId: String): LocalCasePerformance? =
        _cases.value.firstOrNull { it.sessionId == sessionId }

    private fun readFromDisk(): List<LocalCasePerformance> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            array.toObjectList(::decode).sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeToDisk(cases: List<LocalCasePerformance>) {
        val array = JSONArray()
        cases.forEach { array.put(encode(it)) }
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(file)) {
            file.writeText(array.toString())
            temp.delete()
        }
    }

    private fun encode(c: LocalCasePerformance) = JSONObject().apply {
        put("sessionId", c.sessionId)
        put("status", c.status)
        put("completionReason", c.completionReason ?: JSONObject.NULL)
        put("caseId", c.caseId)
        put("caseTitle", c.caseTitle)
        put("caseDomain", c.caseDomain ?: JSONObject.NULL)
        put("caseStem", c.caseStem ?: JSONObject.NULL)
        put("requiredMustCoverPoints", c.requiredMustCoverPoints)
        put("allowedMajorErrors", c.allowedMajorErrors)
        put("allowedMinorErrors", c.allowedMinorErrors)
        put("maxTurns", c.maxTurns ?: JSONObject.NULL)
        put("mustCoverPointsRequired", c.mustCoverPointsRequired)
        put("mustCoverPointsCovered", c.mustCoverPointsCovered)
        put("totalMajorErrors", c.totalMajorErrors)
        put("totalMinorErrors", c.totalMinorErrors)
        put("passed", c.passed)
        put("outcome", c.outcome)
        put("failureReasons", JSONArray(c.failureReasons))
        put("turnCount", c.turnCount)
        put("evaluationEventCount", c.evaluationEventCount)
        put("createdAtMillis", c.createdAtMillis)
        put("turns", JSONArray().also { array ->
            c.turns.forEach { t ->
                array.put(
                    JSONObject()
                        .put("id", t.id)
                        .put("turnIndex", t.turnIndex)
                        .put("examinerPrompt", t.examinerPrompt)
                        .put("candidateResponse", t.candidateResponse)
                        .put("nextExaminerPrompt", t.nextExaminerPrompt ?: JSONObject.NULL)
                        .put("aiSummary", t.aiSummary ?: JSONObject.NULL)
                )
            }
        })
        put("evaluationEvents", JSONArray().also { array ->
            c.evaluationEvents.forEach { e ->
                array.put(
                    JSONObject()
                        .put("turnIndex", e.turnIndex)
                        .put("kind", e.kind)
                        .put("severity", e.severity ?: JSONObject.NULL)
                        .put("explanation", e.explanation)
                        .put("isDecisive", e.isDecisive ?: JSONObject.NULL)
                )
            }
        })
    }

    private fun decode(j: JSONObject) = LocalCasePerformance(
        sessionId = j.getString("sessionId"),
        status = j.optString("status"),
        completionReason = j.stringOrNull("completionReason"),
        caseId = j.optString("caseId"),
        caseTitle = j.optString("caseTitle"),
        caseDomain = j.stringOrNull("caseDomain"),
        caseStem = j.stringOrNull("caseStem"),
        requiredMustCoverPoints = j.optInt("requiredMustCoverPoints"),
        allowedMajorErrors = j.optInt("allowedMajorErrors"),
        allowedMinorErrors = j.optInt("allowedMinorErrors"),
        maxTurns = if (j.isNull("maxTurns")) null else j.optInt("maxTurns"),
        mustCoverPointsRequired = j.optInt("mustCoverPointsRequired"),
        mustCoverPointsCovered = j.optInt("mustCoverPointsCovered"),
        totalMajorErrors = j.optInt("totalMajorErrors"),
        totalMinorErrors = j.optInt("totalMinorErrors"),
        passed = j.optBoolean("passed"),
        outcome = j.optString("outcome"),
        failureReasons = j.optJSONArray("failureReasons").toStringList(),
        turnCount = j.optInt("turnCount"),
        evaluationEventCount = j.optInt("evaluationEventCount"),
        createdAtMillis = j.optLong("createdAtMillis"),
        turns = j.optJSONArray("turns").toObjectList { t ->
            OralCaseDebriefResponse.Turn(
                id = t.optString("id"),
                turnIndex = t.optInt("turnIndex"),
                examinerPrompt = t.optString("examinerPrompt"),
                candidateResponse = t.optString("candidateResponse"),
                nextExaminerPrompt = t.stringOrNull("nextExaminerPrompt"),
                aiSummary = t.stringOrNull("aiSummary"),
            )
        },
        evaluationEvents = j.optJSONArray("evaluationEvents").toObjectList { e ->
            OralCaseDebriefResponse.EvaluationEvent(
                turnIndex = e.optInt("turnIndex"),
                kind = e.optString("kind"),
                severity = e.stringOrNull("severity"),
                explanation = e.optString("explanation"),
                isDecisive = if (e.isNull("isDecisive")) null else e.optBoolean("isDecisive"),
            )
        },
    )
}
