package com.hereliesaz.conveyance.bacterium

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.Offer
import kotlinx.coroutines.delay

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
 * The predator itself reacts to eating: consuming [divideAfterEaten] prey (detected as [prey]
 * shrinking across recompositions -- this library never removes anything itself, so it can only
 * ever *observe* a shrink the host already made) switches the predator's own rendering from
 * [IdleCell] to [BuddingCell] for [DIVIDE_DISPLAY_MILLIS], the real link between eating and
 * reproduction -- consumed biomass has to go somewhere, and division is where it goes. This
 * changes only which of this library's own composables draws the *one* predator element; the
 * predator's own [Act]/address never changes; the choice is driven by data the host already
 * supplies via [prey], not by moving anything.
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
    var previousPreyCount by remember { mutableIntStateOf(prey.size) }
    var eatenSinceDivide by remember { mutableIntStateOf(0) }
    var dividing by remember { mutableStateOf(false) }

    LaunchedEffect(prey.size) {
        val consumed = (previousPreyCount - prey.size).coerceAtLeast(0)
        previousPreyCount = prey.size
        if (consumed > 0) {
            eatenSinceDivide += consumed
            if (eatenSinceDivide >= divideAfterEaten) {
                eatenSinceDivide = 0
                dividing = true
            }
        }
    }
    // A separate effect keyed on `dividing`, not `prey.size` -- a LaunchedEffect restarts (and
    // cancels any delay in flight) whenever its key changes, so tying the display timer to
    // prey.size would leave `dividing` stuck true forever if another prey were eaten mid-display.
    LaunchedEffect(dividing) {
        if (dividing) {
            delay(DIVIDE_DISPLAY_MILLIS)
            dividing = false
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
                .padding(2.dp)
                .size(PREY_DIAMETER)
                .brownianJitter(jitter)
                .clip(CellShape(pseudopodPhase = 0f, pseudopodStrength = 0.15f))
                .background(tint.base),
        )
    }
}
