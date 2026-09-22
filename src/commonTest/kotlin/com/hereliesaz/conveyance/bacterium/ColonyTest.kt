package com.hereliesaz.conveyance.bacterium

import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.Ghosts
import com.hereliesaz.conveyance.compose.subjectElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [Colony]'s two pieces of real bookkeeping, asserted as data in and data out.
 *
 * Neither needs a composition: the Ghost-residue step is a plain call on a plain [Ghosts]
 * instance (no fake needed -- [Ghosts] is an ordinary class, and `LocalGhosts` merely supplies
 * one), and the eaten/queued-division arithmetic was pulled out of [PredatorColony] into
 * [consumptionAfter] precisely so the two defects the README narrates -- an eat and a spawn
 * cancelling out, and a burst discarding its remainder -- have somewhere to be pinned down.
 */
class ColonyTest {

    private val dish = ElementId("dish")

    private fun preyWithRestorableAct(name: String): PreyRequest {
        val subject = SubjectId(name)
        val restore = Act.create("prey.restore.$name", subject, into = dish)
        return PreyRequest(
            subject = subject,
            act = Act.destroy("prey.eat.$name", subject = subject, target = dish, inverse = restore),
            hue = "algal",
        )
    }

    // --- (a) eating a prey item records the residue Collection needs, and drops it from the list ---

    @Test
    fun `eating a prey item leaves a recoverable residue in the slot it held`() {
        val ghosts = Ghosts()
        val prey = preyWithRestorableAct("a")

        assertTrue(ghosts.leaveResidueFor(prey))

        // Collection only holds a departed subject's slot open while `holds` is true; this is the
        // whole point of the call.
        assertTrue(ghosts.holds(prey.subject))
        val residue = ghosts[prey.subject]
        assertEquals(subjectElement(prey.subject), residue?.at)
        assertSame(prey.act.inverse, residue?.inverse)
        assertEquals(prey.act.weight, residue?.weight)
    }

    @Test
    fun `the residue recovers to the act that puts the prey back`() {
        val ghosts = Ghosts()
        val prey = preyWithRestorableAct("a")
        ghosts.leaveResidueFor(prey)

        assertSame(prey.act.inverse, ghosts.recover(prey.subject))
        // Recovered residues do not linger: the slot closes again.
        assertFalse(ghosts.holds(prey.subject))
        assertNull(ghosts.recover(prey.subject))
    }

    @Test
    fun `an eaten prey is gone from the tracked list and counted as consumed`() {
        val ghosts = Ghosts()
        val before = listOf(preyWithRestorableAct("a"), preyWithRestorableAct("b"))
        val eaten = before.first()

        ghosts.leaveResidueFor(eaten)
        val after = before.filterNot { it.subject == eaten.subject }

        assertEquals(1, after.size)
        assertFalse(after.any { it.subject == eaten.subject })
        assertTrue(ghosts.holds(eaten.subject))
        assertEquals(
            1,
            consumptionAfter(
                previousIds = before.mapTo(mutableSetOf()) { it.subject },
                currentIds = after.mapTo(mutableSetOf()) { it.subject },
                carried = 0,
                divideAfterEaten = 3,
            ).carried,
        )
    }

    @Test
    fun `an irreversibly eaten prey leaves no residue rather than throwing`() {
        val ghosts = Ghosts()
        val subject = SubjectId("a")
        val prey = PreyRequest(
            subject = subject,
            act = Act.destroyIrreversibly("prey.eat.a", subject = subject, target = dish),
            hue = "algal",
        )

        assertFalse(ghosts.leaveResidueFor(prey))
        assertFalse(ghosts.holds(subject))
    }

    @Test
    fun `an act that is not a destruction leaves no residue`() {
        val ghosts = Ghosts()
        val subject = SubjectId("a")
        val prey = PreyRequest(subject, Act.reveal("prey.look.a", target = dish), hue = "algal")

        assertFalse(ghosts.leaveResidueFor(prey))
        assertFalse(ghosts.holds(subject))
    }

    @Test
    fun `an act destroying someone else's subject fails loudly rather than filing an unreachable residue`() {
        val ghosts = Ghosts()
        val mine = SubjectId("a")
        val theirs = SubjectId("b")
        val restore = Act.create("prey.restore.b", theirs, into = dish)
        val prey = PreyRequest(
            subject = mine,
            act = Act.destroy("prey.eat.b", subject = theirs, target = dish, inverse = restore),
            hue = "algal",
        )

        assertFailsWith<IllegalArgumentException> { ghosts.leaveResidueFor(prey) }
        assertFalse(ghosts.holds(mine))
        assertFalse(ghosts.holds(theirs))
    }

