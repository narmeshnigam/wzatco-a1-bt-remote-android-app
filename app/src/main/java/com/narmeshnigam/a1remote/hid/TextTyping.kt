package com.narmeshnigam.a1remote.hid

/**
 * The modifier bits of the keyboard report's first byte, from the HID Usage Tables.
 *
 * Only shift is needed to type: the app never sends a chord a person did not ask for.
 */
object KeyboardModifier {
    const val NONE = 0x00
    const val LEFT_SHIFT = 0x02
}

/**
 * One key held for one moment: a usage and whatever modifier is down with it.
 *
 * @param label how the wire log names it. Typed characters share one label rather than each
 *   spelling itself out; the report bytes are in the log either way, so anything typed can be
 *   read back out of it — worth knowing before typing a password with the log open.
 */
data class Keystroke(val usage: Int, val modifiers: Int = KeyboardModifier.NONE, val label: String) {

    /** The report this keystroke puts on the wire. */
    val report: HidReport get() = HidReports.keyboard(modifiers, usage)
}

/**
 * Text as keystrokes: the US layout of the HID Usage Tables, transcribed.
 *
 * This is a statement about the *usage tables*, not about the A1. What character the projector
 * ends up showing for a given usage is decided by its own keyboard layout, and that is open
 * question 19 — letters and digits are the same in every Latin layout, punctuation is not.
 *
 * Pure Kotlin with no Android in it, because a wrong entry here types the wrong character into
 * a Wi-Fi password on a screen the user is squinting at from across the room.
 */
object TextTyping {

    /** The named keys the keyboard screen offers as their own buttons. */
    val ENTER = Keystroke(KeyboardUsage.ENTER, label = "Enter")
    val BACKSPACE = Keystroke(KeyboardUsage.BACKSPACE, label = "Backspace")
    val TAB = Keystroke(KeyboardUsage.TAB, label = "Tab")
    val SPACE = Keystroke(KeyboardUsage.SPACE, label = "Space")

    /** Usage 0x04 is `a`, and the alphabet runs up from there in order. */
    private const val FIRST_LETTER = 0x04

    /** Usage 0x1E is `1`; `9` is 0x26 and `0` breaks the run at 0x27. */
    private const val FIRST_DIGIT = 0x1E
    private const val ZERO = 0x27

    /** Unshifted punctuation, in usage order 0x2D..0x38. */
    private val PUNCTUATION = mapOf(
        '-' to 0x2D,
        '=' to 0x2E,
        '[' to 0x2F,
        ']' to 0x30,
        '\\' to 0x31,
        ';' to 0x33,
        '\'' to 0x34,
        '`' to 0x35,
        ',' to 0x36,
        '.' to 0x37,
        '/' to 0x38,
    )

    /** What the same keys type with shift held. `!` is shift-1, `_` is shift-hyphen, and so on. */
    private val SHIFTED = mapOf(
        '!' to 0x1E,
        '@' to 0x1F,
        '#' to 0x20,
        '$' to 0x21,
        '%' to 0x22,
        '^' to 0x23,
        '&' to 0x24,
        '*' to 0x25,
        '(' to 0x26,
        ')' to ZERO,
        '_' to 0x2D,
        '+' to 0x2E,
        '{' to 0x2F,
        '}' to 0x30,
        '|' to 0x31,
        ':' to 0x33,
        '"' to 0x34,
        '~' to 0x35,
        '<' to 0x36,
        '>' to 0x37,
        '?' to 0x38,
    )

    /**
     * The keystroke that types [char], or null when this keyboard has no key for it.
     *
     * Null is the honest answer for anything outside printable US-ASCII — an accented letter, an
     * emoji, a currency sign. Substituting a nearby character would put something into the
     * projector's field that the user did not type.
     */
    fun keystrokeFor(char: Char): Keystroke? = when {
        char in 'a'..'z' -> Keystroke(FIRST_LETTER + (char - 'a'), label = TEXT_KEY)
        char in 'A'..'Z' -> Keystroke(
            usage = FIRST_LETTER + (char - 'A'),
            modifiers = KeyboardModifier.LEFT_SHIFT,
            label = TEXT_KEY,
        )

        char in '1'..'9' -> Keystroke(FIRST_DIGIT + (char - '1'), label = TEXT_KEY)
        char == '0' -> Keystroke(ZERO, label = TEXT_KEY)
        char == ' ' -> SPACE
        char == '\n' -> ENTER
        char == '\t' -> TAB
        PUNCTUATION.containsKey(char) -> Keystroke(PUNCTUATION.getValue(char), label = TEXT_KEY)
        SHIFTED.containsKey(char) -> Keystroke(
            usage = SHIFTED.getValue(char),
            modifiers = KeyboardModifier.LEFT_SHIFT,
            label = TEXT_KEY,
        )

        else -> null
    }

    /** The characters of [text] this keyboard cannot type, each named once, in first-seen order. */
    fun untypable(text: String): List<Char> = text.filter { keystrokeFor(it) == null }.toSet().toList()

    /**
     * [text] as the keystrokes that would type it, or null when any character has no key.
     *
     * All or nothing on purpose: half a password in a field is worse than none, because the user
     * cannot see which half arrived.
     */
    fun keystrokesFor(text: String): List<Keystroke>? = text.map { char -> keystrokeFor(char) ?: return null }

    /** Every typed character logs under one label; the bytes beside it say which key it was. */
    private const val TEXT_KEY = "Text key"
}
