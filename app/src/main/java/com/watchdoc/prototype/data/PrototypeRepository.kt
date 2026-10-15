package com.watchdoc.prototype.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchdoc.prototype.model.PrototypeState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.prototypeDataStore by preferencesDataStore(name = "watchdoc_state")

interface PrototypeRepository {
    suspend fun load(): PrototypeState
    suspend fun save(state: PrototypeState)
}

class DataStorePrototypeRepository(private val store: DataStore<Preferences>) : PrototypeRepository {
    constructor(context: Context) : this(context.applicationContext.prototypeDataStore)
    private val key = stringPreferencesKey("prototype_json_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Errors propagate to the UI. Never silently overwrite unreadable history with defaults.
    override suspend fun load(): PrototypeState = store.data.first()[key]?.let {
        json.decodeFromString<PrototypeState>(it)
    } ?: PrototypeState()

    override suspend fun save(state: PrototypeState) {
        store.edit { it[key] = json.encodeToString(state) }
    }
}
