package dev.goalocus.core.content

import dev.goalocus.core.domain.model.EstimatedMinutes
import dev.goalocus.core.domain.model.SingleChoice
import dev.goalocus.core.domain.model.Strand
import dev.goalocus.core.domain.model.Topic
import dev.goalocus.core.domain.model.TopicId

internal fun id(value: String) = TopicId(value)

internal fun topic(value: String, vararg hardPrereqs: String) = Topic(
    id = TopicId(value),
    title = value,
    strand = Strand.SYSTEMS,
    module = "test",
    roleTags = emptySet(),
    hardPrereqs = hardPrereqs.map(::TopicId),
    softPrereqs = emptyList(),
    bridges = emptyList(),
    interviewWeight = 3,
    keyPoints = listOf("a", "b"),
    estimatedMinutes = EstimatedMinutes(5, 3, 5),
    references = emptyList(),
    cards = emptyList(),
    guess = SingleChoice("guess", "?", "because", 1, listOf("yes", "no"), 0),
    questions = emptyList(),
)
