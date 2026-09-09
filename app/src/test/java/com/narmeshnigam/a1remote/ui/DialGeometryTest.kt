package com.narmeshnigam.a1remote.ui

import com.narmeshnigam.a1remote.hid.RemoteFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Which part of the circular D-pad a touch lands on.
 *
 * The dial has no buttons to hit-test, so this arithmetic *is* the D-pad: get a sign wrong and
 * the remote walks the projector's menu the opposite way. It is pure Kotlin on purpose, so it
 * can be checked here rather than by holding a thumb on a phone.
 */
class DialGeometryTest {

    private val size = 258f
    private val centre = size / 2f

    /** A point [radius] from the centre at [degrees], where 0° is 3 o'clock, clockwise. */
    private fun at(degrees: Double, radius: Float): Pair<Float, Float> {
        val radians = Math.toRadians(degrees)
        return centre + (cos(radians) * radius).toFloat() to centre + (sin(radians) * radius).toFloat()
    }

    private fun functionAt(degrees: Double, radius: Float = size * 0.4f): RemoteFunction? {
        val (x, y) = at(degrees, radius)
        return functionAt(x, y, size, size)
    }

    @Test
    fun `each quadrant belongs to the arrow drawn in it`() {
        assertEquals(RemoteFunction.RIGHT, functionAt(0.0))
        assertEquals(RemoteFunction.DOWN, functionAt(90.0))
        assertEquals(RemoteFunction.LEFT, functionAt(180.0))
        assertEquals(RemoteFunction.UP, functionAt(270.0))
    }

    @Test
    fun `the whole of an arc belongs to its arrow, not just the point the icon sits on`() {
        // Up spans 225° to 315°: this is what lets a thumb land anywhere along the top.
        listOf(226.0, 250.0, 270.0, 290.0, 314.0).forEach { degrees ->
            assertEquals("$degrees°", RemoteFunction.UP, functionAt(degrees))
        }
    }

    @Test
    fun `the diagonals hinge the quadrants without leaving a gap`() {
        // Every boundary belongs to exactly one arrow, and no angle at all is unclaimed.
        (0 until 360).forEach { degrees ->
            val hit = functionAt(degrees.toDouble())
            assertTrue(
                "$degrees° must be one of the four arrows, was $hit",
                hit in setOf(RemoteFunction.UP, RemoteFunction.DOWN, RemoteFunction.LEFT, RemoteFunction.RIGHT),
            )
        }
    }

    @Test
    fun `the hub is OK whichever way the thumb comes at it`() {
        assertEquals(RemoteFunction.OK, functionAt(0.0, radius = 0f))
        listOf(0.0, 90.0, 180.0, 270.0, 45.0).forEach { degrees ->
            assertEquals("$degrees°", RemoteFunction.OK, functionAt(degrees, radius = size * 0.15f))
        }
    }

    @Test
    fun `a touch outside the ring presses nothing at all`() {
        // The dial is square in layout and round on screen. A thumb in a corner has not pressed
        // a key, and inventing one would send a report the user never asked for.
        assertNull(functionAt(45.0, radius = size))
        assertNull(functionAt(0f, 0f, size, size))
        assertNull(functionAt(size, size, size, size))
    }

    @Test
    fun `the dial keeps its geometry when it is squeezed onto a short screen`() {
        val small = 140f
        assertEquals(RemoteFunction.UP, functionAt(small / 2f, small * 0.1f, small, small))
        assertEquals(RemoteFunction.DOWN, functionAt(small / 2f, small * 0.9f, small, small))
        assertEquals(RemoteFunction.OK, functionAt(small / 2f, small / 2f, small, small))
    }
}
