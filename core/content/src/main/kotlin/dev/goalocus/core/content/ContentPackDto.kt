package dev.goalocus.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Wire format written by tools/contentc (schema version 1). Kept internal: the rest of the app
 * only ever sees domain models from ContentPackParser.
 */

@Serializable
internal data class ContentPackDto(
    val schemaVersion: Int,
    val packVersion: String,
    val phases: List<PhaseDto>,
    val glossary: List<GlossaryTermDto> = emptyList(),
    val topics: List<TopicDto>,
)

@Serializable
internal data class PhaseDto(
    val id: String,
    val name: String,
    val start: String,
    val end: String,
    val weeklyMix: Map<String, Int>,
)

@Serializable
internal data class GlossaryTermDto(
    val term: String,
    val definition: String,
    val topic: String? = null,
)

@Serializable
internal data class TopicDto(
    val id: String,
    val title: String,
    val strand: String,
    val module: String,
    val roleTags: List<String> = emptyList(),
    val hardPrereqs: List<String> = emptyList(),
    val softPrereqs: List<String> = emptyList(),
    val bridges: List<String> = emptyList(),
    val interviewWeight: Int,
    val keyPoints: List<String>,
    val estMinutes: EstMinutesDto,
    val references: List<ReferenceDto>,
    val cards: List<CardDto>,
    val guess: QuestionDto,
    val questions: List<QuestionDto>,
)

@Serializable
internal data class EstMinutesDto(val core: Int, val extended: Int, val deep: Int)

@Serializable
internal data class ReferenceDto(
    val title: String,
    val url: String,
    val kind: String,
    val note: String? = null,
    val startAt: String? = null,
)

@Serializable
internal data class CardDto(val layer: String, val title: String, val markdown: String)

/** Polymorphic on the JSON "type" field (kotlinx.serialization's default discriminator). */
@Serializable
internal sealed interface QuestionDto {
    val id: String
    val prompt: String
    val explanation: String
    val difficulty: Int
}

@Serializable
@SerialName("single_choice")
internal data class SingleChoiceDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val options: List<String>,
    val answer: Int,
) : QuestionDto

@Serializable
@SerialName("complexity_pick")
internal data class ComplexityPickDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val options: List<String>,
    val answer: Int,
) : QuestionDto

@Serializable
@SerialName("spot_the_bug")
internal data class SpotTheBugDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val code: String,
    val language: String,
    val options: List<String>,
    val answer: Int,
) : QuestionDto

@Serializable
@SerialName("scenario_tradeoff")
internal data class ScenarioTradeoffDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val scenario: String,
    val options: List<String>,
    val answer: Int,
) : QuestionDto

@Serializable
@SerialName("multi_choice")
internal data class MultiChoiceDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val options: List<String>,
    val answers: List<Int>,
) : QuestionDto

@Serializable
@SerialName("order_steps")
internal data class OrderStepsDto(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int = DEFAULT_DIFFICULTY,
    val steps: List<String>,
) : QuestionDto

private const val DEFAULT_DIFFICULTY = 2
