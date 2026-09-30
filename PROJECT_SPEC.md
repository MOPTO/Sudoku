# Sudoku for Android: Project Specification

> Status: **research + architecture spec, rev 2** (2026-09-30).
> Purpose: this document is the single source of truth for building the app from scratch.
> Existing skeleton: `com.ariadna.sudoku`, AGP 9.4.1, Gradle 9.6.0, compileSdk/targetSdk 37, minSdk 24, Compose template.

---

## 1. Goals (from the wishlist)

**Product direction:** a well-built but simple Sudoku for **relaxed, chill play**. No competition, no pressure: no ranks or scores, no streaks, no daily tasks, no game-over. The timer is opt-in per game.

| # | Goal | How this spec delivers it | Done when (v1) |
|---|------|---------------------------|----------------|
| 1 | **Infinite replayability.** Auto-generated puzzles from very easy to hardest, at least 6×6. | Pure-Kotlin engine: seeded solution generator → clue digger with uniqueness check → **human-technique grader** that assigns difficulty. Grid sizes 4×4, 6×6, 9×9 in v1; 8×8, 12×12, 16×16 and variants (diagonal, windoku) later. A background puzzle pool makes "New game" instant. | Every (size, difficulty) pair offered in the New-game sheet produces a unique-solution puzzle of the requested tier; "New game" opens in < 300 ms when the pool is filled. |
| 2 | **Themes.** Light/dark + palettes that are easy to change. | Material 3 `ColorScheme` + an extended `SudokuColors` token set, provided via `CompositionLocal`. Each palette is one object in one file. Board colors are **derived from the ColorScheme by default**, so a new palette needs about 6 seed colors. Supports System/Light/Dark, Dynamic Color (Android 12+), and a High-Contrast palette. | Adding a palette = one entry in `Palettes.kt`; switching palette/mode applies instantly without restart or startup flash. |
| 3 | **Mistake hints.** Toggle; wrong numbers turn red; more advanced help. | `MistakeMode` = Off / Conflicts (rule duplicates only) / Immediate (checked against the solution). **Smart hints** come from the logical solver: nudge → technique + highlighted cells → reveal. Optional wrong-note detection. | All three modes behave as in §7.3; a hint is available in every reachable board state (§4.8). |
| 4 | **Highlight row & column** of the selected cell. | Layered highlight model: selected cell > conflicts > same digit > row/column/box peers. Each layer can be toggled in Settings. | Each toggle in Settings visibly switches its layer on/off. |
| 5 | **Tablet layout.** | `currentWindowAdaptiveInfo()` (Material 3 Adaptive) plus aspect-ratio checks: stacked layout on phones in portrait, side-by-side board and controls on tablets, landscape, foldables and desktop windows. Hardware keyboard input is supported. | Correct layout on phone portrait/landscape, tablet and the Resizable emulator; the game survives rotation and resizing without losing state. |

Non-goals: online multiplayer, accounts, ads, cloud sync, leaderboards/ranks/scores, streaks, daily challenges, achievements, a mistake limit / game-over, a separate kids mode, Killer/Jigsaw generation (the engine is designed so Killer/Jigsaw could be added later).

---

## 2. Tech stack (decided)

Versions were checked against the official release pages at the end of Sep 2026. Always take the newest **stable** patch when you create the project.

