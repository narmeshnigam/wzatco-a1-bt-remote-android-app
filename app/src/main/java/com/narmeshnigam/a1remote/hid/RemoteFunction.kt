package com.narmeshnigam.a1remote.hid

/**
 * The twelve remote functions of BUILD_SPEC §1, enumerated one key at a time.
 *
 * Names match the `function` field of the Key Lab findings schema in KEY_LAB.md.
 */
enum class RemoteFunction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    OK,
    BACK,
    HOME,
    MENU,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    POWER,
    FOCUS_UP,
    FOCUS_DOWN,
    SOURCE,
    SCREEN_FLIP,
    KEYSTONE,
}

/**
 * How much is actually known about a function's mapping, per the status column of KEY_LAB.md.
 *
 * Only [CONFIRMED] may be drawn in the verified style. [CANDIDATE] is a guess shipped as the
 * first Key Lab candidate; [UNMAPPED] has no report at all until Key Lab finds one.
 */
enum class KeyStatus {
    CONFIRMED,
    CANDIDATE,
    UNMAPPED,
}

/**
 * What the key map knows about one function.
 *
 * @param report the report to transmit, or null when nothing is mapped
 * @param status how much confidence that mapping carries
 * @param usageName the human name of the usage, for the wire log and the findings file
 */
data class KeyBinding(val report: HidReport?, val status: KeyStatus, val usageName: String)
