package com.watchdoc.prototype.model

import com.watchdoc.prototype.data.SampleDocument
import com.watchdoc.prototype.pagination.PresetPayloadGenerator
import kotlinx.serialization.Serializable

@Serializable enum class DeliveryMode(val label: String) {
    SAME_ID("Update the same notification"), UNIQUE_ID("Separate notification for each send")
}
@Serializable enum class BodyFormat(val label: String) { STANDARD("Standard text"), BIG_TEXT("Android BigTextStyle") }
@Serializable enum class Seen(val label: String) { NOT_CHECKED("Not checked"), YES("Yes"), NO("No") }
@Serializable enum class Answer(val label: String) { UNKNOWN("Unknown"), YES("Yes"), NO("No") }
@Serializable enum class ScrollBehavior(val label: String) { UNKNOWN("Unknown"), AVAILABLE("Available"), UNAVAILABLE("Unavailable") }
@Serializable enum class WatchEntry(val label: String) { UNKNOWN("Unknown"), UPDATED("Updated existing entry"), NEW("Created a new entry"), NONE("No entry seen") }
@Serializable enum class PostStatus(val label: String) {
    POSTED("Posted on phone — watch delivery unverified."),
    PERMISSION_BLOCKED("Blocked by notification permission."),
    APP_DISABLED("Application notifications disabled."),
    CHANNEL_DISABLED("Reading channel disabled."),
    ERROR("Could not post notification.")
}
@Serializable data class WatchObservation(
    val seen: Seen = Seen.NOT_CHECKED,
    val finalPosition: String = "",
    val endMarker: Answer = Answer.UNKNOWN,
    val scroll: ScrollBehavior = ScrollBehavior.UNKNOWN,
    val titleVisible: Answer = Answer.UNKNOWN,
    val entry: WatchEntry = WatchEntry.UNKNOWN,
    val duplicates: Answer = Answer.UNKNOWN,
    val delaySeconds: String = "",
    val phoneLocked: Answer = Answer.UNKNOWN,
    val notes: String = ""
)
@Serializable data class TestSessionMetadata(
    val phoneModel: String = "",
    val androidVersion: String = "",
    val realmeLinkVersion: String = "",
    val watchFirmwareVersion: String = ""
)
@Serializable data class TestRecord(
    val attemptId: String,
    val attemptedAtEpochMs: Long,
    val notificationId: Int,
    val source: String,
    val title: String,
    val body: String,
    val bodyCodePoints: Int,
    val bodyUtf16Units: Int,
    val payloadSha256: String,
    val expectedEndMarker: String? = null,
    val deliveryMode: DeliveryMode,
    val bodyFormat: BodyFormat,
    val updateRequested: Boolean,
    val status: PostStatus = PostStatus.ERROR,
    val localDetail: String = "Attempt reserved; posting completion was not recorded.",
    val phoneLockedAtAttempt: Boolean,
    val observation: WatchObservation = WatchObservation()
)
@Serializable data class PrototypeState(
    val labTitle: String = "WatchDoc payload test",
    val labBody: String = PresetPayloadGenerator.generate(100),
    val deliveryMode: DeliveryMode = DeliveryMode.SAME_ID,
    val bodyFormat: BodyFormat = BodyFormat.STANDARD,
    val readerTitle: String = SampleDocument.title,
    val readerDraft: String = SampleDocument.text,
    val readerText: String = SampleDocument.text,
    val bodyBudget: Int = 200,
    val draftBudget: Int = 200,
    val selectedSection: Int = 0,
    val autoSend: Boolean = false,
    val nextNotificationId: Int = 10000,
    val records: List<TestRecord> = emptyList(),
    val metadata: TestSessionMetadata = TestSessionMetadata(),
    val exportFullPayload: Boolean = false
)
