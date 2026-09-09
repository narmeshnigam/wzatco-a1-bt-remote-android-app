package com.narmeshnigam.a1remote.hid

/**
 * HID usage constants. These are USB HID Usage Tables facts, not claims about the A1 —
 * whether the projector honours any given usage is what Key Lab is for.
 */
object KeyboardUsage {
    const val RIGHT_ARROW = 0x4F
    const val LEFT_ARROW = 0x50
    const val DOWN_ARROW = 0x51
    const val UP_ARROW = 0x52
    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val APPLICATION = 0x65
}

object ConsumerUsage {
    const val POWER = 0x0030
    const val MUTE = 0x00E2
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
    const val AC_HOME = 0x0223
    const val AC_BACK = 0x0224
}

object MouseButton {
    const val NONE = 0x00
    const val LEFT = 0x01
    const val RIGHT = 0x02
    const val MIDDLE = 0x04
}
