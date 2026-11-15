package dev.goalocus.core.content

import dev.goalocus.core.domain.model.ContentPack
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CachingContentSourceTest {
    private val pack = ContentPack(version = "v1", topics = emptyList(), phases = emptyList(), glossary = emptyList())

    @Test
    fun loadsOnceAndSharesThePack() = runTest {
        var calls = 0
        val source = CachingContentSource {
            calls++
            pack
        }

        assertSame(pack, source.load())
        assertSame(pack, source.load())
        assertEquals(1, calls)
    }

    @Test
    fun failuresAreNotCached() = runTest {
        var calls = 0
        val source = CachingContentSource {
            calls++
            if (calls == 1) throw InvalidContentPackException("boom") else pack
        }

        assertFailsWith<InvalidContentPackException> { source.load() }
        assertSame(pack, source.load())
        assertEquals(2, calls)
    }
}