| Area | Choice | Version | Why |
|------|--------|---------|-----|
| Build | Android Gradle Plugin (built-in Kotlin) | 9.4.1 | Already in the template. AGP 9 compiles Kotlin itself, so **do not apply `org.jetbrains.kotlin.android`** (AGP 9 rejects it). |
| Build | Gradle | 9.6.0 | Already in the template (Hilt needs ≥ 9.1). |
| Language | Kotlin | 2.4.0 (template has 2.2.10, bump it) | Stable since June 2026. The Compose compiler plugin and the serialization plugin use the same version. |
| JDK | 17 toolchain (JDK 21 is fine to run Gradle) | — | AGP 9 needs ≥ 17. Replace the template's `VERSION_11` with `VERSION_17`. |
| UI | Jetpack Compose, BOM | 2026.09.00 | Compose UI/Foundation 1.12.x, Material 3 1.4.0. |
| Design system | Material 3 | via BOM | `ColorScheme`, dynamic color, components. |
| Adaptive | `androidx.compose.material3.adaptive:adaptive` (+ `adaptive-layout`, `adaptive-navigation3`) | 1.3.0 | `currentWindowAdaptiveInfo()` with Compact/Medium/Expanded/Large/XL width classes, and fold posture. |
| Navigation | **Navigation 3** (`androidx.navigation3:navigation3-runtime`, `navigation3-ui`) | 1.2.0 | Stable, Compose-first. The back stack is plain state (`rememberNavBackStack`), keys are type-safe `@Serializable` `NavKey`s, and Scenes handle adaptive layouts. |
| ViewModel | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, **`lifecycle-viewmodel-navigation3`** | 2.11.0 | ViewModels scoped to each Nav3 entry (`rememberViewModelStoreNavEntryDecorator()`), plus `collectAsStateWithLifecycle()`. |
| DI | **Hilt** (Dagger) + KSP | Dagger/Hilt 2.60.1 (AGP 9 needs ≥ 2.59), `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.4.0 | Industry standard with compile-time safety. `hiltViewModel()` works inside Nav3 entries. The 1.4.0 artifact doesn't pull in the old Navigation library. |
| Annotation processing | KSP | 2.3.12 | KSP ≥ 2.3.10 is needed for correct R-class resolution under AGP 9 built-in Kotlin. |
| Async | Kotlin Coroutines + Flow | 1.11.0 | Generator runs on `Dispatchers.Default`; state is exposed as `StateFlow`. |
| Serialization | kotlinx.serialization (json) | 1.11.0 | Nav keys, undo history and notes blobs. |
| Settings | Jetpack **DataStore (Preferences)** | 1.2.1 | Async, Flow-based, and safe to use from the main thread. |
| Persistence | **Room** (+ Room Gradle plugin for schema export) | 2.8.5 | Saved games (continue after process death), simple statistics and the puzzle pool. |
| Splash | `androidx.core:core-splashscreen` | 1.2.0 | Themed launch. |
| Core | `core-ktx` 1.19.1, `activity-compose` 1.13.0 | — | Bump from the template's old versions. |
| Tests | JUnit 4 (app) / `kotlin-test` + JUnit 5 (engine module), `kotlinx-coroutines-test`, **Turbine**, Compose UI test, Espresso 3.7.0, `androidx.test.ext:junit` 1.3.0 | — | The engine is fully JVM-testable. |

**Rejected alternatives (and why):**

- *Koin / manual DI*: Koin is fine, but it only fails at runtime. Manual DI is viable for an app this size, but Hilt scales better and is the default in Android guidance.
- *Navigation 2 (navigation-compose)*: still works, but Nav3 is the current recommendation, its back stack is plain state, and its Scenes support adaptive layouts.
- *Proto DataStore*: Preferences DataStore is enough for flat settings.
- *Saving the game in DataStore/JSON only*: Room also gives us history and statistics queries for free.
- *DLX (Dancing Links) solver*: very fast, but more complex. A bitmask + MRV backtracking solver is sub-millisecond on 9×9 and simpler to maintain. Reconsider DLX only if 16×16 generation is too slow.
- *MaterialKolor (seed-based scheme generation)*: a nice optional extra. Hand-authored palettes keep the dependency list minimal and give exact control.

---

## 3. Module & package layout

Two Gradle modules. The whole Sudoku "brain" is pure Kotlin/JVM, so it is fast to test and has no Android dependency.

```
Sudoku/
├─ app/                              (com.android.application, Compose, Hilt, Room)
│  └─ src/main/java/com/ariadna/sudoku/
│     ├─ SudokuApplication.kt        @HiltAndroidApp; starts puzzle-pool warm-up
│     ├─ MainActivity.kt             edge-to-edge, splash, setContent { SudokuRoot() }
│     ├─ di/                         AppModule (dispatchers, app scope, Random), DataModule (Room, DataStore)
│     ├─ data/
│     │  ├─ settings/                SettingsRepository (DataStore), UserSettings model
│     │  ├─ db/                      SudokuDatabase, entities, DAOs, converters
│     │  ├─ game/                    GameRepository (save/load/continue)
│     │  ├─ pool/                    PuzzlePoolRepository (pre-generated puzzles per config)
│     │  └─ stats/                   StatsRepository (read-only queries over solved games)
│     ├─ ui/
│     │  ├─ SudokuRoot.kt            theme + NavDisplay
│     │  ├─ navigation/              NavKeys (Home, Game, Stats, Settings, Techniques), entry provider
│     │  ├─ theme/                   Palette.kt, Palettes.kt, SudokuColors.kt, Theme.kt, Type.kt, Shapes.kt
│     │  ├─ home/                    HomeScreen, HomeViewModel, NewGameSheet
│     │  ├─ game/
│     │  │  ├─ GameScreen.kt         adaptive container (stacked / side-by-side)
│     │  │  ├─ GameViewModel.kt
│     │  │  ├─ GameScreenAction.kt   UI-level actions (wraps core GameAction + hint/pause/completion)
│     │  │  ├─ GameTimer.kt          elapsed-time tracking (only for timed games)
│     │  │  ├─ GameUiState.kt        UI models (CellUi, BoardUi, PadUi, HintUi, ...)
│     │  │  ├─ GameUiMapper.kt       GameSession + Settings → GameUiState (highlights computed here)
│     │  │  └─ components/           SudokuBoard, SudokuCell, NotesGrid, NumberPad, GameToolbar,
│     │  │                           GameTopBar, HintPanel, CompletionDialog, KeyboardInput
│     │  ├─ stats/                   StatsScreen, StatsViewModel
│     │  ├─ settings/                SettingsScreen, SettingsViewModel, PalettePicker
│     │  └─ components/              shared composables
│     └─ util/                       haptics, formatting (time), window helpers
│
└─ core/                             (org.jetbrains.kotlin.jvm, NO Android)
   └─ src/main/kotlin/com/ariadna/sudoku/core/
      ├─ model/      GridSpec, Variant, Houses, Grid, Candidates (bitmask), Digit, CellIndex helpers
      ├─ solver/     BruteForceSolver (count solutions, limit=2), SolveResult
      ├─ logic/      LogicalSolver, Technique enum, Step, techniques/* (one file per technique)
      ├─ generator/  SolutionGenerator, PuzzleDigger, PuzzleGenerator, GenerationConfig
      ├─ grading/    Difficulty, DifficultyGrader, DifficultyProfile (per GridSpec)
      ├─ hint/       HintEngine → Hint (nudge/technique/reveal)
      └─ game/       GameSession (persisted state), InputState (transient), GameState, GameAction,
                     Move, History (undo/redo), reducer, MistakeChecker, NotesOps (auto-notes, auto-remove)
```

Dependency direction: `ui → data → core` and `ui → core`. `core` has no Android dependency. It uses only the Kotlin stdlib, kotlinx.serialization (for `@Serializable` models) and kotlinx-coroutines-core (only for cooperative cancellation, `ensureActive()`, in the generator).

Note on AGP 9: `core` applies `org.jetbrains.kotlin.jvm` (allowed; only `kotlin.android` is rejected). Keep the Kotlin version in one place (`libs.versions.toml → kotlin`) so the JVM plugin and AGP's built-in Kotlin agree.

---

## 4. Sudoku engine (`:core`), the heart of goal #1

### 4.1 Generic grid model

Everything is parameterized by `GridSpec`, never hard-coded to 9.

```kotlin
@Serializable
data class GridSpec(
    val size: Int,        // N: digits 1..N, N×N cells
    val boxRows: Int,     // box height
    val boxCols: Int,     // box width  (boxRows * boxCols == size)
    val variant: Variant = Variant.Classic,
) {
    val cellCount get() = size * size
    companion object {
        val Mini4   = GridSpec(4, 2, 2)   // smallest, gentlest
        val Six     = GridSpec(6, 2, 3)   // required
        val Eight   = GridSpec(8, 2, 4)   // later
        val Classic = GridSpec(9, 3, 3)
        val Twelve  = GridSpec(12, 3, 4)  // later
        val Sixteen = GridSpec(16, 4, 4)  // later; slower generation, pool only
    }
}

enum class Variant { Classic, Diagonal /* X-Sudoku */, Windoku /* hyper */ }
```

**Houses (units)** are precomputed once per spec. A house is a list of cell indices that must contain each digit exactly once: rows + columns + boxes (+ 2 diagonals for `Diagonal`, + 4 extra boxes for `Windoku`). **Every solver, technique, highlighter and conflict checker works over `houses` and `peers[cell]`**, which is why variants cost almost nothing to add. Jigsaw works the same way (irregular boxes, with the house layout stored per puzzle). Killer needs a separate cage-sum constraint and is out of scope.

```kotlin
class Houses(spec: GridSpec) {
    val all: List<IntArray>              // every house
    val housesOf: Array<IntArray>        // cell -> indices of houses containing it
    val peers: Array<IntArray>           // cell -> distinct peer cells (for 9×9 classic: 20)
}
```

**Candidates** are an `Int` bitmask (bit `d-1` means digit `d` is possible). Sizes ≤ 16 fit easily. Wrap it in `@JvmInline value class Candidates(val bits: Int)` with `count` (`Integer.bitCount`), `contains`, `single`, iteration and so on.

**Grid** is an `IntArray(cellCount)` (0 = empty) plus a `BooleanArray` for givens. It serializes to a compact string (`"530070000600195000..."`; for size > 9 use base-36 chars: `1-9, A-G`).

### 4.2 Brute-force solver (uniqueness oracle)

- Backtracking over bitmask candidates.
- **MRV heuristic**: always branch on the empty cell with the fewest candidates.
- Constraint propagation at each node: naked singles + hidden singles until fixpoint (keeps the tree tiny).
- API: `countSolutions(grid, limit = 2): Int` and `solve(grid, random: Random? = null): IntArray?`.
  - With a `Random`, it tries candidates in shuffled order. This is how the *solution generator* works.
- Performance target: 9×9 uniqueness check < 1 ms, 16×16 < 50 ms typical on a mid-range phone.

### 4.3 Solution generator

1. Start with an empty grid for the spec.
2. `solve(empty, random)` gives a random complete valid grid (for 9×9 you can pre-fill the 3 diagonal boxes randomly first, since they're independent, for speed).
3. Optional extra shuffle (cheap and preserves validity): permute digits, rows within a band, bands, columns within a stack, stacks, and transpose when `boxRows == boxCols`. Skip transforms that break the variant (e.g. a Diagonal puzzle only allows the symmetric subset).

**Determinism:** use `kotlin.random.Random(seed)`. One **generation attempt** is a pure function `attempt(spec, difficulty, seed): Puzzle?` (solution + digging both driven by that single `Random(seed)`). A puzzle is therefore reproducible from `(GridSpec, Difficulty, seed, GENERATOR_VERSION)`, where `seed` is the seed of the attempt that **succeeded** (see §4.4). This enables shareable puzzle codes (post-v1). Always also store the resulting givens and solution in the DB, because a change to the algorithm changes the output for a seed.

### 4.4 Digger (clue removal)

```
givens = full solution
target = random clue count in [profile.minClues, profile.maxClues]   // varies clue counts within a tier
order  = shuffled cell list (optionally as symmetric pairs: cell & its 180° mirror → classic look)
for cell in order:
    if givens.count <= target: break
    if removing cell (and its mirror) would go below profile.minClues: continue
    candidate = givens with cell (and its mirror) removed
    if countSolutions(candidate, 2) != 1: continue            // must stay unique
    if !LogicalSolver(profile.maxTechnique).solves(candidate): continue   // must stay within target ceiling
    givens = candidate
