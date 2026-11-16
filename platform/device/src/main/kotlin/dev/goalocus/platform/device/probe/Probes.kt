package dev.goalocus.platform.device.probe

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.goalocus.core.domain.capability.CapabilityProbe
import dev.goalocus.core.domain.capability.CapabilityStatus
import dev.goalocus.core.domain.capability.CapabilityStatus.NeedsUserAction
import dev.goalocus.core.domain.capability.CapabilityStatus.Ready
import dev.goalocus.core.domain.capability.CapabilityStatus.Unsupported
import dev.goalocus.core.domain.capability.UserAction
import javax.inject.Inject

// One probe per capability. Each reads live system state on every call, because the user can
// change any of these in Settings while Goalocus is in the background.

internal class NotificationsProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CapabilityProbe {
    // On Android 13+ this is also false until POST_NOTIFICATIONS is granted.
    override fun probe(): CapabilityStatus = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        Ready
    } else {
        NeedsUserAction(UserAction.GRANT_NOTIFICATIONS)
    }
}

internal class ExactAlarmsProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CapabilityProbe {
    override fun probe(): CapabilityStatus {
        val alarms = context.getSystemService(AlarmManager::class.java)
        return if (alarms.canScheduleExactAlarms()) Ready else NeedsUserAction(UserAction.ALLOW_EXACT_ALARMS)
    }
}

/** AutomaticZenRule with device effects (grayscale, dim wallpaper) arrived in Android 15. */
internal class FocusModesProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CapabilityProbe {
    override fun probe(): CapabilityStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return Unsupported("Focus modes need Android 15 or later")
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        return if (notifications.isNotificationPolicyAccessGranted) {
            Ready
        } else {
            NeedsUserAction(UserAction.GRANT_DO_NOT_DISTURB_ACCESS)
        }
    }
}

/** Promoted ongoing notifications (Live Updates, shown in the Now Bar on One UI) arrived in Android 16. */
internal class LiveUpdatesProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CapabilityProbe {
    override fun probe(): CapabilityStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            return Unsupported("Live Updates need Android 16 or later")
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return NeedsUserAction(UserAction.GRANT_NOTIFICATIONS)
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        return if (notifications.canPostPromotedNotifications()) {
            Ready
        } else {
            NeedsUserAction(
                UserAction.ALLOW_LIVE_UPDATES,
            )
        }
    }
}

internal class NfcTriggerProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CapabilityProbe {
    override fun probe(): CapabilityStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return Unsupported("This phone has no NFC")
        return if (adapter.isEnabled) Ready else NeedsUserAction(UserAction.TURN_ON_NFC)
    }
}
