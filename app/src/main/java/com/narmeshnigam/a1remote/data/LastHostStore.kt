package com.narmeshnigam.a1remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Remembers the last host the phone actually connected to, so the app can reach for it again on
 * the next start (BUILD_SPEC §7 — the remote should feel like it just resumes).
 *
 * Only the address is kept. The name is looked up from the live bond list when it is shown, so a
 * renamed device never displays a stale label, and nothing here is a claim about a device that
 * might no longer be bonded.
 */
class LastHostStore(private val context: Context) {

    /** The saved address, or null if the phone has never completed a connection. */
    suspend fun get(): String? = context.settingsDataStore.data.first()[KEY]

    suspend fun set(address: String) {
        context.settingsDataStore.edit { preferences -> preferences[KEY] = address }
    }

    suspend fun clear() {
        context.settingsDataStore.edit { preferences -> preferences.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("last_host_address")
    }
}
