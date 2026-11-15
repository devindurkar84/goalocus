package dev.goalocus.core.content

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class TopicGraphTest {
    // a.x.base ← a.x.middle ← a.x.top, plus an independent a.x.solo
    private val graph = TopicGraph(
        listOf(
            topic("a.x.top", "a.x.middle"),
            topic("a.x.middle", "a.x.base"),
            topic("a.x.solo"),
            topic("a.x.base"),
        ),
    )

    @Test
    fun topologicalOrderPutsPrerequisitesFirstAndBreaksTiesAlphabetically() {
        assertEquals(
            listOf("a.x.base", "a.x.middle", "a.x.solo", "a.x.top"),
            graph.topologicalOrder.map { it.value },
        )
    }

    @Test
    fun frontierContainsOnlyTopicsWhosePrerequisitesAreLearned() {
        assertEquals(listOf("a.x.base", "a.x.solo"), graph.frontier(emptySet()).map { it.id.value })
        assertEquals(listOf("a.x.middle", "a.x.solo"), graph.frontier(setOf(id("a.x.base"))).map { it.id.value })
    }

    @Test
    fun missingPrerequisitesAreReturnedPrerequisitesFirstAndSkipLearnedOnes() {
        assertEquals(listOf(id("a.x.base"), id("a.x.middle")), graph.missingPrerequisites(id("a.x.top"), emptySet()))
        assertEquals(listOf(id("a.x.middle")), graph.missingPrerequisites(id("a.x.top"), setOf(id("a.x.base"))))
        assertTrue(graph.missingPrerequisites(id("a.x.solo"), emptySet()).isEmpty())
    }

    @Test
    fun dependentsAreIndexed() {
        assertEquals(listOf(id("a.x.middle")), graph.dependentsOf(id("a.x.base")))
        assertTrue(graph.dependentsOf(id("a.x.top")).isEmpty())
    }

    @Test
    fun cycleIsRejectedWithTheTopicsInvolved() {
        val error = assertFailsWith<IllegalArgumentException> {
            TopicGraph(listOf(topic("a.x.one", "a.x.two"), topic("a.x.two", "a.x.one"), topic("a.x.free")))
        }
        assertTrue("a.x.one" in error.message.orEmpty() && "a.x.two" in error.message.orEmpty())
        assertTrue("a.x.free" !in error.message.orEmpty())
    }

    @Test
    fun unknownPrerequisiteIsRejected() {
        assertFailsWith<IllegalArgumentException> { TopicGraph(listOf(topic("a.x.one", "a.x.missing"))) }
    }

    @Test
    fun duplicateIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { TopicGraph(listOf(topic("a.x.one"), topic("a.x.one"))) }
    }

    @Test
    fun selfPrerequisiteIsRejected() {
        assertFailsWith<IllegalArgumentException> { TopicGraph(listOf(topic("a.x.one", "a.x.one"))) }
    }
}
