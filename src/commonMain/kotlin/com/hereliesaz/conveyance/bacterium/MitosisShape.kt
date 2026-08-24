package com.hereliesaz.conveyance.bacterium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val NECK_VISIBLE_UNTIL = 0.6f

/**
 * Binary fission's outline: two circular lobes whose centers separate as [separation] goes 0→1,
 * connected by a shrinking rectangular neck while they're still close -- the cleavage furrow
 * pinching the cytoplasm in two, not a single shape stretching apart. At `separation = 0` the two
 * lobes fully overlap (one merged cell); at `separation = 1` they're two independent circles with
 * no neck left, [NECK_VISIBLE_UNTIL] of the way there.
 *
 * [asymmetry] (0..1, default 0 -- an even split) makes the two lobes unequal: real cells don't
 * always divide evenly -- budding yeast's mother buds off a visibly smaller daughter, and many
 * stem cell divisions are deliberately asymmetric (one daughter keeps the parent's size/identity,
 * the other is smaller and differentiates). At `asymmetry = 0` both lobes read identically to the
 * un-asymmetric shape this class always drew; higher values grow the first lobe slightly past
 * even and shrink the second toward a bud.
 */
class MitosisShape(private val separation: Float, private val asymmetry: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val mergedRadius = minOf(size.width, size.height) / 2f
        val lobeRadius = mergedRadius * 0.66f
        val maxCenterOffset = size.width * 0.28f

        val t = separation.coerceIn(0f, 1f)
        val asym = asymmetry.coerceIn(0f, 1f)
        val offset = maxCenterOffset * t
        // The larger lobe (the "mother") grows slightly past the even-split radius as asymmetry
        // rises; the smaller lobe (the "bud") shrinks further -- both anchored to the same even
        // split at asymmetry = 0, so this is a strict generalization, not a separate code path.
        val motherRadius = mergedRadius + (lobeRadius * (1f + asym * 0.3f) - mergedRadius) * t
        val budRadius = mergedRadius + (lobeRadius * (1f - asym * 0.7f) - mergedRadius) * t
        val motherCenter = Offset(cx - offset, cy)
        val budCenter = Offset(cx + offset, cy)

        val path = Path().apply {
            addOval(Rect(center = motherCenter, radius = motherRadius))
            addOval(Rect(center = budCenter, radius = budRadius))
        }

        if (t < NECK_VISIBLE_UNTIL) {
            val neckCloseness = 1f - (t / NECK_VISIBLE_UNTIL)
            val neckHalfHeight = minOf(motherRadius, budRadius) * neckCloseness * 0.85f
            if (neckHalfHeight > 0.5f) {
                path.addPath(
                    Path().apply {
                        moveTo(motherCenter.x, cy - neckHalfHeight)
                        lineTo(budCenter.x, cy - neckHalfHeight)
                        lineTo(budCenter.x, cy + neckHalfHeight)
                        lineTo(motherCenter.x, cy + neckHalfHeight)
                        close()
                    },
                )
            }
        }

        return Outline.Generic(path)
    }
}
