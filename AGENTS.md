## Agent skills

### Issue tracker

GitHub Issues using `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical 5-role triage vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context (`GLOSSARY.md` + `docs/adr/` at the root). See `docs/agents/domain.md`.

### CI & Verification

All compilation, testing, and artifact verification run remotely via GitHub Actions CI. Do not run heavy local compilation. See `docs/agents/ci.md`.

