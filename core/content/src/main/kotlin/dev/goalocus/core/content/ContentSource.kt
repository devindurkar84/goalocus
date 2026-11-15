package dev.goalocus.core.content

import dev.goalocus.core.domain.model.ContentPack
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Where the content pack comes from: bundled assets today, a download later (architecture §5). */
fun interface ContentSource {
    suspend fun load(): ContentPack
}

/** Reads and parses a pack from a stream on [ioDispatcher]. Opens a fresh stream on every call. */
class StreamContentSource(
    private val ioDispatcher: CoroutineDispatcher,
    private val parser: ContentPackParser = ContentPackParser(),
    private val open: () -> InputStream,
) : ContentSource {
    override suspend fun load(): ContentPack = withContext(ioDispatcher) {
        val json = open().use { it.readBytes().decodeToString() }
        parser.parse(json)
    }
}

/** Loads the pack once and shares it. Failures aren't cached, so a later call can retry. */
class CachingContentSource(private val delegate: ContentSource) : ContentSource {
    private val mutex = Mutex()

    @Volatile
    private var cached: ContentPack? = null

    override suspend fun load(): ContentPack = cached ?: mutex.withLock {
        cached ?: delegate.load().also { cached = it }
    }
}
