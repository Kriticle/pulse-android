package com.pulse.app.ui.screens.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementScoringTest {
    @Test
    fun cameraFreeScoringPreservesOriginalPoints() {
        val result = MovementScoring.forActiveSecond(3, false, false, 0f)
        assertEquals(30, result.points)
        assertTrue(result.maintainsCombo)
    }

    @Test
    fun confidentHighQualityMovementEarnsMaximumMultiplier() {
        val result = MovementScoring.forActiveSecond(2, true, true, 1f)
        assertEquals(40, result.points)
        assertEquals(1f, result.judgedQuality)
        assertTrue(result.maintainsCombo)
    }

    @Test
    fun missingBodyGetsParticipationPointsWithoutCombo() {
        val result = MovementScoring.forActiveSecond(2, true, false, 1f)
        assertEquals(5, result.points)
        assertEquals(0f, result.judgedQuality)
        assertFalse(result.maintainsCombo)
    }

    @Test
    fun qualityIsClampedBeforeScoring() {
        val result = MovementScoring.forActiveSecond(1, true, true, 4f)
        assertEquals(20, result.points)
        assertEquals(1f, result.judgedQuality)
    }
}
