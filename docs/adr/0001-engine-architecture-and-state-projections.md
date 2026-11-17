# Pure Kotlin Domain Engine with Projected State Flows

Context: Evolve is an incremental strategy simulation game requiring high-frequency discrete tick calculations (4Hz/250ms), offline progress simulation, and reactive Jetpack Compose UI rendering without dropped frames or GC stalls.

We decided to build `:core:model` and `:core:engine` as pure Kotlin JVM modules completely decoupled from the Android SDK. The engine maintains an immutable `GameState` as the single source of truth, updated atomically through a pure function `step(dt)` and synchronous `dispatch(GameCommand)` invocations. Rather than broadcasting the monolithic state every 250ms, the engine exposes granular derived `StateFlow` projections (`resourceState`, `evolutionState`, `cityState`) and a one-shot `SharedFlow<GameEvent>` to the UI.

This architecture enables millions of ticks to be simulated in milliseconds for fast unit testing and offline catch-up, eliminates state tearing across interdependent domain slices, and prevents excessive Compose UI recomposition overhead.
