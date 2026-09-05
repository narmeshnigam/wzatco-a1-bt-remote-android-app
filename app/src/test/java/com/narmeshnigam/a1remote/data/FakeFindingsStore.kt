package com.narmeshnigam.a1remote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [FindingsStore], so the Key Lab loop is testable without a device. */
class FakeFindingsStore(initial: List<Finding> = emptyList()) : FindingsStore {

    private val state = MutableStateFlow(initial)

    override val findings: Flow<List<Finding>> = state

    /** What has been filed so far, for a test to assert against directly. */
    val recorded: List<Finding> get() = state.value

    override suspend fun add(finding: Finding) {
        state.value = state.value + finding
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}
