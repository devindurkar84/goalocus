package dev.goalocus.home

import dev.goalocus.core.content.ContentSource
import dev.goalocus.core.content.GetJourneyOverview
import dev.goalocus.core.content.InvalidContentPackException
import dev.goalocus.core.domain.capability.Capability
import dev.goalocus.core.domain.capability.CapabilityProbe
import dev.goalocus.core.domain.capability.CapabilityRegistry
import dev.goalocus.core.domain.capability.CapabilityStatus
import dev.goalocus.core.domain.capability.ProbingCapabilityRegistry
import dev.goalocus.core.domain.capability.UserAction
import dev.goalocus.core.domain.capability.UserActionLauncher
import dev.goalocus.core.domain.model.ContentPack
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val pack = ContentPack(version = "v1", topics = emptyList(), phases = emptyList(), glossary = emptyList())
    private val clock = Clock.fixed(Instant.parse("2026-12-07T02:00:00Z"), ZoneOffset.UTC)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        content: ContentSource = ContentSource { pack },
        registry: CapabilityRegistry = ProbingCapabilityRegistry(emptyMap()),
        launcher: UserActionLauncher = UserActionLauncher {},
    ) = HomeViewModel(GetJourneyOverview(content, clock), registry, launcher)

    @Test
    fun `loads the journey on start`() {
        val journey = viewModel().state.value.journey

        assertIs<JourneyState.Ready>(journey)
        assertEquals("v1", journey.overview.packVersion)
    }

    @Test
    fun `an invalid pack shows the reason and retry recovers`() {
        var broken = true
        val vm = viewModel(content = { if (broken) throw InvalidContentPackException("bad pack") else pack })

        assertEquals(JourneyState.Failed("bad pack"), vm.state.value.journey)

        broken = false
        vm.retry()

        assertIs<JourneyState.Ready>(vm.state.value.journey)
    }

    @Test
    fun `capabilities appear on refresh and fixes go to the launcher`() {
        val launched = mutableListOf<UserAction>()
        val needsNotifications = CapabilityStatus.NeedsUserAction(UserAction.GRANT_NOTIFICATIONS)
        val vm = viewModel(
            registry = ProbingCapabilityRegistry(
                mapOf(
                    Capability.NOTIFICATIONS to CapabilityProbe { needsNotifications },
                ),
            ),
            launcher = { launched += it },
        )
        assertTrue(vm.state.value.capabilities.isEmpty())

        vm.refreshCapabilities()
        vm.onUserAction(UserAction.GRANT_NOTIFICATIONS)

        assertEquals(Capability.entries.size, vm.state.value.capabilities.size)
        assertEquals(
            CapabilityItem(Capability.NOTIFICATIONS, needsNotifications),
            vm.state.value.capabilities.single { it.capability == Capability.NOTIFICATIONS },
        )
        assertEquals(listOf(UserAction.GRANT_NOTIFICATIONS), launched)
    }
}