```

- The **ceiling check** (the logical solver restricted to techniques ≤ target tier) guarantees the puzzle is *not harder* than requested.
- After digging, the **floor check** makes sure the puzzle is *not easier* than requested: the grader must report `hardestTechnique.tier` within the profile's tier range (and `score ≥ profile.minScore`). If it fails, the attempt returns `null`.
- **Retries stay deterministic:** the outer loop derives the next seed from the previous one (`seed = splitMix64(seed)`) and calls `attempt(spec, difficulty, seed)` again. The seed recorded with the puzzle is the one whose attempt succeeded, so re-running a single attempt with that seed reproduces the puzzle exactly.
- Budget: stop after `maxAttempts` or when the coroutine is cancelled (`ensureActive()` inside loops). If the budget runs out, return the best-so-far attempt: the smallest miss, compared first by the tier gap, then the clue-count gap, then the score shortfall.
- **Floor-seeking:** if the puzzle is still easier than the profile's tier floor when the digger reaches its target, it keeps digging towards `minClues` until the floor is met (the ceiling still applies to every removal).
- **Uniqueness shortcut:** when the profile has a ceiling, the logical solve replaces the brute-force count: every logical step is a sound deduction, so solving the grid by logic alone proves the solution is unique. Profiles whose tier range reaches above the implemented techniques have no checkable ceiling and use the brute-force count.
- Milestone 3 status: implemented in `core/generator/` (symmetric digging on by default). Measured on a desktop JVM: every offered pair, 9×9 Expert included, hits its clue range in ≈ 1–2 attempts and < 2 ms p95.
- Milestone 4 status: ceiling and floor checks in place, `GENERATOR_VERSION = 2`, and the default budget is `maxAttempts = 500`. Measured on a desktop JVM (100 seeds per pair), every offered pair is 100 % in range:

  | Pair | Average attempts | p95 |
  |------|------------------|-----|
  | 4×4 and 6×6 Easy/Medium, 9×9 Easy | 1–4 | < 1 ms |
  | 9×9 Hard | ≈ 4 | 4 ms |
  | 9×9 Expert | ≈ 9 | 10 ms |
  | 6×6 Hard | ≈ 35 | 6 ms |
  | 9×9 Medium | ≈ 25 | 38 ms |

  9×9 Medium is the slowest because T3 is rare at 30–37 clues.

### 4.5 Logical solver & difficulty grading

The logical solver repeatedly finds the **easiest applicable step** (technique list ordered by difficulty), applies it (places a digit or removes candidates), and records a `Step`. The grade is:

- **Tier** = hardest technique the puzzle needed.
- **Score** = Σ technique base cost per step (distinguishes "easy Hard" from "hard Hard" within a tier). Internal only: used by the generator and for tuning, never shown to the player.
- If the solver gets stuck with all techniques, the puzzle is **Extreme** (needs chains/bifurcation). It is still valid because uniqueness is guaranteed.
- While only some tiers are implemented, a stuck grade reports `tier = LogicalSolver.MAX_TIER + 1` (T4 while T1–T3 exist) as a **lower bound** ("needs something beyond what's implemented"). It follows `MAX_TIER` automatically as tiers are added.

Techniques, with the implementation order in brackets. References: sudokuwiki.org strategy list, HoDoKu, Sudoku Explainer ratings.

| Tier | Techniques | Base cost (guide) |
|------|-----------|-------------------|
| T1 Singles-basic | Full House (last empty cell in a house), Naked Single | 4, 4 |
| T2 Singles | Hidden Single (box first, then row/col) | 14 |
| T3 Intersections & pairs | Locked Candidates (Pointing / Claiming), Naked Pair, Hidden Pair | 50, 60, 70 |
| T4 Subsets & basic fish | Naked Triple/Quad, Hidden Triple/Quad, X-Wing | 80–150, 140 |
| T5 Advanced | Swordfish, XY-Wing, XYZ-Wing, W-Wing, Simple Coloring, Unique Rectangle type 1 | 150–200 |
| T6 Master | Jellyfish, X-Chain, XY-Chain, (later: AIC) | 250+ |
| T7 Extreme | nothing above works → trial & error | — |

Implement T1–T4 for v1 (enough for Easy → Hard). In v1, "Expert" = "the T1–T4 solver gets stuck" (uniqueness still guaranteed). Add T5/T6 post-v1 to split Expert/Master properly.

### 4.6 Difficulty ladder (per grid size)

Clue counts are **bounds for the digger, not the definition of difficulty**. The technique tier is what matters. Ranges deliberately overlap so that every clue count belongs to at least one difficulty.

| UI label | 4×4 | 6×6 | 9×9 (clue range, tier) |
|----------|-----|-----|------------------------|
| **Easy** | 6–10 givens, T1 | 16–24, ≤ T2 | 36–45, ≤ T2 |
| **Medium** | 4–7, T1 | 10–14, T2 required | 30–37, T3 required |
| **Hard** | — | 9–12, T3 or beyond | 26–31, T4 required |
| **Expert** | — | — | 22–27, beyond T4 (v1) / T5 required (post-v1) |
| **Master** (post-v1) | — | — | 17–25, T6/T7 |

(Mathematical minimums: 4×4 = 4 clues, 6×6 = 8, 9×9 = 17.)

**Small grids sit lower on the ladder** (measured in milestone 4 over 2000 near-minimal digs per size):
- **4×4:** every unique puzzle falls to T1, so the two 4×4 difficulties differ only in clue count.
- **6×6:** T3 steps are rare (≈ 0.3 %). Medium is therefore "hidden single required", and Hard is "the T2 solver gets stuck".

**Interim until T4 exists (milestones 4–10):** 9×9 Hard and Expert both mean "the T1–T3 solver gets stuck" (tier range T4..T7, no checkable ceiling), and differ only in clue range. Milestone 11 tightens Hard to T4 and Expert to T5..T7. Easy on 4×4 and 6×6 is the gentlest entry point for beginners and children; there is **no separate kids mode**. Keep these numbers in `DifficultyProfile` tables so they are easy to tune after play-testing.

### 4.7 Puzzle pool (instant "New game")

Easy/Medium 9×9 generate in milliseconds, but Expert/Master can take hundreds of ms to seconds (hard puzzles are rare in a random search), and 16×16 takes longer still.

- `PuzzlePoolRepository` keeps **3 ready puzzles per (GridSpec, Difficulty)** in Room (`pool_puzzle` table), tagged with `GENERATOR_VERSION`.
- "New game" = `pool.take(spec, difficulty)`: in **one Room transaction**, read the oldest matching row, insert the `game` row built from it, and **delete the pool row**. The puzzle leaves the pool the moment a game starts, so it can never be handed out twice. If the pool is empty, generate on demand with a progress indicator and Cancel.
- On app start (Application-scoped coroutine, `Dispatchers.Default`, low priority) and after each `take`, it refills missing slots one at a time.
- Bumping `GENERATOR_VERSION` invalidates the pool (rows with an older version are deleted at startup).

### 4.8 Hint engine

`HintEngine.next(session): Hint`:

1. **If the board has a wrong entry** (compared to the solution), the hint is `Hint.FixMistake(cell)`. Logic on a wrong board is meaningless, so this always comes first.
2. **If the player's notes are missing the true candidate** in a cell that has at least one note, return `Hint.BadNote(cell, digit)`. Cells without notes are ignored.
3. Otherwise, run the `LogicalSolver` from the current state (using the *real* candidates, not the player's notes) and take the first step, giving `Hint.Step(technique, focusHouse, cells, eliminations/placement)`.
4. **Fallback:** if the solver is stuck (Expert/Master puzzles need techniques beyond the implemented ones), return `Hint.Reveal(cell)` for the empty cell with the fewest real candidates. A hint therefore exists in every non-solved state.

`HintEngine.next` can take noticeable time on large grids, so the ViewModel runs it on `Dispatchers.Default` and passes the result into the reducer as `GameAction.ShowHint(hint)`. The reducer itself never runs solver logic.

The UI reveals it **progressively** (each level counts as one hint use; the count is informational only):

| Level | Shows |
|-------|-------|
| 1. Nudge | "Look at **row 4**" (highlights the house) |
| 2. Technique | "**Hidden Single**: 7 can only go in one cell of row 4" + highlights the involved cells/candidates |
| 3. Reveal | Places the digit / applies the eliminations to the notes |

Each technique has a short explanation string resource (and later a "Techniques" help screen with illustrated examples).

### 4.9 Game state (pure, in `:core`)

The state is split in two so that **only real moves change what gets saved**. Selecting a cell or toggling notes mode must not trigger a database write.

```kotlin
/** Everything that is persisted. Changes only on real moves (input, erase, notes, undo/redo, hint reveal). */
@Serializable
data class GameSession(
    val id: Long,
    val spec: GridSpec,
    val difficulty: Difficulty,
    val seed: Long,
    val timed: Boolean,            // chosen in the New-game sheet, fixed for this game
    val givens: Grid,              // immutable puzzle
    val solution: IntArray,
    val values: IntArray,          // player's values (givens included)
    val notes: IntArray,           // Candidates bits per cell (pencil marks)
    val mistakes: Int,             // counted only in MistakeMode.Immediate (see below)
    val hintsUsed: Int,
    val history: History,          // undo/redo stacks of Move
    val status: GameStatus,        // InProgress, Solved
)

