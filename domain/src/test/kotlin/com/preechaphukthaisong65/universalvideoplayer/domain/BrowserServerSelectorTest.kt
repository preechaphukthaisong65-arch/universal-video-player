package com.preechaphukthaisong65.universalvideoplayer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BrowserServerSelectorTest {
    private fun server(
        id: String,
        latency: Long,
        speed: Long,
        loss: Int = 0,
        reachable: Boolean = true
    ) = BrowserServerHealth(id, reachable, latency, speed, loss)

    @Test fun choosesBestHealthyServerFromFour() {
        val selected = BrowserServerSelector.select(
            listOf(
                server("a", 180, 2_000),
                server("b", 90, 4_000),
                server("c", 130, 3_000),
                server("d", 250, 1_000)
            )
        )
        assertEquals("b", selected)
    }

    @Test fun immediatelyFailsOverWhenCurrentServerIsUnreachable() {
        val selected = BrowserServerSelector.select(
            listOf(server("a", 100, 3_000, reachable = false), server("b", 140, 2_500)),
            currentServerId = "a"
        )
        assertEquals("b", selected)
    }

    @Test fun keepsCurrentServerWhenImprovementIsTooSmall() {
        val selected = BrowserServerSelector.select(
            listOf(server("a", 100, 3_000), server("b", 95, 3_050)),
            currentServerId = "a"
        )
        assertEquals("a", selected)
    }

    @Test fun switchesWhenSpeedImprovesMaterially() {
        val selected = BrowserServerSelector.select(
            listOf(server("a", 100, 1_000), server("b", 120, 2_000)),
            currentServerId = "a"
        )
        assertEquals("b", selected)
    }

    @Test fun returnsNullWhenNoServerIsUsable() {
        assertNull(
            BrowserServerSelector.select(
                listOf(server("a", 100, 3_000, loss = 25), server("b", 0, 0))
            )
        )
    }

    @Test fun rejectsMoreThanFourServers() {
        assertFailsWith<IllegalArgumentException> {
            BrowserServerSelector.select(
                (1..5).map { server("s$it", 100, 1_000) }
            )
        }
    }

    @Test fun rejectsDuplicateServerIds() {
        assertFailsWith<IllegalArgumentException> {
            BrowserServerSelector.select(listOf(server("same", 100, 1_000), server("same", 90, 2_000)))
        }
    }
}
