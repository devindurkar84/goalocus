package dev.goalocus.core.content

import dev.goalocus.core.domain.model.DepthLayer
import dev.goalocus.core.domain.model.MultiChoice
import dev.goalocus.core.domain.model.OrderSteps
import dev.goalocus.core.domain.model.ReferenceKind
import dev.goalocus.core.domain.model.SpotTheBug
import dev.goalocus.core.domain.model.Strand
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test

class ContentPackParserTest {
    private val parser = ContentPackParser()

    @Test
    fun parsesAPackIntoDomainModels() {
        val pack = parser.parse(pack())

        assertEquals("abc123", pack.version)
        val topic = pack.topics.single()
        assertEquals("sys.net.tcp-handshake", topic.id.value)
        assertEquals(Strand.SYSTEMS, topic.strand)
        assertEquals(listOf(DepthLayer.CORE, DepthLayer.EXTENDED), topic.cards.map { it.layer })
        assertEquals(1, topic.cardsUpTo(DepthLayer.CORE).size)
        assertEquals(ReferenceKind.VIDEO, topic.references.single().kind)
        assertEquals("1:05", topic.references.single().startAt)
        assertEquals(0, topic.guess.answerIndex)

        val (multi, order, bug) = topic.questions
        assertIs<MultiChoice>(multi)
        assertEquals(setOf(0, 2), multi.answerIndexes)
        assertIs<OrderSteps>(order)
        assertEquals(2, order.difficulty) // default when omitted
        assertIs<SpotTheBug>(bug)
        assertEquals("python", bug.language)

        val phase = pack.phases.single()
        assertEquals(3, phase.weeklyMix[Strand.SYSTEMS])
        assertTrue(LocalDate.of(2026, 12, 7) in phase)
        assertEquals(phase, pack.phaseOn(LocalDate.of(2026, 12, 13)))
        assertEquals(topic, pack.topic(topic.id))
    }

    @Test
    fun ignoresUnknownFieldsForForwardCompatibility() {
        parser.parse(pack().replace("\"packVersion\"", "\"futureField\":true,\"packVersion\""))
    }

    @Test
    fun rejectsAnUnsupportedSchemaVersion() {
        val error = assertFailsWith<InvalidContentPackException> {
            parser.parse(pack().replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
        }
        assertTrue("schema 2" in error.message.orEmpty())
    }

    @Test
    fun rejectsAnAnswerIndexOutOfRange() {
        val error = assertFailsWith<InvalidContentPackException> {
            parser.parse(pack(guessAnswer = 5))
        }
        assertTrue("topic sys.net.tcp-handshake" in error.message.orEmpty())
    }

    @Test
    fun rejectsAnUnknownHardPrerequisite() {
        assertFailsWith<InvalidContentPackException> { parser.parse(pack(hardPrereqs = "\"sys.net.missing\"")) }
    }

    @Test
    fun rejectsMalformedJson() {
        assertFailsWith<InvalidContentPackException> { parser.parse("{not json") }
    }

    private fun pack(guessAnswer: Int = 0, hardPrereqs: String = "") = """
        {"schemaVersion":1,"packVersion":"abc123",
         "phases":[{"id":"foundations","name":"Phase 1","start":"2026-12-07","end":"2026-12-13",
                    "weeklyMix":{"algorithms":2,"intelligence":1,"systems":3}}],
         "glossary":[{"term":"RTT","definition":"Round-trip time.","topic":"sys.net.tcp-handshake"}],
         "topics":[{
           "id":"sys.net.tcp-handshake","title":"Handshake","strand":"systems","module":"networking",
           "roleTags":["backend"],"hardPrereqs":[$hardPrereqs],"softPrereqs":[],"bridges":["ai.serving.later"],
           "interviewWeight":4,"keyPoints":["one","two"],
           "estMinutes":{"core":5,"extended":4,"deep":10},
           "references":[{"title":"Video","url":"https://example.com","kind":"video","startAt":"1:05"}],
           "cards":[{"layer":"core","title":"Hook","markdown":"Hi"},
                    {"layer":"extended","title":"More","markdown":"Detail"}],
           "guess":{"id":"guess","type":"single_choice","prompt":"?","explanation":"e","difficulty":1,
                    "options":["a","b"],"answer":$guessAnswer},
           "questions":[
             {"id":"m","type":"multi_choice","prompt":"p","explanation":"e","difficulty":1,
              "options":["a","b","c"],"answers":[0,2]},
             {"id":"o","type":"order_steps","prompt":"p","explanation":"e","steps":["1","2","3"]},
             {"id":"b","type":"spot_the_bug","prompt":"p","explanation":"e","difficulty":3,
              "code":"x = 1","language":"python","options":["l1","l2"],"answer":1}
           ]}]}
    """.trimIndent()
}
