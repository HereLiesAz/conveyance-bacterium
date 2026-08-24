package com.hereliesaz.conveyance.bacterium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.compose.Offer
import kotlin.math.cos
import kotlin.math.sin

/**
 * What a `kind: "composable"` `.azp` package's `elements[]` entry (azphalt `spec/composable.md`)
 * supplies once a host has resolved it against this library's [Templates.registry] and built the
 * live [Act] the element performs. `surface` names a locomotion style (see [Locomotion]); `hue`
 * names a tint (see [BacteriumHue]). Like `conveyance-liquid`, `scale` sizes an optional caption
 * beside the cell rather than text baked into it.
 */
data class ComposableRequest(
    val act: Act,
    val hue: String,
    val surface: String,
    val scale: String,
    val label: String? = null,
)

/** [ComposableRequest.surface] -> how a cell moves, per the three locomotion styles named in this set's concept. */
object Locomotion {
    data class Style(val pseudopodStrength: Float, val cycleMillis: Int, val diameter: Dp)

    /** A single blunt leading pseudopod, slow -- the classic amoeboid crawl. */
    val amoeba = Style(pseudopodStrength = 0.24f, cycleMillis = 5200, diameter = 56.dp)

    /** A rounder body, faster cycling -- ciliary beating reads as a quicker, gentler wobble than a pseudopod's reach. */
    val paramecium = Style(pseudopodStrength = 0.10f, cycleMillis = 2600, diameter = 48.dp)

    /** A pronounced single bulge, fastest cycling -- a flagellum's whip drives a sharper, quicker leading edge. */
    val flagellate = Style(pseudopodStrength = 0.32f, cycleMillis = 1800, diameter = 44.dp)

    fun of(surface: String): Style = when (surface) {
        "amoeba" -> amoeba
        "paramecium" -> paramecium
        "flagellate" -> flagellate
        else -> amoeba
    }
}

/** The bacterium composable-set's template registry -- see `conveyance-h2g2`/`conveyance-expressive`/`conveyance-liquid`'s `Templates.kt` for the pattern this follows. */
object Templates {
    val registry: Map<String, @Composable (ComposableRequest) -> Unit> = mapOf(
        "bacterium.cell.idle" to { request -> IdleCell(request) },
        "bacterium.cell.divide" to { request -> DividingCell(request) },
        "bacterium.cell.bud" to { request -> BuddingCell(request) },
        "bacterium.cell.eat" to { request -> EatingCell(request) },
    )
}

private fun captionStyleFor(scale: String): TextStyle = when (scale) {
    "lead" -> TextStyle(fontSize = 17.sp)
    "eyebrow", "micro" -> TextStyle(fontSize = 11.sp)
    else -> TextStyle(fontSize = 14.sp)
}

/**
 * A continuously crawling cell -- [CellShape]'s pseudopod phase drives from a slow, repeating
 * animation, not a one-shot effect, since a living cell never actually holds still. On top of
 * that deliberate crawl, [rememberBrownianJitter] shakes the whole body -- real Brownian motion,
 * the thermal jitter any real single-celled organism shows constantly, not just while it happens
 * to be moving somewhere.
 */
@Composable
fun IdleCell(request: ComposableRequest) {
    val tint = BacteriumHue.of(request.hue)
    val style = Locomotion.of(request.surface)
    val transition = rememberInfiniteTransition(label = "pseudopod")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(style.cycleMillis, easing = LinearEasing)),
        label = "phase",
    )
    val jitter = rememberBrownianJitter(jitterAmplitudePxFor(style.diameter))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            Box(
                modifier = Modifier
                    .size(style.diameter)
                    .brownianJitter(jitter)
                    .clip(CellShape(pseudopodPhase = phase, pseudopodStrength = style.pseudopodStrength))
                    .background(tint.base),
            )
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}

private const val BUD_ASYMMETRY = 0.6f

/**
 * Binary fission, driven by the act's own state -- [ActState.Yielding]'s live progress
 * (`ActScope.yielding`) narrows the cleavage furrow; the act settling means division is complete.
 * This is [com.hereliesaz.conveyance.Consequence.Create] read literally: one subject becoming
 * two is what mitosis *is*. An even split ([MitosisShape.asymmetry] = 0) -- for a real cell's
 * unequal division, see [BuddingCell].
 */
