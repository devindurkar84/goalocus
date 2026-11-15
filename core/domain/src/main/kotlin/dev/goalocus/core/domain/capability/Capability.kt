package dev.goalocus.core.domain.capability

/**
 * Device features Goalocus can use. Features ask the [CapabilityRegistry] instead of checking
 * SDK levels or permissions themselves, so every missing capability degrades the same way.
 */
enum class Capability {
    NOTIFICATIONS,
    EXACT_ALARMS,
    FOCUS_MODES,
    LIVE_UPDATES,
    NFC_TRIGGER,
    ACCESSIBILITY_ENGINE,
    USAGE_ACCESS,
    HEALTH_CONNECT,
    ON_DEVICE_AI,
    ADVANCED_PROTECTION_MONITOR,
}

/** Something the user can do in system settings to turn a capability on. */
enum class UserAction {
    GRANT_NOTIFICATIONS,
    ALLOW_EXACT_ALARMS,
    GRANT_DO_NOT_DISTURB_ACCESS,
    ALLOW_LIVE_UPDATES,
    TURN_ON_NFC,
}

sealed interface CapabilityStatus {
    data object Ready : CapabilityStatus

    data class NeedsUserAction(val action: UserAction) : CapabilityStatus

    /** The device can't do this (old Android, missing hardware). [reason] is shown to the user. */
    data class Unsupported(val reason: String) : CapabilityStatus

    /** Planned in the roadmap but not implemented in this build. */
    data object NotBuiltYet : CapabilityStatus
}

interface CapabilityRegistry {
    /** Cheap to call; re-check after the user returns from settings. */
    fun status(capability: Capability): CapabilityStatus

    fun snapshot(): Map<Capability, CapabilityStatus> = Capability.entries.associateWith(::status)
}

/** Opens the system screen where the user can take [UserAction]. Implemented by the platform layer. */
fun interface UserActionLauncher {
    fun launch(action: UserAction)
}

/** Checks one capability on the current device. One implementation per capability (Strategy). */
fun interface CapabilityProbe {
    fun probe(): CapabilityStatus
}

/**
 * Answers from a set of probes. A capability without a probe is [CapabilityStatus.NotBuiltYet],
 * so shipping a new feature means adding one probe, not editing a central `when`.
 */
class ProbingCapabilityRegistry(
    private val probes: Map<Capability, CapabilityProbe>,
) : CapabilityRegistry {
    override fun status(capability: Capability): CapabilityStatus =
        probes[capability]?.probe() ?: CapabilityStatus.NotBuiltYet
}
