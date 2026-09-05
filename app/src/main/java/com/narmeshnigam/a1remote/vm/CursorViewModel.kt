package com.narmeshnigam.a1remote.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.narmeshnigam.a1remote.hid.CursorMotion
import com.narmeshnigam.a1remote.hid.HidReports
import com.narmeshnigam.a1remote.hid.MouseButton
import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.service.HidLink
import com.narmeshnigam.a1remote.service.LinkState
import com.narmeshnigam.a1remote.service.SendResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The cursor screen's view of the link (BUILD_SPEC §6).
 *
 * It owns the one [CursorMotion] that carries a drag's fractional residue between touch frames.
 * That is why the acceleration lives in a held object rather than in the composable, which is
 * free to be recomposed or to leave the tree at any moment.
 */
class CursorViewModel(application: Application) : AndroidViewModel(application) {

    private val motion = CursorMotion()

    val link: StateFlow<LinkState> = HidLink.state

    private val _lastResult = MutableStateFlow<SendResult?>(null)
    val lastResult: StateFlow<SendResult?> = _lastResult.asStateFlow()

    /** Start of a drag: the previous gesture's leftover fraction is not this gesture's. */
    fun beginDrag() = motion.reset()

    /**
     * One touch frame of drag, in pixels.
     *
     * Motion is the one thing transmitted without a matching release — a delta is over the moment
     * it is delivered, so there is nothing to let go of.
     */
    fun move(dx: Float, dy: Float) {
        motion.step(dx, dy).forEach { report ->
            _lastResult.value = HidLink.sendMotion(report, MOVE_LABEL)
        }
    }

    /** A mouse button can be held, so it goes out as a down/up pair like any key. */
    fun leftClick() {
        val down = HidReports.mouse(buttons = MouseButton.LEFT)
        _lastResult.value = HidLink.sendPress(down, CLICK_LABEL)
    }

    /** Two-finger tap. Back is a mapped function, so it goes through the ordinary key path. */
    fun back() {
        _lastResult.value = HidLink.sendKey(RemoteFunction.BACK)
    }

    private companion object {
        const val MOVE_LABEL = "CURSOR move"
        const val CLICK_LABEL = "CURSOR left click"
    }
}
