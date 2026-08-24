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
- **`MitosisShape`** (`MitosisShape.kt`) -- binary fission's outline: two circular lobes whose
  centers separate as `separation` goes 0→1, connected by a shrinking rectangular neck while
  they're still close -- the cleavage furrow pinching the cytoplasm in two.
- **`BacteriumHue`** (`BacteriumHue.kt`) -- five translucent cytoplasm tints, each a `base`/
  `nucleus` pair (the second used for the vacuole in the eating template) -- semi-transparent, not
  the opaque fills `conveyance-h2g2`/`conveyance-expressive` use, since a real membrane reads as
  faintly see-through.
- **`Locomotion`** (in `Templates.kt`) -- `surface`'s three styles (`amoeba`, `paramecium`,
  `flagellate`) tune pseudopod strength and animation cycle speed differently -- a blunt slow
  pseudopod, a gentler faster ciliary wobble, and a sharp fast flagellar bulge, per the movement
  styles the concept named.
- **`Templates`** (`Templates.kt`) -- three templates:
  - `bacterium.cell.idle` -- continuously crawling, driven by a slow repeating animation (a real
    cell never actually holds still).
  - `bacterium.cell.divide` -- `MitosisShape`'s `separation` tracks `ActScope.yielding`'s live
    progress while the act is `ActState.Yielding`, reaching 1 at `Settled`. This is
    `Consequence.Create` read literally: one subject becoming two *is* mitosis.
  - `bacterium.cell.eat` -- the cup dent closes over the first half of `ActScope.yielding`'s
    progress, then a second, smaller circle (the vacuole) fades in and migrates from the rim
    toward the center over the second half, settling fully inside at `Settled`.

Like `conveyance-liquid`, `scale` sizes an optional caption beside the cell rather than text baked
into it -- a label inside a cell body breaks the biological read the same way it would for a
droplet.

## Status

A first real slice, not a finished set. All three named aspects of the concept -- shape/movement,
reproduction, eating -- have at least one working template, but each is a single style: no
predator-vs-prey as two independently addressed elements (the eating template is one
self-contained composable, not an actual two-body engulfment), and mitosis always produces two
equal lobes rather than the population growth a colony of cells would need.

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
