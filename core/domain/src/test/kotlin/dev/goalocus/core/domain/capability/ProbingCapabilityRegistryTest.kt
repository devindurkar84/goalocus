package dev.goalocus.core.domain.capability

import kotlin.test.Test
import kotlin.test.assertEquals

class ProbingCapabilityRegistryTest {
    @Test
    fun `capability without a probe is not built yet`() {
        val registry = ProbingCapabilityRegistry(emptyMap())

        assertEquals(CapabilityStatus.NotBuiltYet, registry.status(Capability.HEALTH_CONNECT))
    }

    @Test
    fun `probe is asked on every call so settings changes show up`() {
        var granted = false
        val registry = ProbingCapabilityRegistry(
            mapOf(
                Capability.NOTIFICATIONS to CapabilityProbe {
                    if (granted) {
                        CapabilityStatus.Ready
                    } else {
                        CapabilityStatus.NeedsUserAction(UserAction.GRANT_NOTIFICATIONS)
                    }
                },
            ),
        )

        assertEquals(
            CapabilityStatus.NeedsUserAction(UserAction.GRANT_NOTIFICATIONS),
            registry.status(Capability.NOTIFICATIONS),
        )
        granted = true
        assertEquals(CapabilityStatus.Ready, registry.status(Capability.NOTIFICATIONS))
    }

    @Test
    fun `snapshot covers every capability`() {
        val registry = ProbingCapabilityRegistry(
            mapOf(
                Capability.NFC_TRIGGER to CapabilityProbe { CapabilityStatus.Ready },
            ),
        )

        val snapshot = registry.snapshot()

        assertEquals(Capability.entries.toSet(), snapshot.keys)
        assertEquals(CapabilityStatus.Ready, snapshot.getValue(Capability.NFC_TRIGGER))
        assertEquals(CapabilityStatus.NotBuiltYet, snapshot.getValue(Capability.FOCUS_MODES))
    }
}
