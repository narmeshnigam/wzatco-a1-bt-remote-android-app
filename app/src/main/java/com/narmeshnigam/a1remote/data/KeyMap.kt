package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.DefaultKeyMap
import com.narmeshnigam.a1remote.hid.HidReport
import com.narmeshnigam.a1remote.hid.KeyBinding
import com.narmeshnigam.a1remote.hid.KeyStatus
import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * `RemoteFunction` → report (BUILD_SPEC §3).
 *
 * Ships with the table in KEY_LAB.md, is overridden by Key Lab findings, and reloads at runtime
 * without a restart: a confirmed candidate flips its key from unverified to verified while the
 * operator is still looking at the projector.
 */
class KeyMap(private val store: KeyMapStore, scope: CoroutineScope) {

    private val _bindings = MutableStateFlow(DefaultKeyMap.all())

    /** The live table. Every [RemoteFunction] is present; a report of null means unmapped. */
    val bindings: StateFlow<Map<RemoteFunction, KeyBinding>> = _bindings.asStateFlow()

    init {
        scope.launch {
            store.overrides.collect { overrides ->
                _bindings.value = DefaultKeyMap.all() + overrides
            }
        }
    }

    operator fun get(function: RemoteFunction): KeyBinding = _bindings.value.getValue(function)

    /**
     * The report to transmit for [function], or null when nothing is mapped.
     *
     * Null is the honest answer for a function Key Lab has not resolved; a nearby function
     * would be worse than nothing.
     */
    fun reportFor(function: RemoteFunction): HidReport? = get(function).report

    /** True when [function] may be drawn in the verified style. */
    fun isVerified(function: RemoteFunction): Boolean = get(function).status == KeyStatus.CONFIRMED

    /** Records a Key Lab hit. The binding is confirmed by definition: the operator watched it work. */
    suspend fun promote(function: RemoteFunction, report: HidReport, usageName: String) {
        store.put(function, KeyBinding(report, KeyStatus.CONFIRMED, usageName))
    }

    /** Drops an override and falls back to the shipped default. */
    suspend fun reset(function: RemoteFunction) {
        store.remove(function)
    }

    /** Drops every override. */
    suspend fun resetAll() {
        store.clear()
    }
}
