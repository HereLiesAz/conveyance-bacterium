package com.hereliesaz.conveyance.bacterium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.Consequence
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.Ghosts
import com.hereliesaz.conveyance.compose.LocalGhosts
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.subjectElement
import com.hereliesaz.conveyance.compose.tell
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val PREY_DIAMETER = 20.dp
private const val DEFAULT_DIVIDE_AFTER_EATEN = 3
private const val DIVIDE_DISPLAY_MILLIS = 1200L

/**
 * One prey cell in a [PredatorColony] -- unlike [ComposableRequest], this carries its **own**
 * [act], since [Collection] needs every item independently addressable. [act]'s consequence is
 * the eating: engaging it is what the host wires up to remove this [subject] from the list it
 * passes as `prey`. Removal alone is not enough to get a Ghost residue out of [Collection],
 * though -- see [leaveResidueFor], which [PredatorColony] calls on this library's own behalf.
 */
data class PreyRequest(
    val subject: SubjectId,
    val act: Act,
    val hue: String,
)

/**
 * Record what eating [prey] leaves behind, so [Collection] has a residue to draw in the slot the
 * prey held.
 *
 * [Collection]'s slot resolution only keeps a departed subject's slot open while `ghosts.holds(it)` is
 * true; a subject that simply disappears from the list with nothing recorded leaves no `Slot.Gone`
 * and nothing is drawn at all. [Ghosts.leave] is the step that records it, and it has to happen
 * *before* the host's own removal lands -- exactly the ordering `conveyance-demo`'s own Gallery
 * uses, where `ghosts.leave(discard, at = tray)` is the first statement of the destroy act's body.
 * Since a host owns [PreyRequest.act] and this library cannot reach inside its body, the call is
 * made here instead, at the moment the prey is engaged.
 *
 * Returns whether a residue was actually recorded. Nothing is recorded, and nothing throws, for an
 * act that cannot leave one: [Ghosts.leave] requires a [Consequence.Destroy] carrying an inverse,
 * so an eating declared through `Act.destroyIrreversibly` (prey genuinely digested beyond recall)
 * is honestly residue-less rather than a crash. The subject the act names must be the subject this
 * item is keyed by -- a mismatch would file the residue under a key [Collection] never looks up,
 * which is a silent nothing rather than a visible bug, so it fails loudly here instead.
 */
internal fun Ghosts.leaveResidueFor(prey: PreyRequest): Boolean {
    val consequence = prey.act.consequence
    if (consequence !is Consequence.Destroy) return false
    require(consequence.subject == prey.subject) {
        "Prey ${prey.subject} carries an act that destroys ${consequence.subject}: a residue " +
            "left under that key is one no Collection keyed by ${prey.subject} will ever resolve."
    }
    if (prey.act.inverse == null) return false
    leave(prey.act, at = subjectElement(prey.subject))
    return true
}

/**
 * What one recomposition's worth of prey removals does to the predator's division bookkeeping.
 *
 * [carried] is the leftover that has not yet added up to a division; [earned] is how many
 * divisions this batch completed. Pure, and deliberately separate from the composable that holds
 * the state, so the two failure modes this once had -- a simultaneous eat and spawn cancelling out
 * in a size comparison, and a burst larger than [divideAfterEaten] discarding its remainder -- are
 * assertable directly as data in, data out.
 */
internal data class Consumption(val carried: Int, val earned: Int)

/** @see Consumption */
internal fun consumptionAfter(
    previousIds: Set<SubjectId>,
    currentIds: Set<SubjectId>,
    carried: Int,
    divideAfterEaten: Int,
): Consumption {
    val consumed = (previousIds - currentIds).size
    if (consumed == 0) return Consumption(carried = carried, earned = 0)
    val total = carried + consumed
    val earned = total / divideAfterEaten
    return Consumption(carried = total - earned * divideAfterEaten, earned = earned)
}

/**
 * A predator cell and the prey population around it, using Conveyance's own [Collection]
 * primitive for genuine two-body predator/prey -- not the single self-contained composable
 * `bacterium.cell.eat` is. Each [PreyRequest] carries its own [Act]; consuming one is the host
 * removing its subject from [prey], and [Collection] renders the framework's own Ghost residue in
 * the slot it held -- Conveyance's motion, this library only supplies the prey's chrome and the
 * [Ghosts.leave] call that gives [Collection] a residue to draw (see [leaveResidueFor]).
 *
 * This is **not** a [Templates.registry] entry, for the same reason
 * [com.hereliesaz.conveyance.h2g2.H2g2Page] (`conveyance-h2g2`) isn't: every composable manifest
 * element carries exactly one `act` (azphalt `spec/composable.md`), and [Collection] inherently
 * needs a caller-owned list of items each with its *own* act -- a shape this library's
 * single-element [ComposableRequest] can't express. A host wires this up directly.
 *
 * The predator itself reacts to eating: consuming [divideAfterEaten] prey (detected as actual
 * subjects disappearing from [prey] across recompositions -- this library never removes anything
 * itself, so it can only ever *observe* a removal the host already made) switches the predator's
 * own rendering from [IdleCell] to [BuddingCell] for [DIVIDE_DISPLAY_MILLIS] per division earned,
 * the real link between eating and reproduction -- consumed biomass has to go somewhere, and
 * division is where it goes. This changes only which of this library's own composables draws the
 * *one* predator element; the predator's own [Act]/address never changes; the choice is driven by
 * data the host already supplies via [prey], not by moving anything.
 *
 * Consumption is tracked by the actual *set* of subjects present, not [prey]'s size: a size
 * comparison alone can't tell "one eaten" from "one eaten, one spawned" when both land in the
 * same recomposition -- the sizes cancel and the eat goes uncounted. A burst of several eaten at
 * once earns every division it crosses (`consumed / divideAfterEaten`), carrying the remainder
 * forward rather than discarding it, so the cadence stays exactly "every [divideAfterEaten]
 * eaten" regardless of how the host batches its removals. Divisions earned while one is already
 * displaying queue rather than get dropped, each getting its own full [DIVIDE_DISPLAY_MILLIS].
 */
