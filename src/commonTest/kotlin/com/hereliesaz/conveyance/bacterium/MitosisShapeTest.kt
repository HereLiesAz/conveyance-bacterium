package com.hereliesaz.conveyance.bacterium

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MitosisShapeTest {

    private val density = Density(1f)
    private val size = Size(200f, 200f)
    private val tolerance = 0.5f

    private fun boundsOf(separation: Float, asymmetry: Float = 0f) =
        (MitosisShape(separation, asymmetry).createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic).path.getBounds()

    /** At `separation = 0` the two lobes fully overlap into one merged circle inscribed in the box. */
    @Test
    fun `separation 0 is a single circle exactly inscribed in the box`() {
        val bounds = boundsOf(separation = 0f)
        assertEquals(0f, bounds.left, tolerance)
        assertEquals(200f, bounds.right, tolerance)
        assertEquals(0f, bounds.top, tolerance)
        assertEquals(200f, bounds.bottom, tolerance)
    }

    /**
     * At `separation = 1, asymmetry = 0` the two even lobes are fully apart -- exact math, since
     * [MitosisShape] builds these from plain `addOval` calls rather than a sampled/smoothed curve.
     * `lobeRadius = mergedRadius * 0.45`, `maxCenterOffset = size.width * 0.19`.
     */
    @Test
    fun `separation 1 with even asymmetry produces two identical, fully separated lobes`() {
        val bounds = boundsOf(separation = 1f, asymmetry = 0f)
        assertEquals(17f, bounds.left, tolerance)
        assertEquals(183f, bounds.right, tolerance)
        assertEquals(55f, bounds.top, tolerance)
        assertEquals(145f, bounds.bottom, tolerance)
    }

    /**
     * [MitosisShape.LOBE_RADIUS_RATIO]/[MitosisShape.MAX_CENTER_OFFSET_RATIO]'s own KDoc claims
     * neither lobe ever paints outside the shape's box, worst case (`separation = 1, asymmetry = 1`)
     * included -- verified here rather than trusted.
     */
    @Test
    fun `neither lobe ever paints outside the box, even at maximum separation and asymmetry`() {
        val bounds = boundsOf(separation = 1f, asymmetry = 1f)
        assertTrue(bounds.left >= 0f, "left=${bounds.left} must not escape the box")
        assertTrue(bounds.right <= 200f, "right=${bounds.right} must not escape the box")
        assertTrue(bounds.top >= 0f, "top=${bounds.top} must not escape the box")
        assertTrue(bounds.bottom <= 200f, "bottom=${bounds.bottom} must not escape the box")
    }

    /** Higher asymmetry grows the mother lobe and shrinks the bud -- the mother-side (left) edge should move further left. */
    @Test
    fun `higher asymmetry pushes the mother lobe further past the even split`() {
        val even = boundsOf(separation = 1f, asymmetry = 0f)
        val asymmetric = boundsOf(separation = 1f, asymmetry = 1f)
        assertTrue(asymmetric.left < even.left, "asymmetric mother lobe should extend further left than the even split")
    }

    @Test
    fun `separation and asymmetry are coerced into the 0 to 1 range for out-of-range input`() {
        assertEquals(boundsOf(0f), boundsOf(-2f))
        assertEquals(boundsOf(1f), boundsOf(2f))
        assertEquals(boundsOf(1f, 1f), boundsOf(1f, 5f))
    }
}
