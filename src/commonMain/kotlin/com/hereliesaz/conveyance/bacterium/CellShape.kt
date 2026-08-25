package com.hereliesaz.conveyance.bacterium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val CELL_POINTS = 32

/**
 * A single-celled organism's outline: a circle perturbed by localized bumps, each a Gaussian
 * function of angular distance from a bump's own angle -- the shape a real amoeboid membrane
 * actually has, a smooth local bulge rather than a uniform stretch.
 *
 * - **[pseudopodPhase]** (0..1, meant to be driven by a slow, continuously repeating animation)
 *   places a leading pseudopod at `pseudopodPhase * 2π`, with a smaller trailing bulge 180°
 *   behind it -- real amoeboid crawling: cytoplasm extends toward the leading edge, the trailing
 *   edge lags and follows.
 * - **[dentAngle]** / **[dentStrength]** carve an *inward* dent instead of a bulge -- the
 *   phagocytic cup a cell's membrane forms while wrapping around something it's engulfing.
 *   `dentStrength = 0` (the default) leaves the outline undented, for a cell that isn't eating
 *   anything right now.
 */
class CellShape(
    private val pseudopodPhase: Float,
    private val pseudopodStrength: Float = 0.22f,
    private val dentAngle: Float = 0f,
    private val dentStrength: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = minOf(size.width, size.height) / 2f
        val leadAngle = pseudopodPhase * 2f * PI.toFloat()
        val trailAngle = leadAngle + PI.toFloat()

        val points = (0 until CELL_POINTS).map { i ->
            val theta = 2.0 * PI * i / CELL_POINTS
            val lead = gaussianBump(angularDistance(theta, leadAngle.toDouble()), width = 0.9)
            val trail = gaussianBump(angularDistance(theta, trailAngle.toDouble()), width = 1.3) * 0.5
            val dent = gaussianBump(angularDistance(theta, dentAngle.toDouble()), width = 0.6) * dentStrength

            val r = baseRadius * (1f + pseudopodStrength * (lead + trail).toFloat() - dent.toFloat())
                .coerceAtLeast(0.35f)
            Offset(
                x = cx + (r * cos(theta)).toFloat(),
                y = cy + (r * sin(theta)).toFloat(),
            )
        }

        return Outline.Generic(smoothClosedPath(points))
    }
}

private fun angularDistance(a: Double, b: Double): Double {
    var d = (a - b) % (2.0 * PI)
    if (d > PI) d -= 2.0 * PI
    if (d < -PI) d += 2.0 * PI
    return d
}

private fun gaussianBump(angularDist: Double, width: Double): Double =
    exp(-(angularDist * angularDist) / (2.0 * width * width))

internal fun smoothClosedPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    val midpoints = (0 until n).map { i -> lerp(points[i], points[(i + 1) % n], 0.5f) }
    path.moveTo(midpoints[n - 1].x, midpoints[n - 1].y)
    for (i in 0 until n) {
        path.quadraticTo(points[i].x, points[i].y, midpoints[i].x, midpoints[i].y)
    }
    path.close()
    return path
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
