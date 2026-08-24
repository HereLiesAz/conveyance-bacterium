package com.hereliesaz.conveyance.bacterium

import androidx.compose.ui.graphics.Color

/**
 * A cell's tint: a translucent cytoplasm [base] over a slightly denser [nucleus] tone (used for
 * the inner vacuole in [Templates.registry]'s eating template) -- organic, semi-transparent
 * colors, not the flat opaque fills [com.hereliesaz.conveyance.expressive] or
 * [com.hereliesaz.conveyance.h2g2] use, since a real cell membrane reads as faintly see-through.
 */
data class BacteriumTint(val base: Color, val nucleus: Color)

object BacteriumHue {
    val algal = BacteriumTint(Color(0x9958A65C), Color(0xB33A6B2E))
    val amber = BacteriumTint(Color(0x99C29A3E), Color(0xB3936A1E))
    val coral = BacteriumTint(Color(0x99C2685C), Color(0xB38A362C))
    val violet = BacteriumTint(Color(0x998C6CA8), Color(0xB35A3A78))
    val slate = BacteriumTint(Color(0x996E7C82), Color(0xB33E4A50))

    private val named = listOf(algal, amber, coral, violet, slate)

    /** Looks up a tint by the composable manifest's `hue` string; an unrecognized id is hashed onto one of [named]. */
    fun of(hue: String): BacteriumTint = when (hue) {
        "algal" -> algal
        "amber" -> amber
        "coral" -> coral
        "violet" -> violet
        "slate" -> slate
        // Kotlin's Int.MIN_VALUE has no positive two's-complement negation (-Int.MIN_VALUE
        // overflows back to itself), so a naive "negate if negative" can still hand `%` a
        // negative dividend and throw IndexOutOfBoundsException. `mod` (not `%`) always returns
        // a non-negative result for a positive divisor, sidestepping that entirely.
        else -> named[hue.hashCode().mod(named.size)]
    }
}
