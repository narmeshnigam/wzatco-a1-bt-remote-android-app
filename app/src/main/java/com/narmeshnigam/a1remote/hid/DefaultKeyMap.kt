package com.narmeshnigam.a1remote.hid

/**
 * The default key map: the table in KEY_LAB.md, transcribed and nothing more.
 *
 * Confirmed rows are standard usages an Android 9 host is expected to honour. Power is still a
 * guess and is marked as such so the UI draws it unverified — no code tried on the A1 has
 * powered it off yet, and a key that claims otherwise would be lying.
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
    )

    /**
     * The binding for [function]. Never null — every function is in the table — but its
     * [KeyBinding.report] is null when nothing is mapped.
     */
    operator fun get(function: RemoteFunction): KeyBinding = bindings.getValue(function)

    /** The whole shipped table. */
    fun all(): Map<RemoteFunction, KeyBinding> = bindings

    /** The alternate Back mapping KEY_LAB.md records, for Key Lab to fall back on. */
    fun backAlternate(): HidReport = HidReports.key(KeyboardUsage.ESCAPE)

    private fun confirmed(report: HidReport, usageName: String) = KeyBinding(report, KeyStatus.CONFIRMED, usageName)

    private fun candidate(report: HidReport, usageName: String) = KeyBinding(report, KeyStatus.CANDIDATE, usageName)
}
