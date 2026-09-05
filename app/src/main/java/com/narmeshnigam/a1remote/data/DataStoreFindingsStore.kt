package com.narmeshnigam.a1remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.findingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "key_lab_findings")

/**
 * DataStore-backed findings, held as one JSON array under a single key.
 *
 * One key rather than one per row because a findings file is read and written whole, and
 * because the export is then the same encoder the storage uses — there is no second
 * representation to drift.
 *
 * Unreadable stored text yields an empty list rather than an exception. A findings file is
 * evidence, and evidence the app cannot read is evidence it does not have; crashing the Key Lab
 * screen would only cost the operator the session in front of the projector.
 */
class DataStoreFindingsStore(private val context: Context) : FindingsStore {

    override val findings: Flow<List<Finding>> = context.findingsDataStore.data.map { preferences ->
        val stored = preferences[RESULTS] ?: return@map emptyList()
        runCatching { FindingsJson.decodeResults(stored) }.getOrDefault(emptyList())
    }

    override suspend fun add(finding: Finding) {
        context.findingsDataStore.edit { preferences ->
            val current = preferences[RESULTS]
                ?.let { stored -> runCatching { FindingsJson.decodeResults(stored) }.getOrDefault(emptyList()) }
                .orEmpty()
            preferences[RESULTS] = FindingsJson.encodeResults(current + finding)
        }
    }

    override suspend fun clear() {
        context.findingsDataStore.edit { preferences -> preferences.remove(RESULTS) }
    }

    private companion object {
        val RESULTS = stringPreferencesKey("results")
    }
}
