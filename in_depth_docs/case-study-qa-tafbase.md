# Case study: applying DSL-First to a legacy test-automation framework

*Field report — the motivation for the Catalog DSL proposal and the
Behavior/Verification extensions.*

## The project

**qa-tafBase** — a 91.5k-line Java test-automation framework (Selenium +
Cucumber + Appium) driving a trading platform's portals: 320 classes of
framework machinery, page-object catalogs, driver/target matrices, a
golden-image screenshot engine, and six suite modules. The methodology
was applied to it in the "newcomer joins a legacy project" mode: the
model was extracted from the code, artifacts were derived, and the
derived output was diffed against the originals.

## What was modeled

- `kernel.dsl` — element/locator types, `ScenarioRun` / `SuiteRun` /
  `ScreenshotArtifact` lifecycles, a 5-service runtime contract
- `behaviors.dsl` — scenario setup/teardown, screenshot compare,
  baseline promote, parallel stress run
- `verify.dsl` — 13 claims: locator resolution, filename grammar,
  environment/target selection, config layering
- `catalog/*.dsl` — **had to be invented**: 30 page objects /
  1,236 elements / 1,902 locator rows, 36 config keys, 24 driver
  targets, 14 suite manifests

## What derived cleanly

All 30 generated POM classes verified **tuple-identical** to the
hand-written originals (every element, every `(version, lang, kind,
value)` row, multi-language `addLocator` merges, machine-keyed screen
coordinates). Lifecycle classes, config POJOs, target tables, runner
manifests, behavior executors, and check suites all emitted correctly.

**Generability, measured** (see the case's generability report):

| layer | lines | gen% |
|---|---|---|
| catalog-data (declarations) | ~11k | ~92% |
| external-generated (JAXB stubs, already codegen) | ~31k | ~90% |
| **framework mechanism** | **~25.7k** | **~32%** |
| test-content (authored flows) | ~22.7k | ~19% |

## Where the stock DSLs fell short

1. **No declaration-table construct** — the largest gap. A test
   framework's heaviest generatable mass is *tables*: element/locator
   catalogs, config keys, device matrices, suite manifests. Kernel
   `fields { name: TYPE }` can't carry dotted keys or quoted xpath
   values, and a sparse `(version × language)` matrix isn't a fields
   shape. → **Catalog DSL** (this repo, `dsl_first_methodology/CATALOG_DSL.md`).

2. **Selection/routing is scattered glue** — "tag `@uat_env` → env uat
   → locator version UAT", "resolution token → capabilities" lived as
   hand-written switches. → **`bind`** rows in the Catalog DSL; claims
   checked by Verification.

3. **Name grammars are only prose** — `TCID_RunId_Resolution_StepId_Type.PNG`
   and the `(version,lang)` fallback chain are precise rules the
   languages can only describe in `invariants {}`. → **`format`**
   blocks: generate renderer *and* parser, round-trip-checked.

4. **Behavior DSL lacks conditionality** — real teardown is
   `if VIDEO_RECORDING_ENABLED`, `if keepBrowserOpen`, `if isCopyToX`,
   driver-kind branches. Steps had to be modeled unconditional.
   → **`when` statement** (extension in this PR).

5. **Verification `expect` too thin** — `path == value` only. Claims
   needing `!=`, set membership, or "one of" were unwritable.
   → **bounded operator set** (proposal in this PR), keeping the
   legibility argument by restricting, not generalizing.

## What stayed unmodeled (correctly)

The wait/click/type bodies, the image-diff engine, driver plumbing,
mail/SOAP fixtures — the runtime the model plugs into. Not a language
gap; the right boundary.

## Lesson

The methodology's family-of-DSLs pattern held: when a concern didn't
fit the grammars, it got its own small language on the shared
toolchain rather than syntactic contortion. The finding worth
promoting: **declaration catalogs are a recurring fourth concern** —
present in every system with "lots of configuration-shaped code" —
and deserve a standard member, so each project doesn't re-invent one.
