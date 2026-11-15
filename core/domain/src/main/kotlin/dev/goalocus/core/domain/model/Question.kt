package dev.goalocus.core.domain.model

/**
 * A quiz or review question. The set of types is deliberately closed (sealed) so graders and
 * renderers get exhaustive `when` checks; adding a type is a compile error until both handle it.
 */
sealed interface Question {
    /** Unique within its topic. */
    val id: String
    val prompt: String
    val explanation: String

    /** 1 (recall) to 3 (applied reasoning). */
    val difficulty: Int
}

/** Types with exactly one correct option. */
sealed interface SingleAnswerQuestion : Question {
    val options: List<String>
    val answerIndex: Int
}

data class SingleChoice(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    override val options: List<String>,
    override val answerIndex: Int,
) : SingleAnswerQuestion

data class ComplexityPick(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    override val options: List<String>,
    override val answerIndex: Int,
) : SingleAnswerQuestion

data class SpotTheBug(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    val code: String,
    val language: String,
    override val options: List<String>,
    override val answerIndex: Int,
) : SingleAnswerQuestion

data class ScenarioTradeoff(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    val scenario: String,
    override val options: List<String>,
    override val answerIndex: Int,
) : SingleAnswerQuestion

data class MultiChoice(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    val options: List<String>,
    val answerIndexes: Set<Int>,
) : Question

/** [steps] are stored in the correct order; the UI shuffles them. */
data class OrderSteps(
    override val id: String,
    override val prompt: String,
    override val explanation: String,
    override val difficulty: Int,
    val steps: List<String>,
) : Question
