package com.narmeshnigam.a1remote.ui

/** The four screens of BUILD_SPEC §6, in tab-bar order. */
enum class A1Screen(val tabLabel: String) {
    KEYPAD("Keypad"),
    CURSOR("Trackpad"),

    // "Fix Keys" is the user-facing name for what the specs call Key Lab: the tool that finds a
    // working code for the buttons the A1 ignores.
    KEY_LAB("Fix Keys"),
    SETUP("Setup"),
}
