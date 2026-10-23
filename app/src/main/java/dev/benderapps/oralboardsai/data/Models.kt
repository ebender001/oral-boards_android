package dev.benderapps.oralboardsai.data

import org.json.JSONArray
import org.json.JSONObject

enum class Specialty(
    val raw: String,
    val displayName: String,
    val selectionDescription: String,
    /** Server-facing specialty value used for Back4App queries. */
    val backendSpecialty: String,
    /** Optional caseDomain filter used for subspecialty selections. */
    val backendCaseDomain: String?,
) {
    CardiothoracicSurgery(
        raw = "cardiothoracic_surgery",
        displayName = "Cardiothoracic Surgery",
        selectionDescription = "Mixed cardiac and thoracic cases",
        backendSpecialty = "cardiothoracic_surgery",
        backendCaseDomain = null,
    ),
    AdultCardiac(
        raw = "adult_cardiac",
        displayName = "Adult Cardiac Surgery",
        selectionDescription = "Cardiac-focused oral board cases",
        backendSpecialty = "cardiothoracic_surgery",
        backendCaseDomain = "adult_cardiac",
    ),
    GeneralThoracic(
        raw = "general_thoracic",
        displayName = "General Thoracic Surgery",
        selectionDescription = "Thoracic-focused oral board cases",
        backendSpecialty = "cardiothoracic_surgery",
        backendCaseDomain = "thoracic",
    ),
    GeneralSurgery(
        raw = "general_surgery",
        displayName = "General Surgery",
        selectionDescription = "General surgery case-based practice",
        backendSpecialty = "general_surgery",
        backendCaseDomain = null,
    );

    companion object {
        fun fromRaw(raw: String?): Specialty? = entries.firstOrNull { it.raw == raw }
    }
}

enum class InteractionMode(val label: String) {
    Text("Text"),
    Speech("Speech"),
}

// MARK: - Cloud function responses

data class StartCaseResponse(
    val sessionId: String,
    val caseId: String,
    val caseTitle: String,
    val caseStem: String,
    val examinerPrompt: String,
    val specialty: String?,
    val caseSelectionSource: String?,
    val usedRandomCase: Boolean?,
    val maxTurns: Int,
) {
    companion object {
        fun from(json: JSONObject) = StartCaseResponse(
            sessionId = json.getString("sessionId"),
            caseId = json.getString("caseId"),
            caseTitle = json.getString("caseTitle"),
            caseStem = json.getString("caseStem"),
            examinerPrompt = json.getString("examinerPrompt"),
            specialty = json.stringOrNull("specialty"),
            caseSelectionSource = json.stringOrNull("caseSelectionSource"),
            usedRandomCase = if (json.isNull("usedRandomCase")) null else json.optBoolean("usedRandomCase"),
            maxTurns = json.optInt("maxTurns", 6),
        )
    }
}

data class MissedConcept(
    val label: String,
    val severity: String,
    val missedConcept: String,
) {
    companion object {
        fun from(json: JSONObject) = MissedConcept(
            label = json.optString("label"),
            severity = json.optString("severity"),
            missedConcept = json.stringOrNull("missedConcept")
                ?: json.stringOrNull("missed_concept")
                ?: "",
        )
    }
}

data class ExaminerExpectation(
    val label: String,
    val severity: String,
    val explanation: String,
) {
    companion object {
        fun from(json: JSONObject) = ExaminerExpectation(
            label = json.optString("label"),
            severity = json.optString("severity"),
            explanation = json.optString("explanation"),
        )
    }
}

data class SubmitResponseResult(
    val nextExaminerPrompt: String,
    val briefEvaluation: String?,
    val isCaseComplete: Boolean,
    val completionReason: String?,
    val specialty: String?,
    val coveredPoints: List<String>,
    val majorErrors: List<String>,
    val minorErrors: List<String>,
    val missedConcepts: List<MissedConcept>,
    val examinerWasLookingFor: List<ExaminerExpectation>,
) {
    companion object {
        // The server has returned both camelCase and snake_case keys over time,
        // so every field accepts either spelling.
        fun from(json: JSONObject): SubmitResponseResult {
            fun array(camel: String, snake: String): JSONArray? =
                json.optJSONArray(camel) ?: json.optJSONArray(snake)

            return SubmitResponseResult(
                nextExaminerPrompt = json.stringOrNull("nextExaminerPrompt")
                    ?: json.stringOrNull("next_examiner_prompt")
                    ?: "",
                briefEvaluation = json.stringOrNull("briefEvaluation")
                    ?: json.stringOrNull("brief_evaluation"),
                isCaseComplete = if (json.has("isCaseComplete")) {
                    json.optBoolean("isCaseComplete")
                } else {
                    json.optBoolean("is_case_complete")
                },
                completionReason = json.stringOrNull("completionReason")
                    ?: json.stringOrNull("completion_reason"),
                specialty = json.stringOrNull("specialty"),
                coveredPoints = array("coveredPoints", "covered_points").toStringList(),
                majorErrors = array("majorErrors", "major_errors").toStringList(),
                minorErrors = array("minorErrors", "minor_errors").toStringList(),
                missedConcepts = array("missedConcepts", "missed_concepts")
                    .toObjectList(MissedConcept::from),
                examinerWasLookingFor = array("examinerWasLookingFor", "examiner_was_looking_for")
                    .toObjectList(ExaminerExpectation::from),
            )
        }
    }
}

data class AbortCaseResult(
    val success: Boolean,
    val sessionId: String,
    val deletedTurnCount: Int,
) {
    companion object {
        fun from(json: JSONObject) = AbortCaseResult(
            success = json.optBoolean("success"),
            sessionId = json.optString("sessionId"),
            deletedTurnCount = json.optInt("deletedTurnCount"),
        )
    }
}

