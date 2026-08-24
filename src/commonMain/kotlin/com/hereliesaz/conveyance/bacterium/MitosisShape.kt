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
 */
class MitosisShape(private val separation: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val mergedRadius = minOf(size.width, size.height) / 2f
        val lobeRadius = mergedRadius * 0.66f
        val maxCenterOffset = size.width * 0.28f

        val t = separation.coerceIn(0f, 1f)
        val offset = maxCenterOffset * t
        val radius = mergedRadius + (lobeRadius - mergedRadius) * t
        val c1 = Offset(cx - offset, cy)
        val c2 = Offset(cx + offset, cy)

        val path = Path().apply {
            addOval(Rect(center = c1, radius = radius))
            addOval(Rect(center = c2, radius = radius))
        }

        if (t < NECK_VISIBLE_UNTIL) {
            val neckCloseness = 1f - (t / NECK_VISIBLE_UNTIL)
            val neckHalfHeight = radius * neckCloseness * 0.85f
            if (neckHalfHeight > 0.5f) {
                path.addPath(
                    Path().apply {
                        moveTo(c1.x, cy - neckHalfHeight)
                        lineTo(c2.x, cy - neckHalfHeight)
                        lineTo(c2.x, cy + neckHalfHeight)
                        lineTo(c1.x, cy + neckHalfHeight)
                        close()
                    },
                )
            }
        }

        return Outline.Generic(path)
    }
}
