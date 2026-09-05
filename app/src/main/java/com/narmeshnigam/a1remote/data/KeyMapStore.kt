package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for Key Lab's findings: the bindings that override the shipped defaults.
 *
 * An interface, so the key map is testable without a device — the whole point of the fake host
 * in TEST_PLAN.md.
 */
interface KeyMapStore {
    /** Emits the current overrides, and again on every change. */
    val overrides: Flow<Map<RemoteFunction, KeyBinding>>

    suspend fun put(function: RemoteFunction, binding: KeyBinding)

    suspend fun remove(function: RemoteFunction)

    suspend fun clear()
}
