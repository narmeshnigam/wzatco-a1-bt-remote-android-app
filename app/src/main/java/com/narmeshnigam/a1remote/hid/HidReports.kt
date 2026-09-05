package com.narmeshnigam.a1remote.hid

/**
 * Builders for the three report shapes. Every press on the wire is a pair: one of these
 * followed by the matching release. Invalid input throws rather than producing a report the
 * host would misread.
 */
object HidReports {
    /** All-zero keyboard report: nothing held. */
    fun keyboardRelease(): HidReport =
        HidReport(HidDescriptor.REPORT_ID_KEYBOARD, ByteArray(HidDescriptor.KEYBOARD_REPORT_SIZE))

    /** All-zero consumer report: nothing held. */
    fun consumerRelease(): HidReport =
        HidReport(HidDescriptor.REPORT_ID_CONSUMER, ByteArray(HidDescriptor.CONSUMER_REPORT_SIZE))

    /** All-zero mouse report: no buttons, no movement. */
    fun mouseRelease(): HidReport = HidReport(HidDescriptor.REPORT_ID_MOUSE, ByteArray(HidDescriptor.MOUSE_REPORT_SIZE))

    /**
     * Keyboard report: a modifier bitmap and up to six concurrently held usages.
     *
     * @param modifiers bitmap of the eight modifier usages 0xE0..0xE7
     * @param keys keyboard usages in 0x00..0xFF, at most [HidDescriptor.KEYBOARD_KEY_SLOTS]
     */
    fun keyboard(modifiers: Int = 0, vararg keys: Int): HidReport {
        require(modifiers in 0..0xFF) { "modifiers out of range: $modifiers" }
        require(keys.size <= HidDescriptor.KEYBOARD_KEY_SLOTS) {
            "at most ${HidDescriptor.KEYBOARD_KEY_SLOTS} keys per report, got ${keys.size}"
        }
        keys.forEach { usage ->
            require(usage in 0..HidDescriptor.KEYBOARD_USAGE_MAX) { "keyboard usage out of range: $usage" }
        }
        val data = ByteArray(HidDescriptor.KEYBOARD_REPORT_SIZE)
        data[0] = modifiers.toByte()
        // data[1] is the reserved byte and stays zero.
        keys.forEachIndexed { index, usage -> data[KEY_SLOT_OFFSET + index] = usage.toByte() }
        return HidReport(HidDescriptor.REPORT_ID_KEYBOARD, data)
    }

    /** Keyboard report holding exactly one usage and no modifiers — the common case. */
    fun key(usage: Int): HidReport = keyboard(keys = intArrayOf(usage))

    /**
     * Consumer-control report: one 16-bit usage, little-endian.
     *
     * @param usage a consumer usage in 0x0000..[HidDescriptor.CONSUMER_USAGE_MAX]
     */
    fun consumer(usage: Int): HidReport {
        require(usage in 0..HidDescriptor.CONSUMER_USAGE_MAX) {
            "consumer usage out of range (descriptor declares 0..0x%04X): 0x%04X"
                .format(HidDescriptor.CONSUMER_USAGE_MAX, usage)
        }
        val data = ByteArray(HidDescriptor.CONSUMER_REPORT_SIZE)
        data[0] = (usage and 0xFF).toByte()
        data[1] = ((usage shr 8) and 0xFF).toByte()
        return HidReport(HidDescriptor.REPORT_ID_CONSUMER, data)
    }

    /**
     * Mouse report with relative motion. Axis values outside the 8-bit signed range the
     * descriptor declares are clamped, not wrapped — a wrapped delta would send the pointer
     * the wrong way.
     */
    fun mouse(buttons: Int = MouseButton.NONE, dx: Int = 0, dy: Int = 0, wheel: Int = 0): HidReport {
        require(buttons in 0..BUTTON_MASK) { "buttons out of range: $buttons" }
        val data = ByteArray(HidDescriptor.MOUSE_REPORT_SIZE)
        data[0] = buttons.toByte()
        data[1] = clampAxis(dx)
        data[2] = clampAxis(dy)
        data[3] = clampAxis(wheel)
        return HidReport(HidDescriptor.REPORT_ID_MOUSE, data)
    }

    /** The release report matching [report], by report ID. */
    fun releaseFor(report: HidReport): HidReport = when (report.id) {
        HidDescriptor.REPORT_ID_KEYBOARD -> keyboardRelease()
        HidDescriptor.REPORT_ID_CONSUMER -> consumerRelease()
        HidDescriptor.REPORT_ID_MOUSE -> mouseRelease()
        else -> error("unknown report id ${report.id}")
    }

    private fun clampAxis(value: Int): Byte =
        value.coerceIn(HidDescriptor.MOUSE_AXIS_MIN, HidDescriptor.MOUSE_AXIS_MAX).toByte()

    private const val KEY_SLOT_OFFSET = 2
    private const val BUTTON_MASK = 0x07
}
