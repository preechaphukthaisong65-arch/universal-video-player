package com.preechaphukthaisong65.universalvideoplayer.domain

object PlaybackSpeedPolicy {
    const val MIN_SPEED = 0.25f
    const val MAX_SPEED = 3.0f

    fun isAllowed(speed: Float): Boolean =
        speed.isFinite() && speed in MIN_SPEED..MAX_SPEED
}
