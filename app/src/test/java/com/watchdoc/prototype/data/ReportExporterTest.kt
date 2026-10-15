package com.watchdoc.prototype.data

import com.watchdoc.prototype.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ReportExporterTest {
    private val record = TestRecord("attempt-1", 0, 4242, "Lab", "PRIVATE TITLE", "PRIVATE BODY", 12, 12,
        "test-hash", deliveryMode = DeliveryMode.SAME_ID, bodyFormat = BodyFormat.BIG_TEXT,
        updateRequested = true, phoneLockedAtAttempt = false,
        observation = WatchObservation(seen = Seen.YES, notes = "Measured manually"))

    @Test fun defaultExportOmitsPayloadButKeepsMetadataAndObservations() {
        val text = ReportExporter.encode(PrototypeState(records = listOf(record), metadata = TestSessionMetadata(phoneModel = "Test phone")))
        assertFalse(text.contains("PRIVATE TITLE"))
        assertFalse(text.contains("PRIVATE BODY"))
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals(false, root.getValue("watchDeliveryAutomaticallyVerified").jsonPrimitive.boolean)
        val row = root.getValue("records").jsonArray.single().jsonObject
        assertEquals(12, row.getValue("bodyCodePoints").jsonPrimitive.int)
        assertEquals("YES", row.getValue("observation").jsonObject.getValue("seen").jsonPrimitive.content)
        assertTrue(text.contains("Test phone"))
    }
    @Test fun fullExportIsExplicitAndValidJson() {
        val text = ReportExporter.encode(PrototypeState(records = listOf(record), exportFullPayload = true))
        assertTrue(text.contains("PRIVATE TITLE"))
        assertTrue(text.contains("PRIVATE BODY"))
        assertTrue(Json.parseToJsonElement(text).jsonObject.getValue("fullPayloadIncluded").jsonPrimitive.boolean)
    }
    @Test fun stateRoundTripPreservesPositionSettingsRecordsAndObservations() {
        val state = PrototypeState(selectedSection = 4, bodyBudget = 150, autoSend = true, nextNotificationId = 10012,
            records = listOf(record), readerDraft = "Edited 😀 text\n", metadata = TestSessionMetadata(watchFirmwareVersion = "test"))
        assertEquals(state, Json.decodeFromString<PrototypeState>(Json.encodeToString(state)))
    }
}
