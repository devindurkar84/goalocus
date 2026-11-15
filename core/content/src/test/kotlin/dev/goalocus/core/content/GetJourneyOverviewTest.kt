package dev.goalocus.core.content

import dev.goalocus.core.domain.model.ContentPack
import dev.goalocus.core.domain.model.Phase
import dev.goalocus.core.domain.model.Strand
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class GetJourneyOverviewTest {
    private val phase = Phase(
        id = "foundations",
        name = "Foundations",
        start = LocalDate.of(2026, 12, 7),
        end = LocalDate.of(2027, 1, 10),
        weeklyMix = mapOf(Strand.SYSTEMS to 3, Strand.ALGORITHMS to 2, Strand.INTELLIGENCE to 1),
    )
    private val pack = ContentPack(
        version = "abc123",
        topics = listOf(
            topic("sys.net.tcp-ip-model"),
            topic("sys.net.tcp-handshake", "sys.net.tcp-ip-model"),
            topic("dsa.foundations.big-o"),
        ),
        phases = listOf(phase),
        glossary = emptyList(),
    )

    /** 23:30 UTC on 6 Dec is already 7 Dec in India, so the zone must decide the date. */
    private val clock = Clock.fixed(Instant.parse("2026-12-06T23:30:00Z"), ZoneId.of("Asia/Kolkata"))

    @Test
    fun `uses today's date in the device zone to pick the phase`() = runTest {
        val overview = GetJourneyOverview({ pack }, clock)(learned = emptySet())

        assertEquals(LocalDate.of(2026, 12, 7), overview.today)
        assertEquals(phase, overview.phase)
        assertEquals("abc123", overview.packVersion)
        assertEquals(3, overview.topicCount)
    }

    @Test
    fun `up next only offers topics whose prerequisites are learned`() = runTest {
        val use = GetJourneyOverview({ pack }, clock)

        assertEquals(
            listOf(id("dsa.foundations.big-o"), id("sys.net.tcp-ip-model")),
            use(learned = emptySet()).upNext.map { it.id },
        )
        assertEquals(
            listOf(id("dsa.foundations.big-o"), id("sys.net.tcp-handshake")),
            use(learned = setOf(id("sys.net.tcp-ip-model"))).upNext.map { it.id },
        )
    }

    @Test
    fun `limit caps the list and must be positive`() = runTest {
        val use = GetJourneyOverview({ pack }, clock)

        assertEquals(1, use(learned = emptySet(), limit = 1).upNext.size)
        assertFailsWith<IllegalArgumentException> { use(learned = emptySet(), limit = 0) }
    }

    @Test
    fun `no phase outside the journey dates`() = runTest {
        val before = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneId.of("Asia/Kolkata"))

        val overview = GetJourneyOverview({ pack }, before)(learned = emptySet())

        assertNull(overview.phase)
        assertEquals(LocalDate.of(2026, 12, 7), overview.journeyStart)
    }
}
