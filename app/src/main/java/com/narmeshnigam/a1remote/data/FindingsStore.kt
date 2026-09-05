package com.narmeshnigam.a1remote.data

import kotlinx.coroutines.flow.Flow

/**
 * Persistence for what Key Lab has learned.
 *
 * Separate from [KeyMapStore] because the two answer different questions: the key map holds the
 * one usage that works, this holds every usage that was tried and what it did. The misses are
 * the useful part when a function turns out to be unreachable — they are the evidence.
 *
 * An interface, so the Key Lab logic is testable without a device.
 */
interface FindingsStore {
    /** Emits the recorded findings oldest first, and again on every change. */
    val findings: Flow<List<Finding>>

    /** Appends one finding. Nothing is ever overwritten: a second attempt is a second row. */
    suspend fun add(finding: Finding)

    suspend fun clear()
}