@Composable
fun DividingCell(request: ComposableRequest) {
    DivisionCell(request, asymmetry = 0f)
}

/**
 * Asymmetric division -- the same [MitosisShape]/act-state machinery as [DividingCell], with
 * [BUD_ASYMMETRY] instead of an even split: the real way budding yeast divides (the mother buds
 * off a visibly smaller daughter) and many stem cell divisions actually work (one daughter keeps
 * the parent's size/identity, the other is smaller and differentiates) -- not every division is
 * two equal halves.
 */
@Composable
fun BuddingCell(request: ComposableRequest) {
    DivisionCell(request, asymmetry = BUD_ASYMMETRY)
}

@Composable
private fun DivisionCell(request: ComposableRequest, asymmetry: Float) {
    val tint = BacteriumHue.of(request.hue)
    val style = Locomotion.of(request.surface)
    val jitter = rememberBrownianJitter(jitterAmplitudePxFor(style.diameter))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            val separation = when (state) {
                is ActState.Settled -> 1f
                is ActState.Yielding -> yielding ?: 0f
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .size(style.diameter * 1.4f)
                    .brownianJitter(jitter)
                    .clip(MitosisShape(separation = separation, asymmetry = asymmetry))
                    .background(tint.base),
            )
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}

private const val CUP_ANGLE = 0f

/**
 * Phagocytosis, self-contained: the membrane forms an inward cup ([CellShape.dentAngle]/
 * `dentStrength`) that closes around engulfed material, which then settles inward as a food
 * vacuole -- a second, smaller circle drawn over the cell body, moving from the rim toward the
 * center as `engulf` rises. Driven the same way as [DividingCell]: [ActState.Yielding]'s progress
 * while the cup is closing, [ActState.Settled] meaning the vacuole is safely inside.
 *
 * This is one cell miming engulfment on its own, not an actual predator consuming a separately
 * addressed prey -- for that, see [PredatorColony] (`Colony.kt`), which uses Conveyance's real
 * `Collection` primitive for genuine two-body predator/prey; it isn't a [Templates.registry]
 * entry because [Collection] needs a caller-owned list of per-item acts, a shape a single
 * `ComposableRequest` can't express. [rememberBrownianJitter] keeps the whole body (cup and
 * vacuole together, as one physical unit) trembling throughout, same as [IdleCell].
 */
@Composable
fun EatingCell(request: ComposableRequest) {
    val tint = BacteriumHue.of(request.hue)
    val style = Locomotion.of(request.surface)
    val jitter = rememberBrownianJitter(jitterAmplitudePxFor(style.diameter))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            val engulf = when (state) {
                is ActState.Settled -> 1f
                is ActState.Yielding -> yielding ?: 0f
                else -> 0f
            }
            // The cup is only open (dent visible) for the first half of engulfment; past that
            // the membrane has already sealed and only the vacuole's inward migration continues.
            val dentStrength = (0.5f - engulf).coerceIn(0f, 0.5f) * 2f * 0.4f
            val vacuoleProgress = ((engulf - 0.3f) / 0.7f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier.size(style.diameter).brownianJitter(jitter),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(style.diameter)
                        .clip(
                            CellShape(
                                pseudopodPhase = 0f,
                                pseudopodStrength = style.pseudopodStrength * 0.4f,
                                dentAngle = CUP_ANGLE,
                                dentStrength = dentStrength,
                            ),
                        )
                        .background(tint.base),
                )
                if (vacuoleProgress > 0f) {
                    val vacuoleDiameter = style.diameter * (0.34f - 0.08f * vacuoleProgress)
                    // Interpolates from just inside the rim (where the cup sealed) toward center.
                    val restDistance = (style.diameter.value / 2f) * (1f - vacuoleProgress) * 0.55f
                    Box(
                        modifier = Modifier
                            .size(vacuoleDiameter)
                            .offset(
                                x = (restDistance * cos(CUP_ANGLE.toDouble())).dp,
                                y = (restDistance * sin(CUP_ANGLE.toDouble())).dp,
                            )
                            .clip(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0f))
                            .background(tint.nucleus),
                    )
                }
            }
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}
