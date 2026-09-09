package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text to keystrokes, character by character.
 *
 * These are transcription checks against the HID Usage Tables. A wrong entry here types the
 * wrong character into a field on the projector, and the user finds out from a Wi-Fi password
 * that will not connect — so the whole printable range is walked rather than sampled.
 */
class TextTypingTest {

    private fun usageOf(char: Char): Int? = TextTyping.keystrokeFor(char)?.usage

    private fun shifted(char: Char): Boolean = TextTyping.keystrokeFor(char)?.modifiers == KeyboardModifier.LEFT_SHIFT

    @Test
    fun `the alphabet runs from usage 0x04 in order`() {
        assertEquals(0x04, usageOf('a'))
        assertEquals(0x1D, usageOf('z'))
        ('a'..'z').forEachIndexed { index, char ->
            assertEquals(char.toString(), 0x04 + index, usageOf(char))
            assertTrue("$char must not be shifted", !shifted(char))
        }
    }

    @Test
    fun `a capital is the same key with shift, never a different usage`() {
        ('A'..'Z').forEach { char ->
            assertEquals(char.toString(), usageOf(char.lowercaseChar()), usageOf(char))
            assertTrue("$char must be shifted", shifted(char))
        }
    }

    @Test
    fun `digits run 1 to 9 from 0x1E and zero breaks the run at 0x27`() {
        ('1'..'9').forEachIndexed { index, char ->
            assertEquals(char.toString(), 0x1E + index, usageOf(char))
        }
        assertEquals(0x27, usageOf('0'))
        ('0'..'9').forEach { char -> assertTrue("$char must not be shifted", !shifted(char)) }
    }

    @Test
    fun `a shifted symbol is its own unshifted key with shift held`() {
        // Each pair is what one physical key types without and with shift.
        val pairs = listOf(
            '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
            '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')',
            '-' to '_', '=' to '+', '[' to '{', ']' to '}', '\\' to '|',
            ';' to ':', '\'' to '"', '`' to '~', ',' to '<', '.' to '>', '/' to '?',
        )
        pairs.forEach { (plain, shift) ->
            assertEquals("$plain/$shift share a key", usageOf(plain), usageOf(shift))
            assertTrue("$shift must be shifted", shifted(shift))
            assertTrue("$plain must not be shifted", !shifted(plain))
        }
    }

    @Test
    fun `every printable ASCII character has a key`() {
        (' '..'~').forEach { char ->
            // Built without format(): the character under test can itself be a percent sign.
            assertTrue("no key for code " + char.code, TextTyping.keystrokeFor(char) != null)
        }
    }

    @Test
    fun `space, newline and tab are the named keys, not characters of their own`() {
        assertEquals(TextTyping.SPACE, TextTyping.keystrokeFor(' '))
        assertEquals(TextTyping.ENTER, TextTyping.keystrokeFor('\n'))
        assertEquals(TextTyping.TAB, TextTyping.keystrokeFor('\t'))
        assertEquals(0x2C, TextTyping.SPACE.usage)
        assertEquals(0x28, TextTyping.ENTER.usage)
        assertEquals(0x2B, TextTyping.TAB.usage)
        assertEquals(0x2A, TextTyping.BACKSPACE.usage)
    }

    @Test
    fun `a character with no key is refused rather than approximated`() {
        // An accented letter is the case that matters: 'e' would be a plausible substitute for
        // 'é' and would put something in the projector's field that nobody typed.
        listOf('\u00E9', '\u20AC', '\u00A3', '\u2014', '\u201C', '\u00A0', '\u00DF').forEach { char ->
            assertNull("'$char' must have no key", TextTyping.keystrokeFor(char))
        }
    }

    @Test
    fun `a whole string becomes keystrokes in order`() {
        val keystrokes = TextTyping.keystrokesFor("Hi 9!")

        assertEquals(5, keystrokes?.size)
        assertEquals(listOf(0x0B, 0x0C, 0x2C, 0x26, 0x1E), keystrokes?.map { it.usage })
        assertEquals(
            listOf(KeyboardModifier.LEFT_SHIFT, 0, 0, 0, KeyboardModifier.LEFT_SHIFT),
            keystrokes?.map { it.modifiers },
        )
    }

    @Test
    fun `a string with one untypable character sends nothing at all`() {
        // Half a password in a field is worse than none: the user cannot see which half arrived.
        assertNull(TextTyping.keystrokesFor("café"))
        assertEquals(listOf('é'), TextTyping.untypable("café"))
        assertEquals(listOf('é', '€'), TextTyping.untypable("café €5 café"))
        assertEquals(emptyList<Char>(), TextTyping.untypable("Room-101_wifi!"))
    }

    @Test
    fun `an empty string is typable and types nothing`() {
        assertEquals(emptyList<Keystroke>(), TextTyping.keystrokesFor(""))
        assertEquals(emptyList<Char>(), TextTyping.untypable(""))
    }

    @Test
    fun `every keystroke builds a report the descriptor can carry`() {
        (' '..'~').mapNotNull(TextTyping::keystrokeFor).forEach { keystroke ->
            val report = keystroke.report
            assertEquals(HidDescriptor.REPORT_ID_KEYBOARD, report.id)
            assertEquals(HidDescriptor.KEYBOARD_REPORT_SIZE, report.data.size)
            assertEquals("modifier byte", keystroke.modifiers.toByte(), report.data[0])
            assertEquals("reserved byte stays zero", 0.toByte(), report.data[1])
            assertEquals("first key slot", keystroke.usage.toByte(), report.data[2])
        }
    }
}
