package dev.goalocus.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.goalocus.core.content.CachingContentSource
import dev.goalocus.core.content.ContentSource
import dev.goalocus.core.content.GetJourneyOverview
import dev.goalocus.core.content.StreamContentSource
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

/** Must match `PACK_ASSET_PATH` in build-logic's ContentPack.kt, which writes the asset. */
private const val CONTENT_PACK_ASSET = "content/content-pack.json"

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    @Singleton
    fun contentSource(@ApplicationContext context: Context): ContentSource = CachingContentSource(
        StreamContentSource(Dispatchers.IO) { context.assets.open(CONTENT_PACK_ASSET) },
    )

    /** The device clock in the device zone; tests pass a fixed clock instead. */
    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun getJourneyOverview(content: ContentSource, clock: Clock): GetJourneyOverview =
        GetJourneyOverview(content, clock)
}
