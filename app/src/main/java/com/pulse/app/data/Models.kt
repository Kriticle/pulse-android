package com.pulse.app.data

import com.google.gson.annotations.SerializedName

data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Int,
    @SerializedName("audioFile") val audioFile: String,
    @SerializedName("durationSeconds") val durationSeconds: Int
)

data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    @SerializedName("muscleGroup") val muscleGroup: String,
    val instruction: String,
    @SerializedName("animationFile") val animationFile: String? = null

)

data class SegmentDto(
    val id: String,
    val type: String,
    @SerializedName("startTimeSeconds") val startTimeSeconds: Int,
    @SerializedName("endTimeSeconds") val endTimeSeconds: Int,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    val exercise: ExerciseDto,
    val song: SongDto?,
    @SerializedName("loopCount") val loopCount: Int
)

data class LevelDto(
    val id: String,
    val title: String,
    val difficulty: Int,
    @SerializedName("unlockScoreRequired") val unlockScoreRequired: Int,
    val segments: List<SegmentDto>,
    val categories: List<String> = emptyList()
)

data class UserDto(
    val id: String,
    @SerializedName("displayName") val displayName: String,
    val email: String,
    @SerializedName("activeThemeId") val activeThemeId: String?,
    @SerializedName("activeSkinId") val activeSkinId: String?,
    @SerializedName("totalLevelsCompleted") val totalLevelsCompleted: Int,
    @SerializedName("totalWorkoutSeconds") val totalWorkoutSeconds: Int
)

data class CreateAttemptRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("levelId") val levelId: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("accuracyPct") val accuracyPct: Float = 0f
)

data class AttemptResponse(
    @SerializedName("attemptId") val attemptId: String,
    val score: Int,
    val user: UserDto
)

data class BestAttemptDto(
    val levelId: String,
    val score: Int,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("accuracyPct") val accuracyPct: Float,
    @SerializedName("playedAt") val playedAt: Long
)