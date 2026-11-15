package dev.goalocus.core.content

import dev.goalocus.core.domain.model.ComplexityPick
import dev.goalocus.core.domain.model.ContentPack
import dev.goalocus.core.domain.model.DepthLayer
import dev.goalocus.core.domain.model.EstimatedMinutes
import dev.goalocus.core.domain.model.GlossaryTerm
import dev.goalocus.core.domain.model.LessonCard
import dev.goalocus.core.domain.model.MultiChoice
import dev.goalocus.core.domain.model.OrderSteps
import dev.goalocus.core.domain.model.Phase
import dev.goalocus.core.domain.model.Question
import dev.goalocus.core.domain.model.Reference
import dev.goalocus.core.domain.model.ReferenceKind
import dev.goalocus.core.domain.model.ScenarioTradeoff
import dev.goalocus.core.domain.model.SingleChoice
import dev.goalocus.core.domain.model.SpotTheBug
import dev.goalocus.core.domain.model.Strand
import dev.goalocus.core.domain.model.Topic
import dev.goalocus.core.domain.model.TopicId
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.Json

class InvalidContentPackException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Turns the JSON written by `tools/contentc` into domain models.
 *
 * contentc already validates authoring rules; this class re-checks the invariants the app relies on
 * (schema version, answer indexes, a valid prerequisite graph) so a corrupt pack fails loudly at
 * load time instead of crashing mid-quiz.
 */
class ContentPackParser {
    fun parse(json: String): ContentPack {
        val dto = try {
            JSON.decodeFromString(ContentPackDto.serializer(), json)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            throw InvalidContentPackException("Content pack JSON is malformed: ${e.message}", e)
        }
        if (dto.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw InvalidContentPackException(
                "Content pack schema ${dto.schemaVersion} isn't supported (expected $SUPPORTED_SCHEMA_VERSION)",
            )
        }
        val topics = dto.topics.map { topic -> inContext("topic ${topic.id}") { topic.toDomain() } }
        inContext("topic graph") { TopicGraph(topics) }
        return ContentPack(
            version = dto.packVersion,
            topics = topics,
            phases = dto.phases.map { phase -> inContext("phase ${phase.id}") { phase.toDomain() } },
            glossary = dto.glossary.map { term -> inContext("glossary '${term.term}'") { term.toDomain() } },
        )
    }

    private inline fun <T> inContext(where: String, block: () -> T): T = try {
        block()
    } catch (e: IllegalArgumentException) {
        throw InvalidContentPackException("$where: ${e.message}", e)
    } catch (e: DateTimeParseException) {
        throw InvalidContentPackException("$where: ${e.message}", e)
    }

    private companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

private fun TopicDto.toDomain(): Topic {
    val guessQuestion = guess.toDomain()
    require(guessQuestion is SingleChoice) { "guess must be single_choice" }
    require(questions.map { it.id }.toSet().size == questions.size) { "question ids must be unique" }
    return Topic(
        id = TopicId(id),
        title = title,
        strand = strandOf(strand),
        module = module,
        roleTags = roleTags.toSet(),
        hardPrereqs = hardPrereqs.map(::TopicId),
        softPrereqs = softPrereqs.map(::TopicId),
        bridges = bridges.map(::TopicId),
        interviewWeight = interviewWeight,
        keyPoints = keyPoints,
        estimatedMinutes = EstimatedMinutes(estMinutes.core, estMinutes.extended, estMinutes.deep),
        references = references.map {
            Reference(it.title, it.url, enumOf<ReferenceKind>(it.kind), it.note, it.startAt)
        },
        cards = cards.map { LessonCard(enumOf<DepthLayer>(it.layer), it.title, it.markdown) },
        guess = guessQuestion,
        questions = questions.map(QuestionDto::toDomain),
    )
}

private fun QuestionDto.toDomain(): Question = when (this) {
    is SingleChoiceDto -> SingleChoice(id, prompt, explanation, difficulty, options, checkedAnswer(options, answer))
    is ComplexityPickDto -> ComplexityPick(id, prompt, explanation, difficulty, options, checkedAnswer(options, answer))
    is SpotTheBugDto -> SpotTheBug(
        id,
        prompt,
        explanation,
        difficulty,
        code,
        language,
        options,
        checkedAnswer(options, answer),
    )
    is ScenarioTradeoffDto -> ScenarioTradeoff(
        id,
        prompt,
        explanation,
        difficulty,
        scenario,
        options,
        checkedAnswer(options, answer),
    )
    is MultiChoiceDto -> {
        require(answers.isNotEmpty()) { "question $id has no answers" }
        answers.forEach { checkedAnswer(options, it) }
        MultiChoice(id, prompt, explanation, difficulty, options, answers.toSet())
    }
    is OrderStepsDto -> {
        require(steps.size >= 2) { "question $id needs at least two steps" }
        OrderSteps(id, prompt, explanation, difficulty, steps)
    }
}

private fun QuestionDto.checkedAnswer(options: List<String>, answer: Int): Int {
    require(options.size >= 2) { "question $id needs at least two options" }
    require(answer in options.indices) { "question $id answer $answer is out of range" }
    return answer
}

private fun PhaseDto.toDomain() = Phase(
    id = id,
    name = name,
    start = LocalDate.parse(start),
    end = LocalDate.parse(end),
    weeklyMix = weeklyMix.mapKeys { (strand, _) -> strandOf(strand) },
)

private fun GlossaryTermDto.toDomain() = GlossaryTerm(term, definition, topic?.let(::TopicId))

private fun strandOf(wire: String): Strand = enumOf(wire)

/** Wire values are lower snake_case versions of the enum names (e.g. "systems" → SYSTEMS). */
private inline fun <reified E : Enum<E>> enumOf(wire: String): E =
    enumValues<E>().firstOrNull { it.name.equals(wire, ignoreCase = true) }
        ?: throw IllegalArgumentException("unknown ${E::class.simpleName} '$wire'")
