package dev.goalocus.platform.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.goalocus.core.domain.capability.UserAction
import dev.goalocus.core.domain.capability.UserActionLauncher
import javax.inject.Inject

/**
 * Opens the exact Settings screen for a [UserAction]. OEM builds sometimes drop a screen,
 * so if the specific one can't be opened we fall back to Goalocus's app info page.
 */
internal class SettingsLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserActionLauncher {
    override fun launch(action: UserAction) {
        val packageName = context.packageName
        try {
            start(settingsIntent(action, packageName))
        } catch (_: ActivityNotFoundException) {
            start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        }
    }

    // Started from the application context, so it needs its own task.
    private fun start(intent: Intent) = context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

internal fun settingsIntent(action: UserAction, packageName: String): Intent = when (action) {
    UserAction.GRANT_NOTIFICATIONS -> appNotificationSettings(packageName)
    UserAction.ALLOW_EXACT_ALARMS ->
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
    UserAction.GRANT_DO_NOT_DISTURB_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    UserAction.ALLOW_LIVE_UPDATES -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        appNotificationSettings(packageName)
    }
    UserAction.TURN_ON_NFC -> Intent(Settings.ACTION_NFC_SETTINGS)
}

private fun appNotificationSettings(packageName: String) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