/** Transient input/UI-facing state. Never persisted; defaults are restored on load. */
data class InputState(
    val selected: Int? = null,             // selected cell index
    val notesMode: Boolean = false,
    val lockedDigit: Int? = null,          // digit-first input mode
    val hint: HintState? = null,           // current hint + reveal level (1..3)
    val checkedWrongCells: Set<Int> = emptySet(),   // result of a manual Check, cleared after 3 s
)

data class GameState(val session: GameSession, val input: InputState)

sealed interface GameAction {
    data class SelectCell(val index: Int) : GameAction
    data class MoveSelection(val dRow: Int, val dCol: Int) : GameAction   // keyboard / d-pad
    data class Input(val digit: Int) : GameAction
    data object Erase : GameAction
    data object ToggleNotesMode : GameAction
    data class LockDigit(val digit: Int?) : GameAction
    data object Undo : GameAction
    data object Redo : GameAction
    data object FillAllNotes : GameAction              // auto-notes
    data class ShowHint(val hint: Hint) : GameAction   // computed by the VM off the main thread (§4.8)
    data object AdvanceHint : GameAction
    data object DismissHint : GameAction
    data object CheckBoard : GameAction                // one-shot check when MistakeMode = Off
    data object ClearCheck : GameAction                // sent by the VM 3 s after CheckBoard
}

fun GameState.reduce(action: GameAction, rules: GameRules): GameState   // pure function
```

- **Time is not part of the reducer.** Elapsed time lives in the app's `GameTimer` (§5.1) and is written to the `game` row alongside the session. There is no `Tick` action, so the board state doesn't change every second.
- `Move` = `{cell, oldValue, newValue, oldNotes, newNotes, peerNoteChanges}`, so undo restores notes that "auto-remove notes" cleared in peers.
- `GameRules` (from settings): `mistakeMode`, `autoRemoveNotes`.
- **Mistake counting:** only in `Immediate` mode. Each `Input` of a digit that differs from the solution adds 1. Undo does **not** subtract. In `Off` and `Conflicts` the counter stays 0 and is hidden. The count is informational; there is no mistake limit and no game-over.
- **Check** (`Off` mode only): marks wrong cells for 3 s and counts as one hint use.
- The reducer is **pure, cheap and fully unit-tested**. The ViewModel only orchestrates.
- Solved condition: `values contentEquals solution` → `status = Solved`. There is no lose condition.
- Use `IntArray` internally for speed. Override `equals`/`hashCode` (or wrap arrays) because data-class equality on arrays is reference-based. The reducer always copies arrays before changing them (never mutates in place).
- Milestone 5 status: implemented in `core/game/` (reducer in `GameReducer.kt`), with these refinements to the sketch above:
  - `Move = {cell?, changes: List<CellChange>}`, where each `CellChange` holds one cell's old/new value and notes. Peer notes cleared by auto-remove, auto-notes and hint reveals are therefore each **one** undoable move. `History` keeps the last 200 moves itself.
  - Entering the digit a cell already holds erases it (also in digit-first mode). Undo/redo selects the move's `cell` (auto-notes has none and keeps the selection).
  - Each hint level counts as one hint use; level 3 applies the hint as a normal move and keeps it shown. A reveal that no longer matches the board is skipped. Any recorded move dismisses the hint; changing a checked cell drops its Check mark.
  - Once `Solved`, every action that would change the session is ignored.
  - `GameSession` is not `@Serializable` (Room stores its fields, §6.1); `History`/`Move` are, for the `history` column. The `Hint` data types (`core/hint/Hint.kt`) exist already; `HintEngine` follows in milestone 10.
  - `MistakeChecker` (conflicts, wrong cells, wrong notes) and `NotesOps` (candidates, auto-notes, auto-remove) are shared with the UI mapper and the hint engine.

---

## 5. App architecture (MVVM + UDF)

```
┌───────────── UI (Compose) ─────────────┐
│ GameScreen(state, timerText, onAction) │  stateless, previewable
│   ↑ state: StateFlow<GameUiState>      │
│   ↓ onAction(GameScreenAction)         │
├──────────── GameViewModel ─────────────┤
│ state: MutableStateFlow<GameState>     │
│ settings: Flow<UserSettings>           │
│ uiState = combine(state, settings)     │
│          .map(GameUiMapper::map)       │  highlights/conflicts computed here
│          .flowOn(Default)              │
│          .stateIn(viewModelScope, …)   │
│ timer: GameTimer (separate flow)       │
│ onAction → state.update { reduce }     │
│ side effects: autosave, timer, hints,  │
│               completion               │
├──────────── Repositories ──────────────┤
│ GameRepository · SettingsRepository    │
│ PuzzlePoolRepository · StatsRepository │
├──────────── Data sources ──────────────┤
│ Room (SudokuDatabase) · DataStore      │
├──────────── :core (pure Kotlin) ───────┤
│ generator · solver · grader · hints    │
│ GameSession reducer                    │
└────────────────────────────────────────┘
```

Rules:

- **Single immutable `UiState` per screen**, exposed as `StateFlow` and collected with `collectAsStateWithLifecycle()`.
- **One entry point for events**: `fun onAction(action: GameScreenAction)`. Screens take `(state, onAction)` and are stateless, so they preview with fake state. `GameScreenAction` lives in `:app` and keeps UI concerns out of `:core`:

```kotlin
sealed interface GameScreenAction {
    data class Game(val action: GameAction) : GameScreenAction   // forwarded to the pure reducer
    data object RequestHint : GameScreenAction                   // VM runs HintEngine, then Game(ShowHint(...))
    data object Pause : GameScreenAction                         // timed games only
    data object Resume : GameScreenAction
    data object NextPuzzle : GameScreenAction                    // from the completion dialog
}
// Pure navigation (back, open settings) stays as plain lambdas: onBack, onOpenSettings.
```
- **One-off outcomes are state, not events** (per Android guidance): e.g. `GameUiState.completion: CompletionUi?`. The UI shows a dialog while it's non-null, and dismissing it sends an action.
- `GameUiMapper` does all derived work (highlight layers, conflicts, digit counts, pad enable/disable), so composables only draw.
- ViewModels are scoped to Nav3 entries via `rememberViewModelStoreNavEntryDecorator()` and get their arguments from the `NavKey` (e.g. `Game(gameId)`) through Hilt assisted injection:

```kotlin
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted private val key: GameKey,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    @DefaultDispatcher private val default: CoroutineDispatcher,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: GameKey): GameViewModel }
    …
}

