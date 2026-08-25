package com.hereliesaz.conveyance.bacterium

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BacteriumHueTest {

    @Test
    fun `of resolves every real tint name to its own tint`() {
        assertEquals(BacteriumHue.algal, BacteriumHue.of("algal"))
        assertEquals(BacteriumHue.amber, BacteriumHue.of("amber"))
        assertEquals(BacteriumHue.coral, BacteriumHue.of("coral"))
        assertEquals(BacteriumHue.violet, BacteriumHue.of("violet"))
        assertEquals(BacteriumHue.slate, BacteriumHue.of("slate"))
    }

    private val named = listOf(BacteriumHue.algal, BacteriumHue.amber, BacteriumHue.coral, BacteriumHue.violet, BacteriumHue.slate)

    @Test
    fun `of hashes an unrecognized hue onto one of the five named tints, deterministically`() {
        val first = BacteriumHue.of("some-unrecognized-hue")
        val second = BacteriumHue.of("some-unrecognized-hue")
        assertEquals(first, second)
        assertTrue(first in named)
    }

    /**
     * Regression guard for the Int.MIN_VALUE `hashCode()` overflow this session's h2g2 audit
     * found and fixed elsewhere in this framework: a naive "negate if negative" hash-to-index
     * scheme can hand `%` a negative dividend for that one hash value, throwing
     * IndexOutOfBoundsException. [BacteriumHue] was written using `.mod()` from the start.
     */
    @Test
    fun `of never throws for a wide spread of unrecognized hue strings`() {
        (0..500).forEach { i ->
            val tint = BacteriumHue.of("hue-$i")
            assertTrue(tint in named, "hue-$i resolved to a tint outside the five named ones")
        }
    }
}
