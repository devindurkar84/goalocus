package dev.goalocus.platform.device.di

import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dev.goalocus.core.domain.capability.Capability
import dev.goalocus.core.domain.capability.CapabilityProbe
import dev.goalocus.core.domain.capability.CapabilityRegistry
import dev.goalocus.core.domain.capability.ProbingCapabilityRegistry
import dev.goalocus.core.domain.capability.UserActionLauncher
import dev.goalocus.platform.device.SettingsLauncher
import dev.goalocus.platform.device.probe.ExactAlarmsProbe
import dev.goalocus.platform.device.probe.FocusModesProbe
import dev.goalocus.platform.device.probe.LiveUpdatesProbe
import dev.goalocus.platform.device.probe.NfcTriggerProbe
import dev.goalocus.platform.device.probe.NotificationsProbe
import javax.inject.Singleton

@MapKey
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class CapabilityKey(val value: Capability)

/**
 * Wires one probe per capability into a map. To support a new capability, write its probe
 * and add one binding here; capabilities with no binding report NotBuiltYet.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeviceModule {
    @Binds
    @IntoMap
    @CapabilityKey(Capability.NOTIFICATIONS)
    abstract fun notifications(probe: NotificationsProbe): CapabilityProbe

    @Binds
    @IntoMap
    @CapabilityKey(Capability.EXACT_ALARMS)
    abstract fun exactAlarms(probe: ExactAlarmsProbe): CapabilityProbe

    @Binds
    @IntoMap
    @CapabilityKey(Capability.FOCUS_MODES)
    abstract fun focusModes(probe: FocusModesProbe): CapabilityProbe

    @Binds
    @IntoMap
    @CapabilityKey(Capability.LIVE_UPDATES)
    abstract fun liveUpdates(probe: LiveUpdatesProbe): CapabilityProbe

    @Binds
    @IntoMap
    @CapabilityKey(Capability.NFC_TRIGGER)
    abstract fun nfcTrigger(probe: NfcTriggerProbe): CapabilityProbe

    @Binds
    abstract fun userActionLauncher(launcher: SettingsLauncher): UserActionLauncher

    companion object {
        @Provides
        @Singleton
        fun capabilityRegistry(
            probes: Map<Capability, @JvmSuppressWildcards CapabilityProbe>,
        ): CapabilityRegistry = ProbingCapabilityRegistry(probes)
    }
}