// in entryProvider
entry<GameKey> { key ->
    val vm = hiltViewModel<GameViewModel, GameViewModel.Factory> { it.create(key) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    GameScreen(state, vm::onAction, onBack = { backStack.removeLastOrNull() })
}
```

### 5.1 Side effects in `GameViewModel`

| Concern | Implementation |
|---------|----------------|
| **Timer** | Only when `session.timed`. `GameTimer` stores `accumulatedMs` plus `runningSince: Long?` (a `SystemClock.elapsedRealtime()` timestamp). Elapsed = `accumulatedMs + (now − runningSince)`, so it is real time and never drifts. It runs only while the game is in progress, not paused, *and* the lifecycle is RESUMED (`LifecycleResumeEffect` in the screen sends `Pause`/`Resume`). The displayed text comes from a separate `timerText: StateFlow<String>` that updates once per second, so only the timer label recomposes, not the board. Untimed games have no timer at all (nothing tracked, nothing shown, no Pause button). |
| **Autosave** | `state.map { it.session }.distinctUntilChanged().drop(1).debounce(500).collect { games.save(it, timer.elapsedMs) }`. Selection, notes mode and hint display don't change `session`, so they never cause a write. Also an immediate save on `Pause`, on lifecycle `ON_STOP` and in `onCleared` (these also persist the current elapsed time). |
| **Hints** | `RequestHint` → `withContext(Default) { HintEngine.next(session) }` → `Game(ShowHint(hint))`. |
| **Check** | After `CheckBoard`, the VM sends `ClearCheck` 3 s later (cancelled if another `CheckBoard` arrives). |
| **Process death** | The `GameKey(gameId)` is saved in the Nav3 back stack (serializable). On recreation, the VM loads the session and elapsed time from Room, and `InputState` starts from its defaults. At most the last 500 ms of moves can be lost. |
| **Completion** | When the reducer sets `Solved`, the VM stops the timer, saves the game as `SOLVED` with `completedAt`, and exposes the completion UI state. (The pool row was already removed when the game started, §4.7.) |
| **Haptics** | Emitted as a state-driven "last feedback" token or done directly in the UI on action (lightweight). |

- Milestone 6 status: the phone game screen is implemented in `ui/game/` (VM, mapper, timer) and `ui/game/components/`, with these refinements:
  - `GameRepository` and `SettingsRepository` are interfaces. Until milestone 7 they are backed by `InMemoryGameRepository` (generates on demand with `PuzzleGenerator.generate`) and `DefaultSettingsRepository` (always the §6.2 defaults), so games do not survive process death yet: a restored `GameKey` whose game is gone shows `GameUiState.NotFound` and pops back to Home.
  - `GameScreenAction` also has `ScreenResumed`/`ScreenPaused` (sent by `LifecycleResumeEffect`) and `DismissCompletion` (closes the dialog, the solved board stays visible). The timer runs while the game is in progress, the player hasn't pressed Pause, and the screen is resumed. It resumes by itself when the screen comes back, while a Pause the player pressed stays until Resume. `ScreenPaused` and `Pause` save at once.
  - `ScreenState` (paused, solved time, dialog dismissed, next game id) is the VM's screen-level state next to `GameState`. The completion dialog appears once the VM has stopped the timer and recorded the solve time.
  - Home has a temporary quick-start (size, difficulty, timer) instead of the New-game sheet (milestone 7). The top-bar menu and haptics also come in milestone 7, and hints (`RequestHint`, Hint button, hint panel, hint-focus highlight) in milestone 10.
  - `MaterialTheme.motionScheme` is still internal in Material 3 1.4.0, so cell backgrounds use `animateColorAsState`'s default spec. Switch to `motionScheme.fastEffectsSpec()` (§7.6) once it is public.
  - `SudokuColors` (§8.1) is already provided by `SudokuTheme`, derived from the template color scheme. Milestone 8 adds the palettes.

### 5.2 Navigation (Nav3)

```kotlin
@Serializable data object HomeKey : NavKey
@Serializable data class GameKey(val gameId: Long) : NavKey
@Serializable data object StatsKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data object TechniquesKey : NavKey            // later
```

- A single back stack: `rememberNavBackStack(HomeKey)`.
- `NavDisplay(entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()))`.
- The **New game** picker (size, variant, difficulty, timer on/off) is a `ModalBottomSheet` on phones and a dialog on wide windows. It is not a destination. After a game is created, `backStack.add(GameKey(id))`.
- "New game" from the completion dialog replaces the top entry: `backStack[lastIndex] = GameKey(newId)`.
- Optional (tablet polish): `adaptive-navigation3` `ListDetailSceneStrategy` for Stats (mode list → detail) and Settings (category list → detail).

---

## 6. Persistence

### 6.1 Room (`SudokuDatabase`, schema exported via the Room Gradle plugin)

```kotlin
@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val size: Int, val boxRows: Int, val boxCols: Int, val variant: String,
    val difficulty: String,
    val seed: Long,
    val generatorVersion: Int,
    val timed: Boolean,
    val givens: String,          // encoded grid
    val solution: String,
    val values: String,
    val notes: String,           // fixed-width hex: 4 hex chars (16 bits) per cell, row-major
    val history: String?,        // JSON (kotlinx.serialization), capped to last 200 moves
    val elapsedMs: Long,         // always 0 when !timed
    val mistakes: Int,
    val hintsUsed: Int,
    val status: String,          // IN_PROGRESS, SOLVED, ABANDONED
    val createdAt: Long, val updatedAt: Long, val completedAt: Long?,
)

