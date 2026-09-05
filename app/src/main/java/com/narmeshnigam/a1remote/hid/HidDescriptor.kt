package com.narmeshnigam.a1remote.hid

/**
 * The HID report descriptor published in the SDP record, per BUILD_SPEC §4.
 *
 * Three top-level collections with distinct report IDs, so a keyboard press, a consumer-control
 * press and a cursor move can never collide on the wire.
 *
 * Pure Kotlin by contract: nothing in this package may import from `android.*`.
 */
object HidDescriptor {
    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_CONSUMER = 2
    const val REPORT_ID_MOUSE = 3

    /** 1 modifier byte + 1 reserved byte + 6 key slots. */
    const val KEYBOARD_REPORT_SIZE = 8

    /** One 16-bit usage field, little-endian. */
    const val CONSUMER_REPORT_SIZE = 2

    /** Buttons byte + relative X + relative Y + wheel. */
    const val MOUSE_REPORT_SIZE = 4

    /** Key slots in one keyboard report. */
    const val KEYBOARD_KEY_SLOTS = 6

    /** Highest keyboard usage the descriptor declares. */
    const val KEYBOARD_USAGE_MAX = 0xFF

    /**
     * Highest consumer usage the descriptor declares.
     *
     * Deliberately 0x03FF and not 0xFFFF: the Linux/Android HID parser expands a usage range
     * entry by entry and caps the total, so a full 16-bit range makes the whole descriptor
     * fail to parse on the host. 0x03FF covers every usage in KEY_LAB.md, candidates and
     * sweep ranges included, and this is the ceiling Key Lab's manual mode must enforce.
     */
    const val CONSUMER_USAGE_MAX = 0x03FF

    /** Relative mouse axes are 8-bit signed. */
    const val MOUSE_AXIS_MIN = -127
    const val MOUSE_AXIS_MAX = 127

    /** Report 1 — Generic Desktop / Keyboard. */
    fun keyboardCollection(): ByteArray = KEYBOARD.copyOf()

    /** Report 2 — Consumer Control. */
    fun consumerCollection(): ByteArray = CONSUMER.copyOf()

    /** Report 3 — Generic Desktop / Mouse, relative. */
    fun mouseCollection(): ByteArray = MOUSE.copyOf()

    /** The complete descriptor handed to `BluetoothHidDeviceAppSdpSettings`. */
    fun bytes(): ByteArray = DESCRIPTOR.copyOf()

    private val KEYBOARD = items(
        0x05, 0x01, //  Usage Page (Generic Desktop)
        0x09, 0x06, //  Usage (Keyboard)
        0xA1, 0x01, //  Collection (Application)
        0x85, REPORT_ID_KEYBOARD, //    Report ID (1)
        0x05, 0x07, //    Usage Page (Keyboard/Keypad)
        0x19, 0xE0, //    Usage Minimum (Left Control)
        0x29, 0xE7, //    Usage Maximum (Right GUI)
        0x15, 0x00, //    Logical Minimum (0)
        0x25, 0x01, //    Logical Maximum (1)
        0x75, 0x01, //    Report Size (1)
        0x95, 0x08, //    Report Count (8)
        0x81, 0x02, //    Input (Data, Variable, Absolute) -- modifier byte
        0x95, 0x01, //    Report Count (1)
        0x75, 0x08, //    Report Size (8)
        0x81, 0x01, //    Input (Constant) -- reserved byte
        0x95, 0x06, //    Report Count (6)
        0x75, 0x08, //    Report Size (8)
        0x15, 0x00, //    Logical Minimum (0)
        0x26, 0xFF, 0x00, //  Logical Maximum (255)
        0x05, 0x07, //    Usage Page (Keyboard/Keypad)
        0x19, 0x00, //    Usage Minimum (0)
        0x2A, 0xFF, 0x00, //  Usage Maximum (255)
        0x81, 0x00, //    Input (Data, Array, Absolute) -- 6 key slots
        0xC0, //        End Collection
    )

    private val CONSUMER = items(
        0x05, 0x0C, //  Usage Page (Consumer)
        0x09, 0x01, //  Usage (Consumer Control)
        0xA1, 0x01, //  Collection (Application)
        0x85, REPORT_ID_CONSUMER, //    Report ID (2)
        0x15, 0x00, //    Logical Minimum (0)
        0x26, 0xFF, 0x03, //  Logical Maximum (0x03FF)
        0x19, 0x00, //    Usage Minimum (0)
        0x2A, 0xFF, 0x03, //  Usage Maximum (0x03FF)
        0x75, 0x10, //    Report Size (16)
        0x95, 0x01, //    Report Count (1)
        0x81, 0x00, //    Input (Data, Array, Absolute) -- one 16-bit usage
        0xC0, //        End Collection
    )

    private val MOUSE = items(
        0x05, 0x01, //  Usage Page (Generic Desktop)
        0x09, 0x02, //  Usage (Mouse)
        0xA1, 0x01, //  Collection (Application)
        0x85, REPORT_ID_MOUSE, //   Report ID (3)
        0x09, 0x01, //    Usage (Pointer)
        0xA1, 0x00, //    Collection (Physical)
        0x05, 0x09, //      Usage Page (Button)
        0x19, 0x01, //      Usage Minimum (Button 1)
        0x29, 0x03, //      Usage Maximum (Button 3)
        0x15, 0x00, //      Logical Minimum (0)
        0x25, 0x01, //      Logical Maximum (1)
        0x75, 0x01, //      Report Size (1)
        0x95, 0x03, //      Report Count (3)
        0x81, 0x02, //      Input (Data, Variable, Absolute) -- 3 buttons
        0x75, 0x05, //      Report Size (5)
        0x95, 0x01, //      Report Count (1)
        0x81, 0x01, //      Input (Constant) -- padding to a byte
        0x05, 0x01, //      Usage Page (Generic Desktop)
        0x09, 0x30, //      Usage (X)
        0x09, 0x31, //      Usage (Y)
        0x09, 0x38, //      Usage (Wheel)
        0x15, 0x81, //      Logical Minimum (-127)
        0x25, 0x7F, //      Logical Maximum (127)
        0x75, 0x08, //      Report Size (8)
        0x95, 0x03, //      Report Count (3)
        0x81, 0x06, //      Input (Data, Variable, Relative)
        0xC0, //          End Collection
        0xC0, //        End Collection
    )

    private val DESCRIPTOR = KEYBOARD + CONSUMER + MOUSE

    private fun items(vararg item: Int): ByteArray = ByteArray(item.size) { item[it].toByte() }
}