// MARK: - Debrief

data class OralCaseDebriefResponse(
    val sessionId: String,
    val status: String,
    val completionReason: String?,
    val caseInfo: CaseInfo,
    val thresholds: Thresholds,
    val turns: List<Turn>,
    val evaluationEvents: List<EvaluationEvent>,
    val turnAlignment: List<TurnAlignment>,
    val performanceSummary: PerformanceSummary,
) {
    data class CaseInfo(
        val id: String,
        val title: String,
        val caseDomain: String?,
        val stem: String?,
    )

    data class Thresholds(
        val requiredMustCoverPoints: Int,
        val allowedMajorErrors: Int,
        val allowedMinorErrors: Int,
        val maxTurns: Int?,
    )

    data class Turn(
        val id: String,
        val turnIndex: Int,
        val examinerPrompt: String,
        val candidateResponse: String,
        val nextExaminerPrompt: String?,
        val aiSummary: String?,
    )

    data class EvaluationEvent(
        val turnIndex: Int,
        val kind: String,
        val severity: String?,
        val explanation: String,
        val isDecisive: Boolean?,
    )

    data class TurnAlignment(
        val turnId: String,
        val turnIndex: Int,
        val examinerPrompt: String,
        val candidateResponse: String,
        val aiSummary: String?,
        val events: List<EvaluationEvent>,
    )

    data class PerformanceSummary(
        val mustCoverPointsRequired: Int,
        val mustCoverPointsCovered: Int,
        val totalMajorErrors: Int,
        val totalMinorErrors: Int,
        val passed: Boolean,
        val outcome: String,
        val failureReasons: List<String>,
    )

    companion object {
        fun from(json: JSONObject): OralCaseDebriefResponse {
            val caseJson = json.getJSONObject("case")
            val thresholdsJson = json.getJSONObject("thresholds")
            val summaryJson = json.getJSONObject("performanceSummary")

            return OralCaseDebriefResponse(
                sessionId = json.getString("sessionId"),
                status = json.optString("status"),
                completionReason = json.stringOrNull("completionReason"),
                caseInfo = CaseInfo(
                    id = caseJson.getString("id"),
                    title = caseJson.getString("title"),
                    // Accept both the new `caseDomain` name and the older `specialty`
                    // field so previously generated debriefs remain readable.
                    caseDomain = caseJson.stringOrNull("caseDomain") ?: caseJson.stringOrNull("specialty"),
                    stem = caseJson.stringOrNull("stem"),
                ),
                thresholds = Thresholds(
                    requiredMustCoverPoints = thresholdsJson.getInt("requiredMustCoverPoints"),
                    allowedMajorErrors = thresholdsJson.getInt("allowedMajorErrors"),
                    allowedMinorErrors = thresholdsJson.getInt("allowedMinorErrors"),
                    maxTurns = if (thresholdsJson.isNull("maxTurns")) null else thresholdsJson.optInt("maxTurns"),
                ),
                turns = json.optJSONArray("turns").toObjectList { t ->
                    Turn(
                        id = t.getString("id"),
                        turnIndex = t.getInt("turnIndex"),
                        examinerPrompt = t.optString("examinerPrompt"),
                        candidateResponse = t.optString("candidateResponse"),
                        nextExaminerPrompt = t.stringOrNull("nextExaminerPrompt"),
                        aiSummary = t.stringOrNull("aiSummary"),
                    )
                },
                evaluationEvents = json.optJSONArray("evaluationEvents").toObjectList(::parseEvent),
                turnAlignment = json.optJSONArray("turnAlignment").toObjectList { a ->
                    TurnAlignment(
                        turnId = a.getString("turnId"),
                        turnIndex = a.getInt("turnIndex"),
                        examinerPrompt = a.optString("examinerPrompt"),
                        candidateResponse = a.optString("candidateResponse"),
                        aiSummary = a.stringOrNull("aiSummary"),
                        events = a.optJSONArray("events").toObjectList(::parseEvent),
                    )
                },
                performanceSummary = PerformanceSummary(
                    mustCoverPointsRequired = summaryJson.getInt("mustCoverPointsRequired"),
                    mustCoverPointsCovered = summaryJson.getInt("mustCoverPointsCovered"),
                    totalMajorErrors = summaryJson.getInt("totalMajorErrors"),
                    totalMinorErrors = summaryJson.getInt("totalMinorErrors"),
                    passed = summaryJson.getBoolean("passed"),
                    outcome = summaryJson.optString("outcome"),
                    failureReasons = summaryJson.optJSONArray("failureReasons").toStringList(),
                ),
            )
        }

        private fun parseEvent(e: JSONObject) = EvaluationEvent(
            turnIndex = e.getInt("turnIndex"),
            kind = e.optString("kind"),
            severity = e.stringOrNull("severity"),
            explanation = e.optString("explanation"),
            isDecisive = if (e.isNull("isDecisive")) null else e.optBoolean("isDecisive"),
        )
    }
}

// MARK: - Live case state

/** Captures the exact turn content shown and submitted during the live case. */
data class CaseTurn(
    val turnIndex: Int,
    val examinerPrompt: String,
    val candidateResponse: String,
    val newMajorErrors: List<String> = emptyList(),
    val newMinorErrors: List<String> = emptyList(),
    val missedConcepts: List<MissedConcept> = emptyList(),
    val examinerWasLookingFor: List<ExaminerExpectation> = emptyList(),
)

// MARK: - JSON helpers

/** `optString` returns the literal "null" for JSON nulls, so go through isNull first. */
internal fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key)

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        if (isNull(index)) null else optString(index)
    }
}

internal fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(transform) }
}
