package com.narmeshnigam.a1remote.hid

/**
 * The twelve remote functions of BUILD_SPEC §1, enumerated one key at a time.
 *
 * Focus ±, Source, Screen flip and Keystone are deliberately absent. Nothing the app shipped
 * for them moves the A1, so it no longer carries buttons it cannot drive. That is a decision
 * about the remote, not a finding about the projector: what drives those functions is still
 * open in `docs/OPEN_QUESTIONS.md`.
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
}

/**
 * How much is actually known about a function's mapping, per the status column of KEY_LAB.md.
 *
 * Only [CONFIRMED] may be drawn in the verified style. [CANDIDATE] is a guess shipped as the
 * first Key Lab candidate; [UNMAPPED] has no report at all until Key Lab finds one — a state a
 * promoted override can still be reset back to.
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
