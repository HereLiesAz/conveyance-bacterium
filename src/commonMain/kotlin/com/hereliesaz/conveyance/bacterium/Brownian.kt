package com.hereliesaz.conveyance.bacterium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.random.Random

/**
 * Brownian motion: a real single-celled organism never actually holds still, even set apart from
 * its own locomotion -- thermal collision from the surrounding fluid keeps it jittering randomly
 * the whole time. This is a cheap random walk standing in for true noise: re-targets to a new
 * random point within [amplitudePx] every 80-180ms, linearly interpolating between them, which
 * reads as irregular trembling rather than a smooth, obviously-periodic wobble.
 */
@Composable
internal fun rememberBrownianJitter(amplitudePx: Float): Animatable<Offset, AnimationVector2D> {
    val jitter = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(amplitudePx) {
        while (true) {
            val target = Offset(
                x = (Random.nextFloat() * 2f - 1f) * amplitudePx,
                y = (Random.nextFloat() * 2f - 1f) * amplitudePx,
            )
            jitter.animateTo(target, tween(durationMillis = Random.nextInt(80, 180), easing = LinearEasing))
        }
    }
    return jitter
}

/** Reads [jitter]'s live value each layout pass -- a lambda offset, so this animates without recomposing the composable itself. */
internal fun Modifier.brownianJitter(jitter: Animatable<Offset, AnimationVector2D>): Modifier =
    this.offset { IntOffset(jitter.value.x.toInt(), jitter.value.y.toInt()) }

private const val REFERENCE_DIAMETER = 48f
private const val REFERENCE_JITTER_DP = 2.5f

/**
 * How far, in pixels, a cell of [diameter] should jitter -- inversely proportional to size, per
 * the real Einstein-Stokes relation: a smaller particle suspended in fluid is displaced *more* by
 * the same molecular bombardment, not less. [REFERENCE_JITTER_DP] is calibrated against
 * [REFERENCE_DIAMETER] (exactly `Locomotion.paramecium`'s own diameter, the middle of this
 * library's three locomotion sizes -- `amoeba` is 56dp, `flagellate` 44dp -- so both the largest
 * and smallest styles jitter more/less than this reference rather than one end always winning).
 */
@Composable
internal fun jitterAmplitudePxFor(diameter: Dp): Float {
    val amplitudeDp = REFERENCE_JITTER_DP * (REFERENCE_DIAMETER / diameter.value)
    return with(LocalDensity.current) { amplitudeDp.dp.toPx() }
}