    // --- (b) the earned-division bookkeeping ---

    private fun ids(vararg names: String): Set<SubjectId> = names.mapTo(mutableSetOf(), ::SubjectId)

    @Test
    fun `nothing eaten earns nothing and carries the tally forward untouched`() {
        val still = consumptionAfter(ids("a", "b"), ids("a", "b"), carried = 2, divideAfterEaten = 3)
        assertEquals(Consumption(carried = 2, earned = 0), still)
    }

    @Test
    fun `an eat and a spawn in the same recomposition still counts the eat`() {
        // The defect this replaced: both lists are size 2, so a size comparison saw no change at
        // all and the eaten prey went uncounted.
        val both = consumptionAfter(ids("a", "b"), ids("b", "c"), carried = 0, divideAfterEaten = 3)
        assertEquals(Consumption(carried = 1, earned = 0), both)
    }

    @Test
    fun `a spawn alone is not an eat`() {
        val grown = consumptionAfter(ids("a"), ids("a", "b"), carried = 1, divideAfterEaten = 3)
        assertEquals(Consumption(carried = 1, earned = 0), grown)
    }

    @Test
    fun `the third eaten earns exactly one division and resets the tally`() {
        val third = consumptionAfter(ids("a", "b", "c"), ids("a", "b"), carried = 2, divideAfterEaten = 3)
        assertEquals(Consumption(carried = 0, earned = 1), third)
    }

    @Test
    fun `a burst earns every division it crosses and carries the remainder forward`() {
        // Seven eaten at once, at one division per three: two earned, one left over -- not one
        // division with the other four silently discarded.
        val burst = consumptionAfter(
            previousIds = ids("a", "b", "c", "d", "e", "f", "g", "h"),
            currentIds = ids("h"),
            carried = 0,
            divideAfterEaten = 3,
        )
        assertEquals(Consumption(carried = 1, earned = 2), burst)

        // And the remainder really is carried: two more eaten complete the third division.
        val next = consumptionAfter(ids("h", "i", "j"), ids("h"), burst.carried, divideAfterEaten = 3)
        assertEquals(Consumption(carried = 0, earned = 1), next)
    }

    @Test
    fun `the cadence stays one division per divideAfterEaten however the host batches removals`() {
        // Twelve prey eaten across arbitrarily sized batches earns exactly twelve divided by
        // four, regardless of where the batch boundaries fall.
        val batches = listOf(1, 5, 2, 3, 1)
        var carried = 0
        var earnedTotal = 0
        var alive = (1..12).mapTo(mutableSetOf()) { SubjectId("p$it") }
        batches.forEach { size ->
            val before = alive.toSet()
            alive = alive.drop(size).toMutableSet()
            val step = consumptionAfter(before, alive, carried, divideAfterEaten = 4)
            carried = step.carried
            earnedTotal += step.earned
        }
        assertEquals(0, alive.size)
        assertEquals(3, earnedTotal)
        assertEquals(0, carried)
    }

    @Test
    fun `divisions earned while one is already displaying queue rather than replace it`() {
        // The defect this replaced: the "am I dividing" flag was a Boolean, so a second division
        // earned while the first was still on screen wrote `true` over `true` -- a no-op, and the
        // second division was never shown. A queue counts instead, so two earned means two owed.
        var pending = 0
        var carried = 0

        val first = consumptionAfter(ids("a", "b", "c", "d"), ids("d"), carried, divideAfterEaten = 3)
        carried = first.carried
        pending += first.earned
        assertEquals(1, pending)

        // Still displaying the first when the next burst lands.
        val second = consumptionAfter(ids("d", "e", "f", "g", "h"), ids("h"), carried, divideAfterEaten = 3)
        carried = second.carried
        pending += second.earned
        assertEquals(2, pending)
        // Four eaten in that second burst: one completed the division, the fourth is carried.
        assertEquals(1, carried)

        // Each queued division is drained on its own, one full display window at a time.
        pending -= 1
        assertEquals(1, pending)
        pending -= 1
        assertEquals(0, pending)
    }
}
