package com.preechaphukthaisong65.universalvideoplayer.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackSpeedPolicyTest {
    @Test fun acceptsSupportedBoundaries() {
        assertTrue(PlaybackSpeedPolicy.isAllowed(0.25f))
        assertTrue(PlaybackSpeedPolicy.isAllowed(1.0f))
        assertTrue(PlaybackSpeedPolicy.isAllowed(3.0f))
    }

    @Test fun rejectsOutOfRangeAndNonFiniteValues() {
        assertFalse(PlaybackSpeedPolicy.isAllowed(0.0f))
        assertFalse(PlaybackSpeedPolicy.isAllowed(3.01f))
        assertFalse(PlaybackSpeedPolicy.isAllowed(Float.NaN))
        assertFalse(PlaybackSpeedPolicy.isAllowed(Float.POSITIVE_INFINITY))
    }
}
