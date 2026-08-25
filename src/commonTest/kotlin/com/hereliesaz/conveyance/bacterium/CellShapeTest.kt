package com.hereliesaz.conveyance.bacterium

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class CellShapeTest {

    private val density = Density(1f)
    private val size = Size(200f, 200f)
    private val cx = size.width / 2f

    private fun boundsOf(shape: CellShape) =
        (shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic).path.getBounds()

    @Test
    fun `zero pseudopod strength and dent produce a symmetric, near-circular blob`() {
        val bounds = boundsOf(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0f))
        assertTrue(abs(bounds.width - bounds.height) < 4f, "width=${bounds.width} height=${bounds.height}")
    }

    /**
     * At `pseudopodPhase = 0` the leading pseudopod sits at angle 0 (screen +x, rightward) at full
     * strength, with a smaller trailing bulge 180 degrees behind it (leftward) at half strength --
     * the shape should bulge further right than left.
     */
    @Test
    fun `pseudopodPhase 0 bulges rightward more than leftward`() {
        val bounds = boundsOf(CellShape(pseudopodPhase = 0f))
        val rightExtent = bounds.right - cx
        val leftExtent = cx - bounds.left
        assertTrue(rightExtent > leftExtent, "right=$rightExtent should exceed left=$leftExtent")
    }

    /** Half a phase cycle later, the leading pseudopod has rotated to the opposite side. */
    @Test
    fun `pseudopodPhase 0_5 bulges leftward more than rightward`() {
        val bounds = boundsOf(CellShape(pseudopodPhase = 0.5f))
        val rightExtent = bounds.right - cx
        val leftExtent = cx - bounds.left
        assertTrue(leftExtent > rightExtent, "left=$leftExtent should exceed right=$rightExtent")
    }

    /**
     * [CellShape.dentAngle]/[CellShape.dentStrength] carve an inward dent -- with no pseudopod
     * bulge to compete with it, a dent at angle 0 should visibly shrink the rightward extent while
     * leaving the opposite side roughly at its undented radius.
     */
    @Test
    fun `a dent shrinks the outline toward dentAngle without affecting the opposite side`() {
        val baseRadius = size.width / 2f
        val bounds = boundsOf(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0f, dentAngle = 0f, dentStrength = 0.35f))
        val rightExtent = bounds.right - cx
        val leftExtent = cx - bounds.left
        assertTrue(rightExtent < baseRadius * 0.85f, "dented right extent=$rightExtent should be visibly less than baseRadius=$baseRadius")
        assertTrue(leftExtent > baseRadius * 0.9f, "undented left extent=$leftExtent should stay close to baseRadius=$baseRadius")
    }

    @Test
    fun `dentStrength 0 leaves the outline undented regardless of dentAngle`() {
        val undented = boundsOf(CellShape(pseudopodPhase = 0.25f, pseudopodStrength = 0f, dentAngle = 1.2f, dentStrength = 0f))
        val noDentAtAll = boundsOf(CellShape(pseudopodPhase = 0.25f, pseudopodStrength = 0f))
        assertTrue(abs(undented.width - noDentAtAll.width) < 0.01f)
        assertTrue(abs(undented.height - noDentAtAll.height) < 0.01f)
    }
}
