package com.narmeshnigam.a1remote.hid

/**
 * Which of the two keyed report shapes a usage belongs to.
 *
 * The mouse collection has no place here: it carries deltas, not a usage a candidate could
 * name, so nothing Key Lab tries or records is ever a mouse report.
 *
 * [wireName] is the `report` field of the findings schema in KEY_LAB.md and the kind field
 * [BindingCodec] writes; the two use the same two words on purpose.
 */
enum class ReportKind(val wireName: String) {
    KEYBOARD("keyboard"),
    CONSUMER("consumer"),
    ;

    /** The highest usage the descriptor declares for this kind. */
    val usageMax: Int
        get() = when (this) {
            KEYBOARD -> HidDescriptor.KEYBOARD_USAGE_MAX
            CONSUMER -> HidDescriptor.CONSUMER_USAGE_MAX
        }

    /** Builds the single-usage report for [usage]. Throws when [usage] is above [usageMax]. */
    fun report(usage: Int): HidReport = when (this) {
        KEYBOARD -> HidReports.key(usage)
        CONSUMER -> HidReports.consumer(usage)
    }

    /**
     * [usage] as the findings schema writes it: `0x3C` for a keyboard usage, `0x022D` for a
     * consumer one. The widths differ because the sample file in KEY_LAB.md uses both.
     */
    fun hex(usage: Int): String = when (this) {
        KEYBOARD -> "0x%02X".format(usage)
        CONSUMER -> "0x%04X".format(usage)
    }

    companion object {
        /** The kind [report] was built as, or null for a mouse report. */
        fun of(report: HidReport): ReportKind? = when (report.id) {
            HidDescriptor.REPORT_ID_KEYBOARD -> KEYBOARD
            HidDescriptor.REPORT_ID_CONSUMER -> CONSUMER
            else -> null
        }

        /** The kind whose [wireName] is [name], or null. Used when reading findings back. */
        fun byWireName(name: String): ReportKind? = entries.firstOrNull { it.wireName == name }
    }
}
