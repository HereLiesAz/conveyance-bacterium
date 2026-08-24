# conveyance-bacterium

A composable-set library for [Conveyance](https://github.com/HereLiesAz/Conveyance): the Bacterium style -- shape, movement, and interaction modeled on single-celled organisms. An element's form is a motile cell body (pseudopod extension, ciliary/flagellar undulation); its interaction verbs come straight from cell biology -- reproduction (an element dividing in two, mitosis-style) and eating (one element engulfing a smaller one, phagocytosis-style).

## What this is

Per [azphalt's `spec/composable.md`](https://github.com/HereLiesAz/azphalt/blob/main/spec/composable.md),
a `kind: "composable"` `.azp` package is a **pure header**: it names this artifact's Gradle
coordinates (`library.group` / `library.artifact`) and selects a `templateId`, `hue`,
`surface`, `scale`, and `act` from it. It carries no code of its own. This repository *is* the
artifact a composable package's `library` block points at -- the `.azp` package itself is
authored and published separately, wherever its author chooses; this repo does not need to hold
one.

Example composable manifest referencing this library:

```jsonc
{
  "azphalt": "0.1",
  "id": "com.hereliesaz.azphalt.example",
  "name": "Example",
  "version": "1.0.0",
  "kind": "composable",
  "license": "MIT",
  "compat": ">=0.1",
  "composable": {
    "library": { "group": "com.hereliesaz.conveyance", "artifact": "conveyance-bacterium", "version": "0.1.0" },
    "elements": [
      { "id": "confirm-record", "templateId": "bacterium.cell.divide", "hue": "algal", "surface": "amoeba", "scale": "lead", "act": "create", "jobs": ["confirms a destructive action"] }
    ]
  },
  "files": {}
}
```

## What's here

- **`CellShape`** (`CellShape.kt`) -- the outline: a circle perturbed by localized Gaussian
  bulges (angular-distance-weighted, not a uniform stretch) rather than a fixed corner-radius
  shape. A leading pseudopod bulge plus a smaller trailing one 180° behind it is real amoeboid
  crawling -- cytoplasm extends toward the front, the rear lags and follows. The same class also
  carves an *inward* Gaussian dent (`dentAngle`/`dentStrength`) -- the phagocytic cup a membrane
  forms while wrapping around something it's engulfing.
- **`rememberBrownianJitter`/`jitterAmplitudePxFor`** (`Brownian.kt`) -- real Brownian motion: a
  single-celled organism never actually holds still, even set apart from its own locomotion --
  thermal collision from the surrounding fluid keeps it trembling randomly the whole time. A
  cheap random walk (re-targets to a new random point every 80-180ms, linear interpolation
  between them) stands in for true noise. Amplitude scales *inversely* with cell diameter, per
  the real Einstein-Stokes relation -- a smaller particle is displaced more by the same molecular
  bombardment, not less, so prey cells in `PredatorColony` visibly tremble more than the predator
  looming over them. Applied to every template in this library.
- **`MitosisShape`** (`MitosisShape.kt`) -- binary fission's outline: two circular lobes whose
  centers separate as `separation` goes 0→1, connected by a shrinking rectangular neck while
  they're still close -- the cleavage furrow pinching the cytoplasm in two. `asymmetry` (0..1,
  default 0) makes the split uneven -- real budding yeast buds off a visibly smaller daughter,
  and many stem cell divisions are deliberately asymmetric -- strictly generalizing the even
  split rather than a separate code path (`asymmetry = 0` reproduces the original shape exactly).
- **`BacteriumHue`** (`BacteriumHue.kt`) -- five translucent cytoplasm tints, each a `base`/
  `nucleus` pair (the second used for the vacuole in the eating template) -- semi-transparent, not
  the opaque fills `conveyance-h2g2`/`conveyance-expressive` use, since a real membrane reads as
  faintly see-through.
- **`Locomotion`** (in `Templates.kt`) -- `surface`'s three styles (`amoeba`, `paramecium`,
  `flagellate`) tune pseudopod strength and animation cycle speed differently -- a blunt slow
  pseudopod, a gentler faster ciliary wobble, and a sharp fast flagellar bulge, per the movement
  styles the concept named.