@Entity(tableName = "pool_puzzle", indices = [Index("size","boxRows","boxCols","variant","difficulty")])
data class PoolPuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val size: Int, val boxRows: Int, val boxCols: Int, val variant: String,
    val difficulty: String, val seed: Long, val score: Int,
    val givens: String, val solution: String, val generatorVersion: Int,
)
```

**Status mapping** (core ↔ entity): `GameStatus.InProgress` ↔ `IN_PROGRESS`, `GameStatus.Solved` ↔ `SOLVED`. `ABANDONED` exists only in the database: `GameRepository` sets it on the previous in-progress game when a new game is started. There is **at most one `IN_PROGRESS` game** at a time. If one exists, the New-game sheet shows "This will replace your current game" before starting. Abandoned games are excluded from statistics. Paused is not a status; it's transient VM state.

DAOs: `observeInProgress(): Flow<GameEntity?>` (drives "Continue" on Home), `upsert`, `getById`, and simple stat queries per (size, difficulty): puzzles solved, and best/average time over **timed** solved games.

**Migrations:** schemas are exported to `app/schemas/` and committed. Every schema change bumps the DB version and ships an `@AutoMigration` (or a manual `Migration` when Room can't infer it). Never use `fallbackToDestructiveMigration` because it would wipe the player's saved game.

**Backup:** Android Auto Backup is left on (default) for the Room DB and DataStore, so a phone restore brings back settings and the current game. Restored pool rows are harmless: they carry `generatorVersion` and are purged if outdated.

### 6.2 DataStore (Preferences), `UserSettings`

| Key | Type | Default |
|-----|------|---------|
| themeMode | System / Light / Dark | System |
| paletteId | string | `classic` |
| dynamicColor | bool | false (palettes are part of the app identity) |
| pureBlackDark | bool | false (AMOLED) |
| mistakeMode | Off / Conflicts / Immediate | Immediate |
| detectWrongNotes | bool | false |
| highlightRowCol | bool | true |
| highlightBox | bool | true |
| highlightSameDigit | bool | true |
| highlightSameDigitNotes | bool | true |
| autoRemoveNotes | bool | true |
| hideCompletedDigits | bool | true (dims the pad digit when all N are placed) |
| showRemainingCount | bool | true |
| inputMode | CellFirst / DigitFirst | CellFirst |
| haptics | bool | true |
| lastNewGame | spec + difficulty + timer (bool) | 9×9, Medium, timer off |

`lastNewGame` is not shown on the Settings screen. The New-game sheet pre-selects it and writes it back whenever a game is started, so the timer choice (and size/difficulty) is remembered between games.

---

## 7. UI & features

### 7.1 Board rendering

- `SudokuBoard` is a square `Layout` of `SudokuCell` composables (N×N). It takes the largest square that fits (`BoxWithConstraints` → `min(maxWidth, maxHeight)`).
- Grid lines are drawn once in the board's `Modifier.drawWithContent`: thin lines between cells and thick lines on box borders (and a tinted diagonal for the `Diagonal` variant, tinted extra boxes for `Windoku`).
- **Why per-cell composables and not a single Canvas:** each cell gets real accessibility semantics (`contentDescription = "Row 3, column 5, 7, given"`, click action). Stable `@Immutable data class CellUi` lets Compose (strong skipping is on by default) skip unchanged cells, so 81–256 cells are fine. A single-Canvas board is the fallback if profiling ever shows a problem.
- Text is sized relative to the cell (`with(LocalDensity) { (cellPx * 0.6f).toSp() }`). Notes are drawn as a mini √N×√N-ish grid (`boxRows × boxCols`) inside the cell.
- Tap selects; a long-press on a filled cell could toggle "lock digit" (later).

```kotlin
@Immutable
data class CellUi(
    val index: Int,
    val value: Int,                 // 0 = empty
    val isGiven: Boolean,
    val notes: Int,                 // bitmask
    val highlight: CellHighlight,   // Selected, Conflict, SameDigit, Peer, HintFocus, None
    val valueState: ValueState,     // Normal, Wrong, Conflict, Revealed
    val notesHighlightedDigit: Int, // digit to emphasise inside notes (0 = none)
)
```

### 7.2 Highlighting (goal #4)

Computed in `GameUiMapper` with **fixed priority** (highest wins for background):

1. **Selected cell**: strong tint (`selectedCell`)
2. **Hint focus** cells/house (when a hint is shown)
3. **Conflict / error cell**: error container tint
4. **Same digit** as the selected cell's value (or the locked digit): `sameDigit` tint
5. **Peers**: same row + column (`highlightRowCol`) and same box (`highlightBox`): subtle `peer` tint. Also the variant houses (diagonals) when the selected cell is on them.
6. None

In **DigitFirst** mode, the "selected" concept is the locked digit: all its cells get the same-digit highlight, and the pad shows it as checked.

### 7.3 Mistake feedback (goal #3)

| `MistakeMode` | Behavior | Uses solution? |
|---------------|----------|----------------|
| **Off** | Nothing is marked. The toolbar offers a manual **Check** (one-shot: marks wrong cells for 3 s, counts as a hint). | Only on Check |
| **Conflicts** | Marks rule violations only: both duplicate cells in a row/col/box turn red. A wrong-but-not-yet-conflicting digit is **not** revealed (purist mode). | No |
| **Immediate** | A digit that differs from the solution gets red text + error-tinted background, and the mistake counter goes up (§4.9). Conflicting peers are also outlined. | Yes |

- There is **no mistake limit and no game-over**. Mistakes only give feedback; the player can always finish the puzzle.
- **Wrong-note detection** (advanced, optional): notes that exclude the true digit for a cell are shown in the error color.
- **Colorblind-safe:** errors are never color-only. Wrong digits also get an underline/strike mark, and the High-Contrast palette uses distinct error hues.

### 7.4 Controls

- **Number pad**: 1..N (`A–G` for 10–16). Shows the remaining count for each digit and disables/dims digits that are fully placed. Layout: a single row on phones for N ≤ 9 (two rows for 12/16), and a `boxRows × boxCols` grid in the side panel on tablets.
- **Toolbar**: Undo, Redo, Erase, Notes toggle (pencil), Auto-notes, Hint, Check (only when MistakeMode = Off).
- **Top bar**: back, difficulty label + size, mistakes counter (Immediate mode only), menu (restart, new game, settings). **Timed games only:** timer text and a Pause button (tapping the timer also pauses).
- **Pause overlay** (timed games only) hides the board so paused time can't be used to think, and resumes on tap. Untimed games have nothing to pause: leaving the screen simply saves.
- **Hardware keyboard** (tablets, Chromebooks, desktop windowing), via `onPreviewKeyEvent` on the board: `1–9` and `A–G` input (letters only on grids with N > 9), **arrow keys** move (no WASD, because `A` and `D` are digits on 12×12/16×16), `Backspace/Delete` erase, `N` notes toggle, `Ctrl+Z/Ctrl+Y` undo/redo, `H` hint, `Space` pause (timed games).

### 7.5 Home / new game / stats

- **Home**: "Continue" card (in-progress game: size, difficulty, progress %, and time if timed), **New game**, Statistics, Settings.
- **New-game sheet**: size chips (4×4, 6×6, 9×9; later 8×8/12×12/16×16), variant chips (Classic; later Diagonal, Windoku), difficulty segmented control filtered to the difficulties that size supports (§4.6), and a **Timer** switch. It opens pre-filled from `lastNewGame` and saves the choice back when "Start" is pressed (§6.2).
- **Completion dialog**: a standard M3 `AlertDialog` with time (if timed), mistakes (if Immediate mode), hints used, "Next puzzle (same settings)", "Home". No scores, ratings, badges or celebration effects.
- **Statistics** (kept light): per (size, difficulty): puzzles solved, and best/average time over timed games. No win %, no streaks.

### 7.6 Motion & animation

**Principle: use only the platform and Material 3 defaults.** Follow the official Android guidance. No custom choreography, hand-picked durations, custom easing curves, or decorative effects (confetti, wobble, bounce, particles, flips). Motion should explain a change of state, not decorate it.

**Screen transitions (Navigation 3).** `NavDisplay` animates between destinations automatically. Do **not** pass `transitionSpec`, `popTransitionSpec` or `predictivePopTransitionSpec` to `NavDisplay`, and do not set the per-entry `NavDisplay.TransitionKey` / `PopTransitionKey` / `PredictivePopTransitionKey` metadata. The library defaults are used for forward, back and predictive-back navigation.

**Predictive back.** The app targets API 37 (≥ 36), so predictive back and the system back-to-home animation are on by default. No `android:enableOnBackInvokedCallback` manifest flag is needed. Don't intercept back with custom `BackHandler`/`PredictiveBackHandler` animations; leave it to Nav3 and the Material components.

**Material components animate themselves.** `ModalBottomSheet` (New-game sheet, including its built-in predictive-back animation), `AlertDialog`, `DropdownMenu`, `Snackbar`, `Switch`, segmented buttons, chips and the ripple/pressed states on the number pad and toolbar all use their built-in motion. Don't wrap or override it.

**Motion scheme.** `MaterialTheme` keeps its default `MotionScheme.standard()`. Don't use `expressive()`, whose springier motion doesn't fit a calm game. When the app animates its own UI, it takes specs from `MaterialTheme.motionScheme`, never from literal `tween(…)`/`spring(…)` values:
- colour and alpha changes → `motionScheme.defaultEffectsSpec()` (or `fastEffectsSpec()` for per-tap feedback)
- size and position changes → `motionScheme.defaultSpatialSpec()`

**The complete list of app-level animations** (anything not listed here is not animated):

| What changes | API (per the Compose animation quick guide) |
|--------------|---------------------------------------------|
| Cell background when selection/highlights change | `animateColorAsState(target, motionScheme.fastEffectsSpec())` drawn with `Modifier.drawBehind` (not `Modifier.background`, to avoid extra recompositions) |
| Board colors when the palette or light/dark mode changes | `animateColorAsState` on the `SudokuColors` tokens with `defaultEffectsSpec()` |
| Showing/hiding UI parts: hint panel, Check button, pause overlay, Continue card | `AnimatedVisibility` with its default enter/exit |
| Hint panel text changing between levels (nudge → technique → reveal) | `AnimatedContent` with its default transition |
| Hint panel height following its text | `Modifier.animateContentSize()` |

Not animated: digit entry/erase (the value simply appears), stacked ↔ side-by-side layout switches on rotate/resize (the system handles configuration changes), and puzzle completion (the dialog's own default appearance is enough).

**Performance** (from the official guidance): animate colors in the draw phase (`drawBehind`), use `graphicsLayer {}` / lambda-based modifiers for anything else, and read animated values as late as possible so animations don't recompose the board.

**Accessibility.** Compose animations follow the system animation-scale setting, so with **Settings → Accessibility → Remove animations** everything snaps instantly. Using only standard APIs gives us this for free. Never bypass it (e.g. with manually driven frame loops). The release accessibility pass includes a run with animations removed.

### 7.7 Nice-to-haves (post-v1)

Techniques help screen (explained with mini boards), sound effects, shareable puzzle code (`spec-difficulty-seed-version`) and import of a puzzle by code, a "reveal solution" option.

---

## 8. Theming (goal #2)

### 8.1 Model

```kotlin
@Immutable
data class SudokuColors(          // board-specific tokens
    val boardBackground: Color,
    val gridLineThin: Color,
    val gridLineThick: Color,
    val givenText: Color,
    val userText: Color,
    val noteText: Color,
    val selectedCell: Color,
    val peerCell: Color,
    val sameDigitCell: Color,
    val errorText: Color,
    val errorCell: Color,
    val hintCell: Color,
    val variantRegion: Color,     // diagonal / windoku tint
)

