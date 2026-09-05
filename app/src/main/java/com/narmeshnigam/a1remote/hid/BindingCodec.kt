package com.narmeshnigam.a1remote.hid

/**
 * The on-disk form of a key binding: `report:usage:status:name`, for example
 * `consumer:022D:CONFIRMED:Zoom In`.
 *
 * Pure Kotlin and deliberately dull. A binding that cannot be read back exactly as it was
 * written would silently give the projector a different key than Key Lab confirmed, so the
 * decoder refuses anything it does not fully understand rather than guessing.
 */
object BindingCodec {
    private const val SEPARATOR = ":"
    private const val FIELD_COUNT = 4
    private const val KEYBOARD = "keyboard"
    private const val CONSUMER = "consumer"
    private const val HEX = "%04X"

    /** Encodes a binding that has a report. Returns null for an unmapped binding. */
    fun encode(binding: KeyBinding): String? {
        val report = binding.report ?: return null
        val kind = kindOf(report) ?: return null
        val usage = usageOf(report) ?: return null
        return listOf(kind, HEX.format(usage), binding.status.name, binding.usageName)
            .joinToString(SEPARATOR)
    }

    /** Decodes a binding, or null if the text is not one this version wrote. */
    fun decode(text: String): KeyBinding? {
        val parts = text.split(SEPARATOR, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val usage = parts[1].toIntOrNull(radix = 16) ?: return null
        val status = KeyStatus.entries.firstOrNull { it.name == parts[2] } ?: return null
        val report = runCatching {
            when (parts[0]) {
                KEYBOARD -> HidReports.key(usage)
                CONSUMER -> HidReports.consumer(usage)
                else -> null
            }
        }.getOrNull() ?: return null
        return KeyBinding(report, status, parts[3])
    }

    private fun kindOf(report: HidReport): String? = when (report.id) {
        HidDescriptor.REPORT_ID_KEYBOARD -> KEYBOARD
        HidDescriptor.REPORT_ID_CONSUMER -> CONSUMER
        else -> null
    }

    /** The single usage a keyboard or consumer report carries, or null if it carries more. */
    private fun usageOf(report: HidReport): Int? = when (report.id) {
        HidDescriptor.REPORT_ID_KEYBOARD -> keyboardUsageOf(report.data)
        HidDescriptor.REPORT_ID_CONSUMER -> (report.data[0].toInt() and 0xFF) or
            ((report.data[1].toInt() and 0xFF) shl 8)

        else -> null
    }

    private fun keyboardUsageOf(data: ByteArray): Int? {
        if (data[0] != 0.toByte()) return null // modifiers are not part of the stored form
        val slots = data.drop(2).map { it.toInt() and 0xFF }.filter { it != 0 }
        return slots.singleOrNull()
    }
}
