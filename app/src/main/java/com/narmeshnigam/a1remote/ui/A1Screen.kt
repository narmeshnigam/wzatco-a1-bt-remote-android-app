package com.narmeshnigam.a1remote.ui

/** The four screens of BUILD_SPEC §6, in tab-bar order. */
enum class A1Screen(val tabLabel: String) {
    KEYPAD("Keypad"),
    CURSOR("Trackpad"),

    // Typing into a field on the projector. It took the tab Fix Keys used to hold: fixing a key
    // is something the operator does once, typing a Wi-Fi password is something they do at the
    // moment they need it, and only one of those earns a permanent tab.
    TEXT("Keyboard"),
    SETUP("Setup"),
}