val LocalSudokuColors = staticCompositionLocalOf<SudokuColors> { error("No SudokuColors") }

/** Default derivation: any ColorScheme (incl. dynamic) gives good board colors. */
fun ColorScheme.toSudokuColors() = SudokuColors(
    boardBackground = surface,
    gridLineThin    = outlineVariant,
    gridLineThick   = onSurface,
    givenText       = onSurface,
    userText        = primary,
    noteText        = onSurfaceVariant,
    selectedCell    = primaryContainer,
    peerCell        = surfaceContainerHighest,
    sameDigitCell   = secondaryContainer,
    errorText       = error,
    errorCell       = errorContainer,
    hintCell        = tertiaryContainer,
    variantRegion   = surfaceContainerHigh,
)

data class Palette(
    val id: String,
    @StringRes val name: Int,
    val light: ColorScheme,
    val dark: ColorScheme,
    val lightBoard: (ColorScheme) -> SudokuColors = { it.toSudokuColors() },   // override only if needed
    val darkBoard:  (ColorScheme) -> SudokuColors = { it.toSudokuColors() },
)
```

### 8.2 Palettes (all in `ui/theme/Palettes.kt`)

`Classic` (neutral blue), `Ocean`, `Forest`, `Sunset`, `Paper` (sepia, eye-friendly), `Lavender`, `High Contrast` (WCAG AAA text, distinct error color), plus the **Dynamic** pseudo-palette (Android 12+: `dynamicLight/DarkColorScheme`) and an optional **Pure black** dark variant (surface = `Color.Black`).

**To add a palette:** add one `Palette(...)` entry to `Palettes.all`. Build the `lightColorScheme(...)`/`darkColorScheme(...)` from the Material Theme Builder export (paste the Kotlin), and the board colors follow automatically. That's the "change the palette easily" requirement.

### 8.3 Theme wiring

```kotlin
@Composable
fun SudokuTheme(settings: ThemeSettings, content: @Composable () -> Unit) {
    val dark = when (settings.mode) { System -> isSystemInDarkTheme(); Light -> false; Dark -> true }
    val scheme = when {
        settings.dynamic && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> Palettes.byId(settings.paletteId).let { if (dark) it.dark else it.light }
    }.let { if (dark && settings.pureBlack) it.copy(surface = Color.Black, background = Color.Black) else it }
    val board = /* palette.lightBoard/darkBoard(scheme) */
    CompositionLocalProvider(LocalSudokuColors provides board) {
        MaterialTheme(colorScheme = scheme, typography = Typography, shapes = Shapes, content = content)
    }
}
```

- The theme reads settings via the `MainActivity`-level `SettingsViewModel`/repository, so a change applies app-wide instantly.
- **No theme flash on startup:** DataStore loads asynchronously, so `MainActivity` keeps the splash screen up (`installSplashScreen().setKeepOnScreenCondition { settings.value == null }`) until the first `UserSettings` value has arrived. Only then is the themed content drawn.
- Palette switches animate the board tokens with `animateColorAsState` as described in §7.6.
- Status/navigation bar icon contrast follows `dark` (`enableEdgeToEdge(SystemBarStyle.auto(...))`).
- The splash screen uses a neutral background that works with every palette.
- Typography: a clear, friendly, tabular-figure font for digits (e.g. *Inter* or *Nunito*, bundled). Digits must be equal width.

---

## 9. Adaptive / tablet layout (goal #5)

### 9.1 Decision logic

```kotlin
val info = currentWindowAdaptiveInfo()
BoxWithConstraints {
    val sideBySide = maxWidth >= 600.dp && maxWidth > maxHeight * 1.15f     // tablets, landscape, desktop windows
    val tabletop = info.windowPosture.isTabletop                              // half-folded foldable
    when {
        tabletop   -> TabletopGameLayout(...)   // board in top half, controls in bottom half
        sideBySide -> SideBySideGameLayout(...)
        else       -> StackedGameLayout(...)
    }
}
```

Use the **actual available aspect ratio**, not just the size class. This correctly handles phone landscape, split-screen and free-form windows.

### 9.2 Layouts

| Layout | Used for | Arrangement |
|--------|----------|-------------|
| **Stacked** | phone portrait, narrow split-screen | top bar → board (full width, square) → toolbar → number pad (1 row) |
| **Side-by-side** | tablet (either orientation when wide), phone landscape, desktop | board on the left (height-bound square, centered), on the right a panel (max 420 dp): stats row (time, mistakes, difficulty), toolbar, **number pad as a grid**, hint panel |
| **Tabletop** | foldable half-open | board above the hinge, controls below |
| Tablet portrait (tall, not wide) | uses Stacked, but caps the board at ~ 640 dp and makes the pad/tools larger with more spacing |

Other screens: Home becomes two columns on Expanded+ (Continue on the left, New game config on the right). Settings/Stats content is width-capped (`widthIn(max = 640.dp)`) and centered. The optional Nav3 list-detail scene for Stats/Settings applies on ≥ 600 dp.

Also: support all orientations (no `screenOrientation` lock), `resizeableActivity` default, and test in the **Resizable emulator**, a Pixel Tablet, and a foldable. Touch targets ≥ 48 dp. The board keeps its state across rotation (state lives in the ViewModel and Room).

---

## 10. Build configuration

### 10.1 `gradle/libs.versions.toml` (target)

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.0"
ksp = "2.3.12"
hilt = "2.60.1"
androidxHilt = "1.4.0"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
coreKtx = "1.19.1"
coreSplashscreen = "1.2.0"
lifecycle = "2.11.0"
navigation3 = "1.2.0"
material3Adaptive = "1.3.0"
datastore = "1.2.1"
room = "2.8.5"
coroutines = "1.11.0"
serialization = "1.11.0"
junit = "4.13.2"
junitExt = "1.3.0"
espresso = "3.7.0"
turbine = "1.2.1"          # verify latest at setup time

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "coreSplashscreen" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }

androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }

androidx-adaptive = { module = "androidx.compose.material3.adaptive:adaptive", version.ref = "material3Adaptive" }
androidx-adaptive-layout = { module = "androidx.compose.material3.adaptive:adaptive-layout", version.ref = "material3Adaptive" }
androidx-adaptive-navigation3 = { module = "androidx.compose.material3.adaptive:adaptive-navigation3", version.ref = "material3Adaptive" }

androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }

androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-lifecycle-viewmodel-compose = { module = "androidx.hilt:hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }

androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

junit = { module = "junit:junit", version.ref = "junit" }
androidx-junit = { module = "androidx.test.ext:junit", version.ref = "junitExt" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

### 10.2 Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
```

`settings.gradle.kts`: `include(":app", ":core")`.

