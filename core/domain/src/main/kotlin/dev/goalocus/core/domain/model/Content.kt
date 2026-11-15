package dev.goalocus.core.domain.model

import java.time.LocalDate

/** Stable id like `sys.net.tcp-handshake`. Progress is keyed by it, so it never changes once shipped. */
@JvmInline
value class TopicId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Invalid topic id '$value'" }
    }

    override fun toString(): String = value

    private companion object {
        val PATTERN = Regex("^[a-z]+(\\.[a-z0-9]+(-[a-z0-9]+)*){2,}$")
    }
}

enum class Strand { SYSTEMS, ALGORITHMS, INTELLIGENCE }

/** How deep a lesson goes; the session ladder decides which layers a learner sees. */
enum class DepthLayer { CORE, EXTENDED, DEEP }

enum class ReferenceKind { ARTICLE, VIDEO, DOCS, BOOK, PAPER, COURSE, TOOL }

data class LessonCard(
    val layer: DepthLayer,
    val title: String,
    val markdown: String,
)

data class Reference(
    val title: String,
    val url: String,
    val kind: ReferenceKind,
    val note: String? = null,
    val startAt: String? = null,
)

data class EstimatedMinutes(
    val core: Int,
    val extended: Int,
    val deep: Int,
) {
    fun upTo(layer: DepthLayer): Int = when (layer) {
        DepthLayer.CORE -> core
        DepthLayer.EXTENDED -> core + extended
        DepthLayer.DEEP -> core + extended + deep
    }
}

data class Topic(
    val id: TopicId,
    val title: String,
    val strand: Strand,
    val module: String,
    val roleTags: Set<String>,
    /** Must be learned first; the scheduler enforces these. */
    val hardPrereqs: List<TopicId>,
    /** Shown as a short refresher inside the lesson instead of blocking. May point at topics not written yet. */
    val softPrereqs: List<TopicId>,
    /** "Connects to" links. May point at topics not written yet. */
    val bridges: List<TopicId>,
    val interviewWeight: Int,
    val keyPoints: List<String>,
    val estimatedMinutes: EstimatedMinutes,
    val references: List<Reference>,
    val cards: List<LessonCard>,
    val guess: SingleChoice,
    val questions: List<Question>,
) {
    /** Cards visible at a given depth, in authored order. */
    fun cardsUpTo(layer: DepthLayer): List<LessonCard> = cards.filter { it.layer <= layer }
}

data class Phase(
    val id: String,
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
    /** Learning days per strand in a 6-learning-day week. */
    val weeklyMix: Map<Strand, Int>,
) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

data class GlossaryTerm(
    val term: String,
    val definition: String,
    val topicId: TopicId?,
)

/** Everything compiled from `content/`, immutable for the life of the app process. */
data class ContentPack(
    val version: String,
    val topics: List<Topic>,
    val phases: List<Phase>,
    val glossary: List<GlossaryTerm>,
) {
    private val topicsById: Map<TopicId, Topic> by lazy { topics.associateBy(Topic::id) }

    fun topic(id: TopicId): Topic? = topicsById[id]

    fun phaseOn(date: LocalDate): Phase? = phases.firstOrNull { date in it }
}
