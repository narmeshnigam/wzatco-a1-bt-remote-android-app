package com.narmeshnigam.a1remote.ui

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription

private const val ATTACH_TIMEOUT_MS = 5_000L

/**
 * Waits until the key labelled [label] is in the semantics tree.
 *
 * On this ROM the rule's activity can attach its compose view a beat after `setContent` returns,
 * and a lookup made in that gap fails with "No compose hierarchies found" — a harness race seen
 * intermittently on the DN2101, never a key bug. Asking for the node without requiring a root
 * to exist yet turns the race into a plain wait.
 */
internal fun ComposeContentTestRule.awaitKey(label: String) {
    waitUntil(timeoutMillis = ATTACH_TIMEOUT_MS) {
        onAllNodesWithContentDescription(label)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }
}
