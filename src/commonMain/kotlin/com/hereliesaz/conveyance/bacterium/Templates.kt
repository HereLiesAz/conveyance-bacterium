package com.hereliesaz.conveyance.bacterium

/**
 * The `bacterium` composable-set's template registry -- what a `kind: "composable"` `.azp` package's
 * `templateId` resolves against once this artifact is linked at build time (azphalt spec,
 * `spec/composable.md`). A host looks a `templateId` up here; nothing arrives that this
 * artifact didn't already ship.
 *
 * Empty until the first template is designed -- there is nothing yet for a `templateId` to find.
 */
object Templates {
    // TODO: register the first templateId -> @Composable entry.
}
