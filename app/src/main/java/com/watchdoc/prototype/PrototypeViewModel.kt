package com.watchdoc.prototype

import android.net.Uri
import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchdoc.prototype.data.*
import com.watchdoc.prototype.model.*
import com.watchdoc.prototype.notification.*
import com.watchdoc.prototype.pagination.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.util.UUID

data class PrototypeUiState(
    val state: PrototypeState? = null,
    val sections: List<TextSection> = emptyList(),
    val diagnostics: NotificationDiagnostics? = null,
    val message: String? = null,
    val loadError: String? = null
)

class PrototypeViewModel(
    private val repository: PrototypeRepository,
    private val publisher: WatchNotificationPublisher,
    private val checker: NotificationStatusChecker,
    private val resolver: ContentResolver
) : ViewModel() {
    private val chunker = TextChunker()
    private val actions = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val mutableUi = MutableStateFlow(PrototypeUiState())
    val ui: StateFlow<PrototypeUiState> = mutableUi.asStateFlow()
    private val snapshot: PrototypeState get() = checkNotNull(mutableUi.value.state)

    init {
        viewModelScope.launch {
            load()
            // A single actor orders edits, ID allocation, history writes and manual sends.
            for (action in actions) {
                try { action() }
                catch (e: Exception) {
                    if (e is CancellationException) throw e
                    notice("Operation failed: ${e.message ?: e.javaClass.simpleName}. Check local history before retrying a send.")
                }
            }
        }
    }

    private suspend fun load() {
        try {
            val state = withContext(Dispatchers.IO) { repository.load() }
            val sections = withContext(Dispatchers.Default) { chunker.chunk(state.readerText, state.bodyBudget) }
            mutableUi.value = mutableUi.value.copy(state = state.copy(selectedSection = ReaderPosition.bound(state.selectedSection, sections.size)),
                sections = sections, diagnostics = checker.check(), loadError = null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutableUi.update { it.copy(loadError = "Could not load local state: ${e.message}. Existing data has not been overwritten.") }
        }
    }

    fun retryLoad() { actions.trySend { load() } }
    fun notice(message: String?) { mutableUi.update { it.copy(message = message) } }
    fun refreshDiagnostics() { mutableUi.update { it.copy(diagnostics = checker.check()) } }

    private suspend fun commit(state: PrototypeState, regenerate: Boolean = false) {
        withContext(Dispatchers.IO) { repository.save(state) }
        val sections = if (regenerate) withContext(Dispatchers.Default) { chunker.chunk(state.readerText, state.bodyBudget) } else mutableUi.value.sections
        mutableUi.update { it.copy(state = state, sections = sections) }
    }
    fun edit(transform: (PrototypeState) -> PrototypeState) {
        actions.trySend { commit(transform(snapshot)) }
    }
    fun preset(size: Int) = edit { it.copy(labBody = PresetPayloadGenerator.generate(size)) }
    fun loadSample() = edit { it.copy(readerDraft = SampleDocument.text, readerTitle = SampleDocument.title) }
    fun regenerate() {
        actions.trySend {
            commit(snapshot.copy(readerText = snapshot.readerDraft, bodyBudget = snapshot.draftBudget, selectedSection = 0), regenerate = true)
            notice("Sections regenerated. Reading position reset to section 1. Nothing was sent.")
        }
    }
    fun navigate(forward: Boolean) {
        actions.trySend {
            val count = mutableUi.value.sections.size
            val next = if (forward) ReaderPosition.next(snapshot.selectedSection, count) else ReaderPosition.previous(snapshot.selectedSection, count)
            if (next != snapshot.selectedSection) {
                commit(snapshot.copy(selectedSection = next))
                if (snapshot.autoSend) sendReaderInternal()
            }
        }
    }
    fun sendLab() { actions.trySend {
        if (snapshot.labBody.isBlank()) notice("Enter a notification body before sending.")
        else post(snapshot.labTitle.ifBlank { "WatchDoc test" }, snapshot.labBody, "Lab")
    } }
    fun sendSetup() { actions.trySend { post("WatchDoc setup", "[SETUP] Phone notification test. Check the S2 manually. |END|", "Setup") } }
    fun sendLockedSetup() { actions.trySend {
        if (!publisher.phoneLocked()) {
            notice("Locked test not sent: Android did not report the phone locked. Configure secure immediate locking and try again.")
        } else post("WatchDoc locked test", "[LOCKED] One manually armed phone-lock test. Check the S2. |END|", "Setup")
    } }
    fun sendReader() { actions.trySend { sendReaderInternal() } }
    private suspend fun sendReaderInternal() {
        val section = mutableUi.value.sections.getOrNull(snapshot.selectedSection)
        if (section == null) { notice("No sections. Paste text and regenerate sections first."); return }
        post("${snapshot.readerTitle.ifBlank { "Document" }} · ${section.index + 1}/${section.total}", section.body, "Reader")
    }

    private suspend fun post(title: String, body: String, source: String) {
        // NotificationCompat truncates CharSequences beyond 5,120 UTF-16 units. Refuse
        // oversized lab input so the recorded payload always matches what we request.
        if (body.length > 5120 || title.length > 5120) {
            notice("Android notification fields are limited to 5,120 UTF-16 units. Shorten the title/body or use the reader. Nothing was sent.")
            return
        }
        val state = snapshot
        val sameId = state.deliveryMode == DeliveryMode.SAME_ID
        check(sameId || state.nextNotificationId < Int.MAX_VALUE) { "Unique notification ID range exhausted." }
        val id = if (sameId) WatchNotificationPublisher.FIXED_READER_ID else state.nextNotificationId
        val record = TestRecord(
            attemptId = UUID.randomUUID().toString(), attemptedAtEpochMs = System.currentTimeMillis(),
            notificationId = id, source = source, title = title, body = body,
            bodyCodePoints = body.codePointLength(), bodyUtf16Units = body.length,
            payloadSha256 = MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) },
            expectedEndMarker = Regex("\\|END(?:-[0-9]{4})?\\|$").find(body)?.value,
            deliveryMode = state.deliveryMode, bodyFormat = state.bodyFormat, updateRequested = sameId,
            phoneLockedAtAttempt = publisher.phoneLocked()
        )
        // Reserve the ID and record before calling Android. A crash never triggers a resend.
        commit(state.copy(nextNotificationId = if (sameId) state.nextNotificationId else id + 1, records = state.records + record))
        val result = publisher.post(record)
        val completed = record.copy(status = result.status, localDetail = result.detail)
        commit(snapshot.copy(records = snapshot.records.map { if (it.attemptId == record.attemptId) completed else it }))
        refreshDiagnostics()
        notice("${result.status.label} Android ID $id.")
    }

    fun clearNotifications() { actions.trySend {
        publisher.clear()
        notice("Prototype notifications cleared from the phone. Watch retention must be checked manually; test history was kept.")
    } }
    fun saveObservation(id: String, observation: WatchObservation) = edit { state ->
        state.copy(records = state.records.map { if (it.attemptId == id) it.copy(observation = observation) else it })
    }
    fun export(uri: Uri?) {
        if (uri == null) { notice("Export cancelled. Local history was kept."); return }
        actions.trySend {
            val report = withContext(Dispatchers.Default) { ReportExporter.encode(snapshot) }
            withContext(Dispatchers.IO) {
                val output = resolver.openOutputStream(uri, "wt") ?: error("Document provider did not open the report.")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(report) }
            }
            notice("JSON report exported. Watch observations are manual, not delivery confirmations.")
        }
    }
}
