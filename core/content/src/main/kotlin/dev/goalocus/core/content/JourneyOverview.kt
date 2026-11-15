package dev.goalocus.core.content

import dev.goalocus.core.domain.model.Phase
import dev.goalocus.core.domain.model.Topic
import dev.goalocus.core.domain.model.TopicId
import java.time.Clock
import java.time.LocalDate

/** What the home screen shows before any progress tracking exists (milestone M0). */
data class JourneyOverview(
    val packVersion: String,
    val topicCount: Int,
    val questionCount: Int,
    val today: LocalDate,
    /** First day of the first phase; null if the pack has no phases. */
    val journeyStart: LocalDate?,
    /** Null before the journey starts or after it ends. */
    val phase: Phase?,
    /** First topics the learner can start now, prerequisites first. */
    val upNext: List<Topic>,
)

/** Builds a [JourneyOverview] from the content pack and today's date in the device time zone. */
class GetJourneyOverview(
    private val content: ContentSource,
    private val clock: Clock,
) {
    suspend operator fun invoke(learned: Set<TopicId>, limit: Int = DEFAULT_LIMIT): JourneyOverview {
        require(limit > 0) { "limit must be positive" }
        val pack = content.load()
        val today = LocalDate.now(clock)
        return JourneyOverview(
            packVersion = pack.version,
            topicCount = pack.topics.size,
            questionCount = pack.topics.sumOf { it.questions.size },
            today = today,
            journeyStart = pack.phases.minOfOrNull(Phase::start),
            phase = pack.phaseOn(today),
            upNext = TopicGraph(pack.topics).frontier(learned).take(limit),
        )
    }

    private companion object {
        const val DEFAULT_LIMIT = 3
    }
}
