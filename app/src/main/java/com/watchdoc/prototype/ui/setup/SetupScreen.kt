package com.watchdoc.prototype.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.watchdoc.prototype.notification.NotificationDiagnostics
import com.watchdoc.prototype.ui.*

@Composable fun SetupScreen(diagnostics: NotificationDiagnostics?, requestPermission: () -> Unit,
    openAppSettings: () -> Unit, openChannelSettings: () -> Unit, openAccessSettings: () -> Unit,
    refresh: () -> Unit, send: () -> Unit, lockedTestArmed: Boolean, toggleLockedTest: () -> Unit) {
    ScreenContent {
        Heading("Set up the path", "Phone → Realme Link → S2 notification interface")
        InfoCard("Check the watch connection in Realme Link. This prototype cannot detect the connection or confirm watch delivery.")
        Text("Phone diagnostics", style = MaterialTheme.typography.titleLarge)
        diagnostics?.let {
            Text("Notification permission: ${if (!it.runtimeRequired) "Not required on this Android version" else if (it.permissionGranted) "Granted" else "Not granted"}")
            Text("Application notifications: ${if (it.applicationEnabled) "Enabled" else "Disabled"}")
            Text("Reading channel: ${if (it.channelEnabled) "Enabled" else "Blocked / unavailable"} (importance ${it.channelImportance})")
            InfoCard(it.guidance)
            if (it.runtimeRequired && !it.permissionGranted) Button(requestPermission, Modifier.fillMaxWidth()) { Text("Request notification permission") }
        }
        OutlinedButton(openAppSettings, Modifier.fillMaxWidth()) { Text("Open app notification settings") }
        OutlinedButton(openChannelSettings, Modifier.fillMaxWidth()) { Text("Open reading channel settings") }
        TextButton(refresh) { Text("Refresh diagnostics") }
        HorizontalDivider()
        Text("Realme Link checklist", style = MaterialTheme.typography.titleLarge)
        Text("1. Install/open Realme Link and pair your Watch S2 using its device setup flow. Keep the phone nearby and connected.")
        Text("2. In Realme Link, enable the watch's phone notification / message synchronization setting. Names vary by app version.")
        Text("3. Grant Realme Link the notification access requested by its setup flow. The button below opens Android's notification-access page. Select Realme Link, if present. WatchDoc does not need or request listener access.")
        OutlinedButton(openAccessSettings, Modifier.fillMaxWidth()) { Text("Open notification-access settings") }
        Text("4. Select WatchDoc Prototype in Realme Link's app notification list, if available. If absent, send a setup test, reopen the list and record whether the app becomes selectable. Support for custom apps is unverified.")
        Text("5. Check Android and watch Do Not Disturb settings and any Realme Link background restrictions if forwarding fails. Make any changes manually and record them.")
        Button(send, Modifier.fillMaxWidth()) { Text("Send a short setup test") }
        OutlinedButton(toggleLockedTest, Modifier.fillMaxWidth()) { Text(if (lockedTestArmed) "Cancel armed locked-phone test" else "Arm one test for the next screen-off") }
        Text("For a locked-phone test, enable a secure screen lock with immediate locking, arm once, then press the power button. The app attempts one short send on screen-off only if Android reports the phone locked. No timer or service runs. If locking takes longer, no send occurs; retry with immediate-lock settings. Recreating or closing this activity cancels arming.")
        InfoCard("Privacy: the full text can appear in the phone notification area, on the lock screen, and on the watch. Use non-sensitive sample text. All prototype data stays on this phone unless you export it.")
    }
}
