package com.watchdoc.prototype.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watchdoc.prototype.PrototypeViewModel
import com.watchdoc.prototype.model.PrototypeState
import com.watchdoc.prototype.pagination.*
import com.watchdoc.prototype.ui.*
import com.watchdoc.prototype.ui.lab.DeliveryControls

@Composable fun ReaderScreen(state: PrototypeState, sections: List<TextSection>, vm: PrototypeViewModel) {
    ScreenContent {
        Heading("Text Reader", "Navigate on the phone; read through the watch's notifications.")
        OutlinedTextField(state.readerTitle, { value -> vm.edit { it.copy(readerTitle = value) } }, label = { Text("Document title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedButton(vm::loadSample) { Text("Load Computer Networks sample into editor") }
        OutlinedTextField(state.readerDraft, { value -> vm.edit { it.copy(readerDraft = value) } }, label = { Text("Paste or edit document text") }, minLines = 5, maxLines = 10, modifier = Modifier.fillMaxWidth())
        Text("Body budget: ${state.draftBudget} Unicode code points", style = MaterialTheme.typography.titleMedium)
        Slider(state.draftBudget.toFloat(), { value -> vm.edit { it.copy(draftBudget = value.toInt().coerceIn(50, 1000)) } }, valueRange = 50f..1000f, steps = 18)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ vm.edit { it.copy(draftBudget = (it.draftBudget - 1).coerceAtLeast(50)) } }) { Text("−1") }
            OutlinedButton({ vm.edit { it.copy(draftBudget = (it.draftBudget + 1).coerceAtMost(1000)) } }) { Text("+1") }
        }
        Text("50–1,000 code points, including [section/total] and its space. Emoji may contain several code points. This is a test budget, not an assumed S2 limit.")
        Button(vm::regenerate, Modifier.fillMaxWidth()) { Text("Regenerate sections · reset to section 1") }
        val dirty = state.readerDraft != state.readerText || state.draftBudget != state.bodyBudget
        if (dirty) InfoCard("Editor changes are pending. Regenerate to apply them; the preview below still uses the previously applied text and ${state.bodyBudget}-point budget.")
        val section = sections.getOrNull(state.selectedSection)
        Text(if (section == null) "No sections" else "Section ${section.index + 1} of ${section.total}", style = MaterialTheme.typography.titleLarge)
        if (section == null) InfoCard("Paste some text and regenerate sections. Empty or whitespace-only input has no readable sections.")
        else {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${state.readerTitle} · ${section.index + 1}/${section.total}", style = MaterialTheme.typography.titleMedium)
                    Text(section.body)
                    Text("${section.body.codePointLength()} / ${state.bodyBudget} body code points", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton({ vm.navigate(false) }, Modifier.weight(1f), enabled = section != null && state.selectedSection > 0) { Text("Previous") }
            OutlinedButton({ vm.navigate(true) }, Modifier.weight(1f), enabled = section != null && state.selectedSection < sections.lastIndex) { Text("Next") }
        }
        Button(vm::sendReader, Modifier.fillMaxWidth(), enabled = section != null) { Text("Send Current") }
        Toggle("Send automatically after phone navigation", state.autoSend) { value -> vm.edit { it.copy(autoSend = value) } }
        Text("Only an explicit Previous/Next action can auto-send. Opening the app, rotating, restoring state and regenerating never send notifications.")
        DeliveryControls(state, vm)
    }
}
