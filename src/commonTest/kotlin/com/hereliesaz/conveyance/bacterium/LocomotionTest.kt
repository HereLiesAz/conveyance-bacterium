package com.hereliesaz.conveyance.bacterium

import kotlin.test.Test
import kotlin.test.assertEquals

class LocomotionTest {

    @Test
    fun `of resolves every real surface name to its own style`() {
        assertEquals(Locomotion.amoeba, Locomotion.of("amoeba"))
        assertEquals(Locomotion.paramecium, Locomotion.of("paramecium"))
        assertEquals(Locomotion.flagellate, Locomotion.of("flagellate"))
    }

    @Test
    fun `of falls back to amoeba for an unrecognized surface`() {
        assertEquals(Locomotion.amoeba, Locomotion.of(""))
        assertEquals(Locomotion.amoeba, Locomotion.of("nonsense"))
    }

    /** The three styles are meant to read as genuinely different cadences, not just cosmetic variants. */
    @Test
    fun `the three styles have distinct pseudopodStrength, cycleMillis, and diameter`() {
        val styles = listOf(Locomotion.amoeba, Locomotion.paramecium, Locomotion.flagellate)
        assertEquals(3, styles.map { it.pseudopodStrength }.toSet().size)
        assertEquals(3, styles.map { it.cycleMillis }.toSet().size)
        assertEquals(3, styles.map { it.diameter }.toSet().size)
    }

    /** Faster cycling should pair with a more pronounced bulge -- flagellate is both fastest and strongest. */
    @Test
    fun `flagellate is both the fastest-cycling and most pronounced style`() {
        assertEquals(Locomotion.flagellate.cycleMillis, listOf(Locomotion.amoeba, Locomotion.paramecium, Locomotion.flagellate).minOf { it.cycleMillis })
        assertEquals(Locomotion.flagellate.pseudopodStrength, listOf(Locomotion.amoeba, Locomotion.paramecium, Locomotion.flagellate).maxOf { it.pseudopodStrength })
    }
}
