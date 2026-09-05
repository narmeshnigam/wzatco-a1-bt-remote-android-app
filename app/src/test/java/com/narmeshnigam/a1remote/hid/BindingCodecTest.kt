package com.narmeshnigam.a1remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BindingCodecTest {

    @Test
    fun `a keyboard binding round-trips exactly`() {
        val binding = KeyBinding(HidReports.key(0x3E), KeyStatus.CONFIRMED, "F5")
        val encoded = BindingCodec.encode(binding)
        assertEquals("keyboard:003E:CONFIRMED:F5", encoded)
        assertEquals(binding, BindingCodec.decode(requireNotNull(encoded)))
    }

    @Test
    fun `a consumer binding round-trips exactly`() {
        val binding = KeyBinding(HidReports.consumer(0x022D), KeyStatus.CONFIRMED, "Zoom In")
        val encoded = BindingCodec.encode(binding)
        assertEquals("consumer:022D:CONFIRMED:Zoom In", encoded)
        assertEquals(binding, BindingCodec.decode(requireNotNull(encoded)))
    }

    @Test
    fun `every default binding that has a report round-trips`() {
        RemoteFunction.entries.forEach { function ->
            val binding = DefaultKeyMap[function]
            val encoded = BindingCodec.encode(binding)
            if (binding.report == null) {
                assertNull("$function has no report and must not encode", encoded)
            } else {
                assertEquals("$function", binding, BindingCodec.decode(requireNotNull(encoded)))
            }
        }
    }

    @Test
    fun `a usage name containing the separator survives`() {
        val binding = KeyBinding(HidReports.consumer(0x0089), KeyStatus.CONFIRMED, "TV: select")
        assertEquals(binding, BindingCodec.decode(requireNotNull(BindingCodec.encode(binding))))
    }

    @Test
    fun `malformed text decodes to null rather than to a plausible binding`() {
        listOf(
            "",
            "consumer",
            "consumer:022D",
            "consumer:022D:CONFIRMED",
            "mouse:0001:CONFIRMED:Button 1",
            "consumer:ZZZZ:CONFIRMED:Zoom In",
            "consumer:022D:MAYBE:Zoom In",
            "consumer:FFFF:CONFIRMED:out of declared range",
            "keyboard:0100:CONFIRMED:out of declared range",
        ).forEach { text ->
            assertNull("\"$text\" must not decode", BindingCodec.decode(text))
        }
    }

    @Test
    fun `a multi-key report has no single-usage form and does not encode`() {
        val binding = KeyBinding(HidReports.keyboard(keys = intArrayOf(0x04, 0x05)), KeyStatus.CONFIRMED, "two")
        assertNull(BindingCodec.encode(binding))
    }

    @Test
    fun `a modified keyboard report does not encode`() {
        val binding =
            KeyBinding(HidReports.keyboard(modifiers = 0x02, keys = intArrayOf(0x04)), KeyStatus.CONFIRMED, "shift a")
        assertNull(BindingCodec.encode(binding))
    }

    @Test
    fun `a mouse report is not a key binding`() {
        val binding = KeyBinding(HidReports.mouse(buttons = MouseButton.LEFT), KeyStatus.CONFIRMED, "click")
        assertNull(BindingCodec.encode(binding))
    }
}
