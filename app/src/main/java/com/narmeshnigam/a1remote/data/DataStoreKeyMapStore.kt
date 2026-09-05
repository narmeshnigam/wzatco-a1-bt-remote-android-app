package com.narmeshnigam.a1remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.narmeshnigam.a1remote.hid.BindingCodec
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keyMapDataStore: DataStore<Preferences> by preferencesDataStore(name = "key_map")

/**
 * DataStore-backed overrides, one preference per function, encoded by [BindingCodec].
 *
 * An entry that cannot be decoded is dropped rather than repaired: a binding the app is not
 * certain of has to fall back to the shipped default, not to a best guess.
 */
class DataStoreKeyMapStore(private val context: Context) : KeyMapStore {

    override val overrides: Flow<Map<RemoteFunction, KeyBinding>> =
        context.keyMapDataStore.data.map { preferences ->
            RemoteFunction.entries.mapNotNull { function ->
                val encoded = preferences[keyOf(function)] ?: return@mapNotNull null
                val binding = BindingCodec.decode(encoded) ?: return@mapNotNull null
                function to binding
            }.toMap()
        }

    override suspend fun put(function: RemoteFunction, binding: KeyBinding) {
        val encoded = BindingCodec.encode(binding) ?: return
        context.keyMapDataStore.edit { preferences -> preferences[keyOf(function)] = encoded }
    }

    override suspend fun remove(function: RemoteFunction) {
        context.keyMapDataStore.edit { preferences -> preferences.remove(keyOf(function)) }
    }

    override suspend fun clear() {
        context.keyMapDataStore.edit { preferences -> preferences.clear() }
    }

    private fun keyOf(function: RemoteFunction) = stringPreferencesKey("binding_${function.name}")
}
