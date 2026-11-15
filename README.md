# Goalocus

A focus app for people preparing for technical roles. It blocks distracting apps (like Regain), then gives you one short, visual lesson a day at a fixed time, followed by a quiz built from real interview questions.

The current journey targets a hybrid **AI Engineer** role. It braids three strands: systems (OS, networking, cloud, system design), algorithms (DSA), and intelligence (ML, LLMs, RAG, agents, MLOps).

> Personal sideloaded build for a Galaxy S26 Ultra (Android 17). Play Store comes later.

## Status

Milestone **M0: skeleton**.

| Area | State |
|---|---|
| Content format, checker (`tools/contentc`) and first 7 topics | Done |
| Content pack compiled into the APK at build time | Done |
| Topic graph (prerequisites, "up next") | Done |
| Device capability checks (notifications, alarms, focus modes, Live Updates, NFC) | Done |
| Home screen | Done |
| M1 · Blocking core: app tiers, rule chain, accessibility engine, Study mode | 19–24 Nov |
| M2 · Learning core: 7:30 AM alarm, NFC start, Now Bar timer, lessons, quiz, FSRS reviews | 25 Nov – 1 Dec |
| M3 · Urge interrupt: unlock quiz, escalation, emergency unlock, night/morning lock, onboarding | 2–6 Dec |
| v1: AI tutor, sleep-aware sessions, widget, weekly report | 7 Dec – 10 Jan |

## Build and install

You need:

- **Android Studio** (latest stable). Its bundled JDK works.
- **Python 3.11+** on your `PATH`. The build runs the content checker, which uses only the standard library.

Steps:

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on your phone, or run `./gradlew :app:installDebug` with the phone connected over USB.

If Python isn't called `python3` on your machine (common on Windows), add one of these to `~/.gradle/gradle.properties`:

```properties
goalocus.python=py
# or
goalocus.python=python
```

### Installing on a phone with Auto Blocker

Samsung **Auto Blocker** blocks installs from outside the Galaxy Store and Play Store, including USB installs.

1. Turn Auto Blocker **off** (Settings → Security and privacy → Auto Blocker).
2. Install the app.
3. Turn Auto Blocker back **on**.

Keep "Maximum restrictions" off: it disables device-admin apps, which strict mode needs.

Because the app is sideloaded, Android hides its Accessibility switch until you allow it: App info → ⋮ → **Allow restricted settings**. (Needed from M1, when the blocking engine arrives.)

Every CI run also uploads a debug APK as a build artifact.

## Everyday commands

```bash
./gradlew test                       # all unit tests
./gradlew spotlessApply              # format Kotlin (ktlint)
./gradlew :app:assembleDebug         # builds the APK; also compiles the content pack

cd tools/contentc
python3 -m pytest tests -q           # content checker tests
python3 -m contentc validate ../../content            # check all content
python3 -m contentc validate --strict ../../content   # treat warnings as errors
python3 -m contentc stats ../../content               # coverage per phase
```

## Project layout

```
app/                  Android app: Hilt wiring, MainActivity, screens
core/domain/          Pure Kotlin models and interfaces. No Android.
core/content/         Pure Kotlin: content pack parser, topic graph, use cases
core/designsystem/    Compose theme and shared UI
platform/device/      Android adapters: capability probes, Settings screens
build-logic/          Gradle convention plugins (one place for build config)
content/              Lessons and quizzes as text files (see docs/content-format.md)
tools/contentc/       Python checker/compiler for content/
docs/                 Product spec, architecture, app access policy, content format
```

Read [`docs/02-architecture.md`](docs/02-architecture.md) before changing module boundaries, and [`docs/content-format.md`](docs/content-format.md) before writing a lesson.
