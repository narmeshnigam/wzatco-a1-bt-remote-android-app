package com.narmeshnigam.a1remote.vm

import com.narmeshnigam.a1remote.hid.RemoteFunction
import kotlinx.coroutines.delay

/**
 * The auto-repeat timing of BUILD_SPEC §5: hold for 400 ms, then a repeat every 90 ms.
 *
 * Pure Kotlin and suspending, so the timing is testable on virtual time rather than by holding a
 * finger on a phone. The initial press is *not* emitted here — the caller has already sent it on
 * key-down — so the first tick this produces is the 400 ms one.
 */
object AutoRepeat {
    const val INITIAL_DELAY_MS = 400L
    const val INTERVAL_MS = 90L

    /**
     * Repeats [onTick] until the calling coroutine is cancelled, which is what a finger lifting
     * does. Never returns normally.
     */
    suspend fun run(onTick: () -> Unit): Nothing {
        delay(INITIAL_DELAY_MS)
        while (true) {
            onTick()
            delay(INTERVAL_MS)
        }
    }
}

/** The functions that auto-repeat, per BUILD_SPEC §5. Nothing else may. */
enum class RepeatBehaviour {
    /** One report per press. OK, Back, Home, Menu, Mute, Power. */
    SINGLE,

    /** Repeats while held: the D-pad and Volume. */
    REPEATING,
}

/**
 * Whether holding [this] should keep sending.
 *
 * The list is BUILD_SPEC §5's, and it is short on purpose: repeating Back or Home would walk the
 * projector out of wherever the user was, and repeating Power is unthinkable.
 */
fun RemoteFunction.repeatBehaviour(): RepeatBehaviour = when (this) {
    RemoteFunction.UP,
    RemoteFunction.DOWN,
    RemoteFunction.LEFT,
    RemoteFunction.RIGHT,
    RemoteFunction.VOLUME_UP,
    RemoteFunction.VOLUME_DOWN,
    -> RepeatBehaviour.REPEATING

    RemoteFunction.OK,
    RemoteFunction.BACK,
    RemoteFunction.HOME,
    RemoteFunction.MENU,
    RemoteFunction.MUTE,
    RemoteFunction.POWER,
    -> RepeatBehaviour.SINGLE
}
