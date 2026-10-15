package com.watchdoc.prototype.data

import com.watchdoc.prototype.model.PrototypeState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.Instant

object ReportExporter {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    fun encode(state: PrototypeState): String = buildJsonObject {
        put("schemaVersion", 1)
        put("application", "WatchDoc Prototype")
        put("exportedAtUtc", Instant.now().toString())
        put("watchDeliveryAutomaticallyVerified", false)
        put("fullPayloadIncluded", state.exportFullPayload)
        put("characterLimitDefinition", "Unicode code points; reader body includes [section/total] prefix")
        put("metadata", json.encodeToJsonElement(state.metadata))
        put("records", buildJsonArray {
            state.records.forEach { record ->
                val encoded = json.encodeToJsonElement(record).jsonObject.toMutableMap()
                if (!state.exportFullPayload) {
                    encoded.remove("title")
                    encoded.remove("body")
                }
                encoded["attemptedAtUtc"] = JsonPrimitive(Instant.ofEpochMilli(record.attemptedAtEpochMs).toString())
                add(JsonObject(encoded))
            }
        })
    }.let { json.encodeToString(it) }
}