- **`Templates`** (`Templates.kt`) -- four templates:
  - `bacterium.cell.idle` -- continuously crawling, driven by a slow repeating animation (a real
    cell never actually holds still).
  - `bacterium.cell.divide` -- `MitosisShape`'s `separation` tracks `ActScope.yielding`'s live
    progress while the act is `ActState.Yielding`, reaching 1 at `Settled`; an even split. This
    is `Consequence.Create` read literally: one subject becoming two *is* mitosis.
  - `bacterium.cell.bud` -- the same act-state machinery as `bacterium.cell.divide`, at
    `MitosisShape.asymmetry = 0.6` -- a real unequal division (budding yeast, asymmetric stem
    cell division) rather than every split being two equal halves.
  - `bacterium.cell.eat` -- the cup dent closes over the first half of `ActScope.yielding`'s
    progress, then a second, smaller circle (the vacuole) fades in and migrates from the rim
    toward the center over the second half, settling fully inside at `Settled`. Self-contained --
    one cell miming engulfment, not an actual predator consuming a separate prey; see
    `PredatorColony` below for that.
- **`PredatorColony`/`PreyRequest`** (`Colony.kt`) -- genuine two-body predator/prey, built on
  Conveyance's own `Collection` primitive: a predator alongside a real population of independently
  addressed prey, each carrying its own `Act`. Eating one is the host removing its `SubjectId`
  from the `prey` list it passes in; `Collection` renders the framework's own Ghost residue for
  it. The predator itself reacts to eating: consuming `divideAfterEaten` (default 3) prey --
  detected as `prey` shrinking across recompositions, since this library never removes anything
  itself -- switches the predator's own rendering from `IdleCell` to `bacterium.cell.bud`'s
  `BuddingCell` for 1.2s, the real link between eating and reproduction: consumed biomass has to
  go somewhere, and division is where it goes. **Not** a `Templates.registry` entry, for the same
  reason `conveyance-h2g2`'s `H2g2Page` isn't: every composable manifest element carries exactly
  one `act` (azphalt `spec/composable.md`), and `Collection` inherently needs a caller-owned list
  of items each with its *own* act -- a shape the single-element `ComposableRequest` can't
  express. A host wires this up directly.

Like `conveyance-liquid`, `scale` sizes an optional caption beside the cell rather than text baked
into it -- a label inside a cell body breaks the biological read the same way it would for a
droplet.

## Status

All three named aspects of the concept -- shape/movement, reproduction, eating -- now have both a
single-cell template and, for reproduction and eating, a version that reacts to real state
(asymmetric budding, and a predator that divides after eating enough). What's still not here: a
population-scale colony (many predators and prey sharing one `Collection`, rather than one
predator against a prey list), and `bacterium.cell.bud`'s asymmetry (0.6) is a fixed constant --
nothing in the composable manifest's vocabulary names a variable asymmetry per element.

An adversarial audit found and this repo has since fixed five real defects, beyond the
already-known missing click wiring (every template, plus `PredatorColony`'s `SpawnControl`/
`PreyBlob`, now attach `Modifier.tell(owesTell, weight).clickable { engage() }`, matching
`conveyance-demo/.../Gallery.kt`'s own wiring): `MitosisShape`'s lobe/offset constants painted
outside the shape's own box at high separation and asymmetry (up to 21% past the edge, worst case)
-- both constants are now solved together so neither lobe ever exceeds the box across the full
`0..1` range of either parameter. `PredatorColony`'s eaten-count tracked `prey.size` alone, so an
eat and a simultaneous reproduce (both landing in the same recomposition) cancelled out and the
eat went uncounted; a burst of more than `divideAfterEaten` eaten at once discarded the remainder
instead of carrying it forward; and a second division earned while the first was still displaying
was silently dropped rather than queued (`mutableStateOf(true)` written while already `true` is a
no-op, so the display-timer effect never restarted). Consumption is now tracked by the actual set
of subjects present rather than a size delta, every division a burst earns is queued rather than
lost, and each queued division gets its own full display window. `bacterium.cell.eat`'s cup-dent
and vacuole windows overlapped for 20% of the engulf range, contradicting its own "cup closes,
then vacuole appears" doc comment -- both now share the same 0.5 boundary.

## Using it

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.HereLiesAz:conveyance-bacterium:main-SNAPSHOT")
}
```

Resolved via [JitPack](https://jitpack.io) directly from this repository -- `conveyance-core` and
`conveyance-compose` both apply `maven-publish`, which is all JitPack needs, so there is no
separate publish step to configure. Conveyance itself has no tagged release yet, so this artifact
and its upstream dependency on Conveyance both pin to `main-SNAPSHOT` for now; switch both to a
real tag once one exists.
