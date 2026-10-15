package com.watchdoc.prototype.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.watchdoc.prototype.PrototypeViewModel
import com.watchdoc.prototype.model.*
import com.watchdoc.prototype.ui.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun ResultsScreen(state: PrototypeState, vm: PrototypeViewModel, export: () -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm:ss").withZone(ZoneId.systemDefault()) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Heading("Test Results", "${state.records.size} local attempts · watch observations recorded by you") }
        item { InfoCard("Three separate milestones: phone posting, Realme Link forwarding for this app, and practical S2 reading. A local posting result proves none of the watch behaviors.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Test-session metadata", style = MaterialTheme.typography.titleLarge)
                MetadataField("Phone model", state.metadata.phoneModel) { value -> vm.edit { it.copy(metadata = it.metadata.copy(phoneModel = value)) } }
                MetadataField("Android version", state.metadata.androidVersion) { value -> vm.edit { it.copy(metadata = it.metadata.copy(androidVersion = value)) } }
                MetadataField("Realme Link version", state.metadata.realmeLinkVersion) { value -> vm.edit { it.copy(metadata = it.metadata.copy(realmeLinkVersion = value)) } }
                MetadataField("Watch firmware version", state.metadata.watchFirmwareVersion) { value -> vm.edit { it.copy(metadata = it.metadata.copy(watchFirmwareVersion = value)) } }
                Toggle("Include full notification titles and bodies in export", state.exportFullPayload) { value -> vm.edit { it.copy(exportFullPayload = value) } }
                Text("Default export omits titles/bodies and includes lengths, payload hashes, IDs, settings and observations. Notes and metadata are always included; avoid sensitive text in them. Exported files are managed by the document provider you select.")
                Button(export, Modifier.fillMaxWidth()) { Text("Export JSON report") }
                HorizontalDivider()
            }
        }
        if (state.records.isEmpty()) item { Text("No attempts yet. Send a setup, lab or reader notification, then record what you see on the watch.") }
        items(state.records.asReversed(), key = { it.attemptId }) { record ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${record.source} · ${timeFormat.format(Instant.ofEpochMilli(record.attemptedAtEpochMs))}", style = MaterialTheme.typography.titleMedium)
                    Text(record.title)
                    Text(record.status.label, color = if (record.status == PostStatus.POSTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text("Android ID ${record.notificationId} · ${record.bodyCodePoints} body points\n${record.deliveryMode.label} · ${record.bodyFormat.label}", style = MaterialTheme.typography.bodySmall)
                    Text("Update requested: ${if (record.updateRequested) "Yes" else "No"}. Seen on watch: ${record.observation.seen.label}. Phone locked at attempt: ${record.phoneLockedAtAttempt}.")
                    record.expectedEndMarker?.let { Text("Expected end marker: $it") }
                    if (record.observation.finalPosition.isNotBlank()) Text("Last visible: ${record.observation.finalPosition}")
                    if (record.observation.notes.isNotBlank()) Text(record.observation.notes)
                    Text(record.localDetail, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton({ editingId = record.attemptId }) { Text("Record / edit watch observations") }
                }
            }
        }
    }
    state.records.find { it.attemptId == editingId }?.let { record ->
        ObservationDialog(record, dismiss = { editingId = null }, save = { observation ->
            vm.saveObservation(record.attemptId, observation)
            editingId = null
        })
    }
}

@Composable private fun MetadataField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable private fun ObservationDialog(record: TestRecord, dismiss: () -> Unit, save: (WatchObservation) -> Unit) {
    // Save the entire unsaved draft across rotation/process recreation without Parcelable.
    var draftJson by rememberSaveable(record.attemptId) { mutableStateOf(Json.encodeToString(record.observation)) }
    val draft = Json.decodeFromString<WatchObservation>(draftJson)
    fun update(value: WatchObservation) { draftJson = Json.encodeToString(value) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Watch observations · ID ${record.notificationId}") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manual observations only. Positions are 1-based in the complete body, including its section prefix.")
                SelectionContainer { Text("Sent body:\n${record.body}", style = MaterialTheme.typography.bodySmall) }
                Choice("Seen on watch", draft.seen, Seen.entries.toList(), { it.label }) { update(draft.copy(seen = it)) }
                MetadataField("Final visible segment / character position", draft.finalPosition) { update(draft.copy(finalPosition = it)) }
                Choice("End marker visible", draft.endMarker, Answer.entries.toList(), { it.label }) { update(draft.copy(endMarker = it)) }
                Choice("Scroll behavior", draft.scroll, ScrollBehavior.entries.toList(), { it.label }) { update(draft.copy(scroll = it)) }
                Choice("Title visible", draft.titleVisible, Answer.entries.toList(), { it.label }) { update(draft.copy(titleVisible = it)) }
                Choice("Watch entry behavior", draft.entry, WatchEntry.entries.toList(), { it.label }) { update(draft.copy(entry = it)) }
                Choice("Duplicate notifications observed", draft.duplicates, Answer.entries.toList(), { it.label }) { update(draft.copy(duplicates = it)) }
                OutlinedTextField(draft.delaySeconds, { value ->
                    if (value.matches(Regex("[0-9]*([.][0-9]*)?"))) update(draft.copy(delaySeconds = value))
                }, label = { Text("Approximate delay in seconds (blank = unknown)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Choice("Phone locked during test (manual)", draft.phoneLocked, Answer.entries.toList(), { it.label }) { update(draft.copy(phoneLocked = it)) }
                Text("Android reported phone locked at send: ${record.phoneLockedAtAttempt}. This is separate from your manual test observation.")
                OutlinedTextField(draft.notes, { update(draft.copy(notes = it)) }, label = { Text("Notes: retention, readability, settings, etc.") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton({ save(draft) }) { Text("Save observations") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}
