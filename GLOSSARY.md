# Evolve Android

Kotlin/Compose native port of the incremental simulation game Evolve, maintaining mathematical parity with the upstream web reference while providing an idiomatic mobile experience.

## Language

**Reference Model**:
The upstream JavaScript codebase in `refs/`, used as the authoritative specification for simulation formulas, game rules, and text assets.
_Avoid_: Upstream code, core submodule

**Domain Engine**:
The pure Kotlin simulation engine executing game logic, tick updates, and state calculations independently of Android UI.
_Avoid_: Backend, game loop script

**Evolution Era**:
The initial phase of gameplay spanning primordial soup (RNA/DNA/Organelles), species speciation, traits, and sentience awakening.
_Avoid_: Stage 1, Prehistoric phase

**Civilization Era**:
The second phase of gameplay spanning early settlement (Stone Age), basic resource gathering, worker job allocation, and initial technology research.
_Avoid_: Stage 2, City phase

**Tick**:
The periodic discrete time-step where resource deltas, research progress, and simulation rules are evaluated.
_Avoid_: Frame, interval, update loop

**Save Bridge**:
The persistence mechanism providing strongly-typed local storage and importing legacy web Evolve export strings.
_Avoid_: LocalStorage, save parser