@Composable
fun PredatorColony(
    predator: ComposableRequest,
    prey: List<PreyRequest>,
    /** Spawns a new prey cell -- [Collection]'s own required "where new things come from" control. */
    reproduce: Act,
    modifier: Modifier = Modifier,
    divideAfterEaten: Int = DEFAULT_DIVIDE_AFTER_EATEN,
) {
    var previousPreyIds by remember { mutableStateOf(prey.map { it.subject }.toSet()) }
    var eatenSinceDivide by remember { mutableIntStateOf(0) }
    var pendingDivisions by remember { mutableIntStateOf(0) }
    var dividing by remember { mutableStateOf(false) }

    LaunchedEffect(prey) {
        val currentIds = prey.map { it.subject }.toSet()
        val consumption = consumptionAfter(previousPreyIds, currentIds, eatenSinceDivide, divideAfterEaten)
        previousPreyIds = currentIds
        eatenSinceDivide = consumption.carried
        pendingDivisions += consumption.earned
    }
    // One long-lived consumer of `pendingDivisions`, keyed on Unit so it never restarts (and so
    // never drops a delay in flight the way keying on `dividing` or `pendingDivisions` itself
    // would): it waits for at least one division to be owed, displays it for the full
    // DIVIDE_DISPLAY_MILLIS, then decrements and either shows the next queued one immediately or
    // goes back to waiting.
    LaunchedEffect(Unit) {
        while (true) {
            snapshotFlow { pendingDivisions }.first { it > 0 }
            dividing = true
            delay(DIVIDE_DISPLAY_MILLIS)
            pendingDivisions -= 1
            if (pendingDivisions == 0) dividing = false
        }
    }

    Column {
        if (dividing) BuddingCell(predator) else IdleCell(predator)
        Collection(
            items = prey,
            creator = reproduce,
            key = { it.subject },
            modifier = modifier,
            creatorContent = { SpawnControl() },
            item = { preyItem -> PreyBlob(preyItem) },
        )
    }
}

@Composable
private fun ActScope.SpawnControl() {
    Box(
        modifier = Modifier
            .tell(owesTell, weight)
            .clickable { engage() }
            .size(18.dp)
            .clip(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0f))
            .background(BacteriumHue.algal.base),
    )
}

/**
 * A single prey cell's chrome -- small, undifferentiated, [Offer]-backed by its own
 * [PreyRequest.act]. Jitters via [rememberBrownianJitter] like every other cell in this library --
 * prey are real single-celled organisms too, and smaller ones jitter *more* per the real
 * Einstein-Stokes relation ([jitterAmplitudePxFor]), so a prey blob trembles more than the
 * predator looming over it.
 */
@Composable
private fun PreyBlob(prey: PreyRequest) {
    val tint = BacteriumHue.of(prey.hue)
    val jitter = rememberBrownianJitter(jitterAmplitudePxFor(PREY_DIAMETER))
    val ghosts = LocalGhosts.current
    Offer(act = prey.act) {
        // A refused eating is a prey still sitting there: the residue recorded on engagement was
        // for a destruction that never happened, so it is withdrawn again rather than left as an
        // undo affordance for nothing. Safe to run here -- a refusal means the host never removed
        // the subject, so this blob is still composed to see it.
        LaunchedEffect(state) {
            if (state is ActState.Refused) ghosts.release(prey.subject)
        }
        Box(
            modifier = Modifier
                .tell(owesTell, weight)
                .clickable {
                    // Recorded before the act runs, because the act's own body is where the host
                    // removes this subject from the list -- and once it is out of the list with no
                    // residue on file, Collection closes the slot and nothing is drawn at all.
                    ghosts.leaveResidueFor(prey)
                    engage()
                }
                .padding(2.dp)
                .size(PREY_DIAMETER)
                .brownianJitter(jitter)
                .clip(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0.15f))
                .background(tint.base),
        )
    }
}
