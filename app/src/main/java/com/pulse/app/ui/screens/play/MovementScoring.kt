package com.pulse.app.ui.screens.play

data class MovementScore(
    val points: Int,
    val judgedQuality: Float,
    val maintainsCombo: Boolean
)

object MovementScoring {
    fun forActiveSecond(
        difficulty: Int,
        cameraEnabled: Boolean,
        hasFreshVisiblePose: Boolean,
        movementQuality: Float
    ): MovementScore {
        val basePoints = 10 * difficulty.coerceAtLeast(1)
        if (!cameraEnabled) {
            return MovementScore(basePoints, 0f, true)
        }

        val quality = if (hasFreshVisiblePose) movementQuality.coerceIn(0f, 1f) else 0f
        val multiplier = if (hasFreshVisiblePose) 0.5f + quality * 1.5f else 0.25f
        return MovementScore(
            points = (basePoints * multiplier).toInt(),
            judgedQuality = quality,
            maintainsCombo = quality >= 0.65f
        )
    }
}
