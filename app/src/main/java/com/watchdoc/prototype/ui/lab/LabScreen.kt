package com.watchdoc.prototype.ui.lab

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watchdoc.prototype.PrototypeViewModel
import com.watchdoc.prototype.model.*
import com.watchdoc.prototype.pagination.*
import com.watchdoc.prototype.ui.*

@Composable fun DeliveryControls(state: PrototypeState, vm: PrototypeViewModel) {
    Choice("Delivery mode (Lab, Reader and Setup)", state.deliveryMode, DeliveryMode.entries.toList(), { it.label }) { value -> vm.edit { it.copy(deliveryMode = value) } }
    Choice("Body format (Lab, Reader and Setup)", state.bodyFormat, BodyFormat.entries.toList(), { it.label }) { value -> vm.edit { it.copy(bodyFormat = value) } }
}

@Composable fun LabScreen(state: PrototypeState, vm: PrototypeViewModel) {
    ScreenContent {
        Heading("Notification Lab", "Measure visible text, forwarding, updates and separate entries.")
        OutlinedTextField(state.labTitle, { value -> vm.edit { it.copy(labTitle = value) } }, label = { Text("Document / notification title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Exact ASCII body presets", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PresetPayloadGenerator.sizes.forEach { size -> OutlinedButton({ vm.preset(size) }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(size.toString()) } }
        }
        OutlinedTextField(state.labBody, { value -> vm.edit { it.copy(labBody = value) } }, label = { Text("Notification body") }, minLines = 5, maxLines = 12, modifier = Modifier.fillMaxWidth())
        Text("Body: ${state.labBody.codePointLength()} Unicode code points · ${state.labBody.length} UTF-16 units. Title counted separately.", style = MaterialTheme.typography.labelLarge)
        Text("Presets use numbered, 25-character segments and |END-NNNN| markers. The final segment may be partial to keep the body exact. Record the last visible segment or 1-based body position.")
        DeliveryControls(state, vm)
        InfoCard("Same ID requests an update to Android ID 4242 and requests repeated alerts. Separate mode allocates a new ID for each send. Android/OEM throttling and Realme Link behavior may affect repeated alerts. Only your observations establish what happened on the watch.")
        InfoCard("BigTextStyle sends the full body in both contentText and bigText. Android's expanded layout does not guarantee the same layout or text length on the S2.")
        Button(vm::sendLab, Modifier.fillMaxWidth(), enabled = state.labBody.isNotBlank()) { Text("Send") }
        OutlinedButton(vm::clearNotifications, Modifier.fillMaxWidth()) { Text("Clear prototype notifications") }
        state.records.lastOrNull()?.let {
            Text("Last local posting result", style = MaterialTheme.typography.titleMedium)
            Text("${it.status.label}\nAndroid ID ${it.notificationId} · ${it.source}\n${it.localDetail}")
        }
    }
}
