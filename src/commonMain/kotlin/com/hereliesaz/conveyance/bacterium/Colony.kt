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
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.Offer
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
 * passes as `prey`, which is what actually triggers [Collection]'s own Ghost residue -- this
 * library never removes anything itself.
 */
data class PreyRequest(
    val subject: SubjectId,
    val act: Act,
    val hue: String,
)

/**
 * A predator cell and the prey population around it, using Conveyance's own [Collection]
 * primitive for genuine two-body predator/prey -- not the single self-contained composable
 * `bacterium.cell.eat` is. Each [PreyRequest] carries its own [Act]; consuming one is the host
 * removing its subject from [prey], and [Collection] renders the framework's own Ghost residue
 * for it -- Conveyance's motion, this library only supplies [PreyRequest]'s chrome via [preyItem].
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
        val consumed = (previousPreyIds - currentIds).size
        previousPreyIds = currentIds
        if (consumed > 0) {
            eatenSinceDivide += consumed
            val earned = eatenSinceDivide / divideAfterEaten
            if (earned > 0) {
                eatenSinceDivide -= earned * divideAfterEaten
                pendingDivisions += earned
            }
        }
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
    Offer(act = prey.act) {
        Box(
            modifier = Modifier
                .tell(owesTell, weight)
                .clickable { engage() }
                .padding(2.dp)
                .size(PREY_DIAMETER)
                .brownianJitter(jitter)
                .clip(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0.15f))
                .background(tint.base),
        )
    }
}
