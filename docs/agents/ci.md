# CI & Verification: GitHub Actions

All build verification, linting, unit testing, and artifact compilation are handled remotely via GitHub Actions CI rather than running heavy local compilation.

## Workflow

The canonical pipeline is defined in `.github/workflows/ci.yml`.

### Triggers
- Push to `master` and feature/prototype branches (`prototype/*`, `feature/*`).
- Pull requests targeting `master`.
- Manual workflow dispatch (`workflow_dispatch`).

### Jobs
1. **Lint & Test**:
   - Runs unit tests across pure JVM modules (`:core:model`, `:core:engine`) and Android libraries (`:core:ui`, `:feature:evolution`).
   - Runs `check` / `test` tasks.
2. **Build Debug APK**:
   - Runs `./gradlew assembleDebug --stacktrace`.
   - Uploads debug APK as an artifact.

### Agent Conventions
- **No local full build / assembleDebug**: Agents should not run heavy local Android compilation or emulator tests.
- **Push or trigger CI**: After creating or modifying code/modules, commit and push to the remote branch (or run `gh workflow run ci.yml --ref <branch>`).
- **Monitor status with `gh`**:
  - `gh run list --workflow=ci.yml`
  - `gh run watch <run-id>`
  - `gh run view <run-id> --log-failed` (to diagnose errors if any step fails)
