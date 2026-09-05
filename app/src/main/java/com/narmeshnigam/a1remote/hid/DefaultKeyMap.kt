package com.narmeshnigam.a1remote.hid

/**
 * The default key map: the table in KEY_LAB.md, transcribed and nothing more.
 *
 * Confirmed rows are standard usages an Android 9 host is expected to honour. Candidate rows
 * are guesses and are marked as such so the UI can draw them unverified. Screen flip and
 * keystone have no mapping at all and return null — a wrong report would be worse than none.
 *
 * Gate 2 layers DataStore overrides and Key Lab promotions on top of this; this object stays
 * the shipped-defaults table.
 */
object DefaultKeyMap {
    private val bindings: Map<RemoteFunction, KeyBinding> = mapOf(
        RemoteFunction.UP to confirmed(HidReports.key(KeyboardUsage.UP_ARROW), "Up Arrow"),
        RemoteFunction.DOWN to confirmed(HidReports.key(KeyboardUsage.DOWN_ARROW), "Down Arrow"),
        RemoteFunction.LEFT to confirmed(HidReports.key(KeyboardUsage.LEFT_ARROW), "Left Arrow"),
        RemoteFunction.RIGHT to confirmed(HidReports.key(KeyboardUsage.RIGHT_ARROW), "Right Arrow"),
        RemoteFunction.OK to confirmed(HidReports.key(KeyboardUsage.ENTER), "Enter"),
        RemoteFunction.BACK to confirmed(HidReports.consumer(ConsumerUsage.AC_BACK), "AC Back"),
        RemoteFunction.HOME to confirmed(HidReports.consumer(ConsumerUsage.AC_HOME), "AC Home"),
        RemoteFunction.MENU to confirmed(HidReports.key(KeyboardUsage.APPLICATION), "Application"),
        RemoteFunction.VOLUME_UP to confirmed(HidReports.consumer(ConsumerUsage.VOLUME_UP), "Volume Up"),
        RemoteFunction.VOLUME_DOWN to confirmed(HidReports.consumer(ConsumerUsage.VOLUME_DOWN), "Volume Down"),
        RemoteFunction.MUTE to confirmed(HidReports.consumer(ConsumerUsage.MUTE), "Mute"),
        RemoteFunction.POWER to candidate(HidReports.consumer(ConsumerUsage.POWER), "Power"),
        RemoteFunction.FOCUS_UP to candidate(HidReports.consumer(ConsumerUsage.ZOOM_IN), "Zoom In"),
        RemoteFunction.FOCUS_DOWN to candidate(HidReports.consumer(ConsumerUsage.ZOOM_OUT), "Zoom Out"),
        RemoteFunction.SOURCE to candidate(HidReports.consumer(ConsumerUsage.MEDIA_SELECT_TV), "Media Select TV"),
        RemoteFunction.SCREEN_FLIP to unmapped(),
        RemoteFunction.KEYSTONE to unmapped(),
    )

    /**
     * The binding for [function]. Never null — every function is in the table — but its
     * [KeyBinding.report] is null when nothing is mapped.
     */
    operator fun get(function: RemoteFunction): KeyBinding = bindings.getValue(function)

    /** The alternate Back mapping KEY_LAB.md records, for Key Lab to fall back on. */
    fun backAlternate(): HidReport = HidReports.key(KeyboardUsage.ESCAPE)

    private fun confirmed(report: HidReport, usageName: String) = KeyBinding(report, KeyStatus.CONFIRMED, usageName)

    private fun candidate(report: HidReport, usageName: String) = KeyBinding(report, KeyStatus.CANDIDATE, usageName)

    private fun unmapped() = KeyBinding(null, KeyStatus.UNMAPPED, "unknown")
}
