# Goalocus — notes for coding agents

Read `README.md` for what the app is and how to build it. The specs live in `docs/`:
`01-product-spec.md` (what and why), `02-architecture.md` (how), `03-app-access-policy.md`
(which apps are never blocked), `content-format.md` (how lessons are written).

## Ground rules

- Follow SOLID. Prefer small interfaces in `core/domain` with implementations in the
  layer that owns the dependency. New behaviour should be a new class plus one binding,
  not an edit to a central `when` (see `ProbingCapabilityRegistry` + `DeviceModule`).
- Check every change for efficiency and for how easily the next feature can extend it.
- Never block an essential app (calls, SMS, UPI, banking, authenticators, maps, etc.).
  The list and its tiers are in `docs/03-app-access-policy.md`; it is a safety requirement.

## Module rules

| Module | May depend on | Must not |
|---|---|---|
| `core:domain` | Kotlin stdlib; coroutines core once an interface needs `Flow` | Android, serialization |
| `core:content` | `core:domain`, coroutines, serialization | Android |
| `core:designsystem` | Compose | Feature logic |
| `platform:*` | `core:domain`, AndroidX | Other `platform` modules, UI |
| `app` | everything | Business rules (put them in `core`) |

- All build config goes in `build-logic/` convention plugins and `gradle/libs.versions.toml`.
  Module build files only list plugins, a namespace and dependencies.
- AGP 9 has Kotlin built in: never apply `org.jetbrains.kotlin.android`.
- AGP, Kotlin, KSP, Hilt and Gradle versions move together (they're pinned to a set
  that's known to build). Don't bump one alone.
- Device features go through `CapabilityRegistry`; no SDK checks in feature code.
  A capability without a probe reports `NotBuiltYet`.

## Content

- Lessons are text in `content/`, compiled by `tools/contentc` into
  `content/content-pack.json` in the APK's assets. Errors there fail the Gradle build.
- Topic ids never change once shipped: progress is keyed by them.
- Lessons are original writing. Link sources; never copy them.
- Keep content tech-agnostic: no employer, team or personal details.

## Commands

```bash
./gradlew spotlessApply test :app:assembleDebug :app:lintDebug   # what CI runs (plus spotlessCheck)
cd tools/contentc && python3 -m pytest tests -q
cd tools/contentc && python3 -m contentc validate ../../content
```

CI runs Kotlin compilation with `-PwarningsAsErrors=true`, so keep code warning-free.
