package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [KeyMapStore], so the key map is testable without a device. */
class FakeKeyMapStore(initial: Map<RemoteFunction, KeyBinding> = emptyMap()) : KeyMapStore {

    private val state = MutableStateFlow(initial)

    override val overrides: Flow<Map<RemoteFunction, KeyBinding>> = state

    override suspend fun put(function: RemoteFunction, binding: KeyBinding) {
        state.value = state.value + (function to binding)
    }

    override suspend fun remove(function: RemoteFunction) {
        state.value = state.value - function
    }

    override suspend fun clear() {
        state.value = emptyMap()
    }
}
