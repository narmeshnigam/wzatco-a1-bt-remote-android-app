package com.narmeshnigam.a1remote.hid

/**
 * One report on the wire: the report ID and its payload.
 *
 * The payload never carries the report ID — `BluetoothHidDevice.sendReport(device, id, data)`
 * takes the ID separately and prepends it itself.
 */
class HidReport(val id: Int, val data: ByteArray) {
    /** True when every payload byte is zero, i.e. this is a release report. */
    val isRelease: Boolean get() = data.all { it == 0.toByte() }

    fun hex(): String = data.joinToString(" ") { byte -> HEX_BYTE.format(byte) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HidReport) return false
        return id == other.id && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * id + data.contentHashCode()

    override fun toString(): String = "HidReport(id=$id, data=[${hex()}])"

    private companion object {
        const val HEX_BYTE = "%02X"
    }
}
