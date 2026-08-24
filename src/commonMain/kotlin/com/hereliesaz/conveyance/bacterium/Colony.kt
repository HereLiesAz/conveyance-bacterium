package com.hereliesaz.conveyance.bacterium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.Offer

private val PREY_DIAMETER = 20.dp

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
 */
@Composable
fun PredatorColony(
    predator: ComposableRequest,
    prey: List<PreyRequest>,
    /** Spawns a new prey cell -- [Collection]'s own required "where new things come from" control. */
    reproduce: Act,
    modifier: Modifier = Modifier,
) {
    Column {
        IdleCell(predator)
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
