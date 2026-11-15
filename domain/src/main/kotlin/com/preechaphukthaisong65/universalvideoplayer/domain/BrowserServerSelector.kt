package com.preechaphukthaisong65.universalvideoplayer.domain

/**
 * Health data measured by the browser transport layer. No endpoint credentials belong here.
 */
data class BrowserServerHealth(
    val serverId: String,
    val reachable: Boolean,
    val latencyMs: Long?,
    val downloadKbps: Long?,
    val packetLossPercent: Int
) {
    init {
        require(serverId.isNotBlank())
        require(latencyMs == null || latencyMs >= 0)
        require(downloadKbps == null || downloadKbps >= 0)
        require(packetLossPercent in 0..100)
    }

    val isUsable: Boolean
        get() = reachable &&
            latencyMs != null && latencyMs > 0 &&
            downloadKbps != null && downloadKbps > 0 &&
            packetLossPercent < 20
}

/**
 * Selects from at most four configured private endpoints.
 *
 * A failed current endpoint is replaced immediately. Otherwise, the current endpoint
 * is retained unless the best candidate is at least [minimumImprovementPercent] better,
 * preventing rapid server oscillation.
 */
object BrowserServerSelector {
    fun select(
        candidates: List<BrowserServerHealth>,
        currentServerId: String? = null,
        minimumImprovementPercent: Int = 20
    ): String? {
        require(candidates.size <= 4) { "At most four browser servers are supported" }
        require(candidates.map { it.serverId }.distinct().size == candidates.size) {
            "Server IDs must be unique"
        }
        require(minimumImprovementPercent in 1..100)

        val usable = candidates.filter { it.isUsable }
        if (usable.isEmpty()) return null

        val ranked = usable.sortedWith(
            compareBy<BrowserServerHealth> { it.packetLossPercent }
                .thenBy { it.latencyMs }
                .thenByDescending { it.downloadKbps }
        )
        val best = ranked.first()
        val current = usable.firstOrNull { it.serverId == currentServerId }
            ?: return best.serverId

        if (best.serverId == current.serverId) return current.serverId

        val currentLatency = current.latencyMs!!
        val bestLatency = best.latencyMs!!
        val latencyGain = ((currentLatency - bestLatency).toDouble() / currentLatency) * 100.0
        val currentLoss = current.packetLossPercent
        val lossGain = currentLoss - best.packetLossPercent
        val currentSpeed = current.downloadKbps!!
        val bestSpeed = best.downloadKbps!!
        val speedGain = ((bestSpeed - currentSpeed).toDouble() / currentSpeed) * 100.0

        val materiallyBetter = latencyGain >= minimumImprovementPercent ||
            speedGain >= minimumImprovementPercent ||
            lossGain >= 10

        return if (materiallyBetter) best.serverId else current.serverId
    }
}
