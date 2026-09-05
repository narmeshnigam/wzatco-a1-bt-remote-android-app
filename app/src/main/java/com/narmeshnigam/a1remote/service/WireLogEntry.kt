package com.narmeshnigam.a1remote.service

/**
 * One line of the diagnostic wire log: what was attempted and what came back.
 *
 * @param atMillis wall-clock time of the entry
 * @param label the function or platform call
 * @param detail the report bytes or the returned value
 */
data class WireLogEntry(val atMillis: Long, val label: String, val detail: String)