### 10.3 `core/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.kotlinx.coroutines.core)      // ensureActive() in generator
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
tasks.test { useJUnitPlatform() }
```

### 10.4 `app/build.gradle.kts` (key parts)

```kotlin
plugins {
    alias(libs.plugins.android.application)   // built-in Kotlin, no kotlin.android plugin!
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.ariadna.sudoku"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.ariadna.sudoku"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // or HiltTestRunner
    }
    buildTypes {
        release {
            optimization { enable = true }      // R8 on for release; engine is reflection-free
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

`gradle.properties`: keep `org.gradle.configuration-cache=true`, enable `org.gradle.parallel=true` and `org.gradle.caching=true`, and raise `-Xmx` to 4g.

---

## 11. Testing strategy

| Layer | What | Tools |
|-------|------|-------|
| `:core` model | houses/peers for each spec (9×9 classic: 27 houses, 20 peers; 6×6: 18 houses, 12 peers), candidate bit ops | kotlin-test |
| `:core` solver | known puzzles solve correctly; multi-solution puzzles report ≥ 2; empty grid is solvable | kotlin-test |
| `:core` generator (**property tests**) | for many seeds × specs × difficulties: givens ⊂ solution, solution valid, **exactly one solution**, grader tier within the profile range, clue count within profile, and re-running `attempt()` with the recorded (successful) seed produces the identical puzzle | kotlin-test, parameterized loops |
| `:core` techniques | one hand-crafted fixture per technique (fires exactly as expected, no false eliminations). Cross-check: after any logical step, the solution still satisfies all remaining candidates | kotlin-test |
| `:core` hints | order FixMistake → BadNote → Step → Reveal fallback; a stuck (Expert) board still yields a hint | kotlin-test |
| `:core` reducer | input/erase/notes/auto-remove/undo/redo restore notes, mistake counting only in Immediate (undo doesn't subtract), Check + ClearCheck, solved detection, locked-digit mode, `SelectCell`/`ToggleNotesMode` leave `session` unchanged | kotlin-test |
| Benchmarks | generation time per (spec, difficulty), p50/p95 logged in a JVM test (not asserted strictly) | kotlin-test |
| ViewModel | action → state; autosave fires after a move but **not** after selection changes; timer runs only for timed games and pauses/resumes with the lifecycle; hint levels; starting a new game abandons the previous one | JUnit4 + coroutines-test + Turbine, fake repositories |
| Data | `pool.take` removes the row and creates the game atomically; the same pool puzzle is never returned twice | Room in-memory DB (instrumented or Robolectric) |
| UI | selecting a cell highlights row/col/box, a wrong digit shows the error state (via semantics), layout switches at the width threshold | Compose UI test (`createComposeRule`, `DeviceConfigurationOverride` for sizes) |
| Visual | previews for every component × light/dark × palettes × phone/tablet (`@PreviewScreenSizes`, `@PreviewLightDark`) | Android Studio previews; optionally Compose Preview Screenshot Testing |

---

## 12. Implementation roadmap

Each milestone ends in a runnable, tested state. **v1 = milestones 1–13.** Everything after that is 1.x.

**v1**

1. **Build setup.** Update the version catalog, add the `:core` module, Hilt application class, JDK 17, and replace the template Greeting with an empty `SudokuRoot` + Nav3 with Home/Game placeholders.
2. **Engine core.** `GridSpec`, `Houses`, `Candidates`, `Grid` encoding, brute-force solver + tests (4×4, 6×6, 9×9).
3. **Generator v1.** Deterministic `attempt()` + seed-chained retry loop, digger with uniqueness + clue-range profiles. Property tests.
4. **Logical solver T1–T3 + grader.** Difficulty ladder for Easy/Medium, hooked into the digger (ceiling + floor checks).
5. **Game state reducer.** `GameSession` / `InputState`, actions, notes, undo/redo, mistake modes, Check, solved detection. Tests.
6. **Game screen (phone).** Board, cells, highlights, number pad, toolbar, `GameViewModel`, `GameTimer` for timed games, completion dialog.
7. **Persistence.** Room (game, pool), autosave/continue, one-in-progress rule, DataStore settings, Settings screen (mistake mode + highlight toggles), New-game sheet with timer switch and remembered choices.
8. **Theming.** `SudokuColors`, palettes, theme mode, dynamic color, palette picker with live preview, splash held until settings load.
9. **Adaptive layouts.** Side-by-side/tabletop game layouts, Home two-column, keyboard input. Test on tablet/foldable/resizable emulators.
10. **Hints.** HintEngine (mistake → bad note → logical step → reveal fallback), off-main-thread computation, 3-level reveal UI, explanations.
11. **Techniques T4 + pool.** Hard tier and v1 Expert ("beyond T4"), puzzle pool with background refill, generation progress/cancel.
12. **Statistics.** Simple stats screen (solved count, best/average time for timed games).
13. **Release prep.** R8, baseline profile (optional), accessibility pass (TalkBack walk-through, contrast, "Remove animations" on), app icon, Play listing.

**Post-v1 (1.x)**

14. **Techniques T5/T6.** Proper Expert, plus Master.
15. **Variants & sizes.** Diagonal & Windoku (houses + generator constraints + board tinting), 8×8/12×12/16×16 sizes.
16. **Nice-to-haves** from §7.7.

---

## 13. Key risks & mitigations

| Risk | Mitigation |
|------|------------|
| Expert/Master generation is slow or fails to hit the tier | Puzzle pool + background refill; attempt budget with the best-so-far fallback (closest score); on-demand generation with cancel. |
| Grader disagrees with human difficulty perception | Tier + score model, tunable `DifficultyProfile` tables, play-testing; log the internal score in debug builds so the curve can be tuned. |
| Bug in a technique produces wrong eliminations → wrong hints | Every technique is tested with fixtures + the "solution still consistent with candidates" invariant, run over thousands of generated puzzles in the `:core` test suite (`./gradlew :core:test`). |
| Too many DB writes / battery use | Only `GameSession` changes are saved (debounced); timer and selection never trigger writes (§5.1). |
| 16×16 performance | Pool-only for 16×16; consider a DLX solver if the brute-force uniqueness check is too slow. |
| AGP 9 plugin incompatibilities | Versions above are known to work together (Hilt ≥ 2.59, KSP ≥ 2.3.10). Never add `kotlin-android`. |
| Array equality in data classes | Custom `equals/hashCode` or wrapper types for `IntArray` fields; UI models use `List`/value types. |

---

## 14. References

- Navigation 3 release notes / Scenes / list-detail recipe: https://developer.android.com/jetpack/androidx/releases/navigation3, https://developer.android.com/guide/navigation/navigation-3/scenes, https://developer.android.com/guide/navigation/navigation-3/recipes/scenes-listdetail, https://github.com/android/nav3-recipes
- Lifecycle (incl. `lifecycle-viewmodel-navigation3`): https://developer.android.com/jetpack/androidx/releases/lifecycle
- Material 3 Adaptive: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- Animation: Nav3 destination transitions https://developer.android.com/guide/navigation/navigation-3/animate-destinations; Compose animation quick guide https://developer.android.com/develop/ui/compose/animation/quick-guide; predictive back https://developer.android.com/develop/ui/compose/system/predictive-back-setup and https://developer.android.com/about/versions/16/behavior-changes-16; `MotionScheme` https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- Compose BOM mapping: https://developer.android.com/develop/ui/compose/bom/bom-mapping
- Hilt (androidx) / Dagger releases: https://developer.android.com/jetpack/androidx/releases/hilt, https://github.com/google/dagger/releases
- Room: https://developer.android.com/jetpack/androidx/releases/room
- AndroidX stable versions: https://developer.android.com/jetpack/androidx/versions/stable-channel
- AGP 9 migration / version matrix: https://github.com/kotlin/kotlin-agent-skills/blob/main/skills/kotlin-tooling-agp9-migration/references/VERSION-MATRIX.md
- Kotlin 2.4.0: https://blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/
- Sudoku generation & difficulty: Daniel Beer, "Generating difficult Sudoku puzzles quickly" https://dlbeer.co.nz/articles/sudoku.html; sudokuwiki.org (strategy catalogue); HoDoKu technique ratings; Sudoku Explainer (SE) rating scale.
