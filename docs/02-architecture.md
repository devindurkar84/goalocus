# Goalocus — Architecture v0.4

*Status: M0 built · Updated 15 Nov 2026 · Owner: Dev*
*Related: [Product spec](01-product-spec.md) · [App Access Policy](03-app-access-policy.md) · [Content format](content-format.md)*

**v0.4 changes:** matches the M0 code. Topic files use TOML front matter between `+++` fences (§9); the capability registry is built from one probe per capability bound into a Hilt map (§5); the content pack is compiled by a Gradle convention plugin (§9); code lives at github.com/devindurkar84/goalocus.

---

## 1. Principles

- **Clean Architecture:** domain (pure Kotlin) ← data / platform adapters ← UI. Dependencies point inward.
- **SOLID by construction:**

| Principle | How it shows up |
|---|---|
| **S**ingle responsibility | Accessibility service only *adapts* events; rules *decide*; enforcer *acts*. AI providers only *generate*; the router *chooses*; use cases *orchestrate* |
| **O**pen–closed | New blocked screen = new `SurfaceDetector`; new question type = new renderer + grader; new device capability = new `CapabilityProbe` + one binding; new AI provider = new `AiProvider` + a routing-table entry — no edits to existing classes |
| **L**iskov substitution | Accessibility and usage-stats signal sources are interchangeable; every device capability has a NoOp twin; every `AiProvider` honours the same streaming/availability contract |
| **I**nterface segregation | Small interfaces (`AppClassifier`, `AccessRule`, `FocusModeController`, `SleepSignalSource`, `AiProvider`) instead of god managers |
| **D**ependency inversion | Domain and features depend on interfaces; Hilt binds Android / Samsung / vendor implementations at the edge |

- **Patterns used deliberately:** Strategy (engines, policies, providers, capability probes), Chain of Responsibility (access rules, AI fallback), Composite (classifiers), Null Object (missing capabilities), State machine (study session), Repository, Adapter, Observer (Flow).
- **Capability-gated, never version-branched in features:** features never call `Build.VERSION`; the capability layer decides once.
- **Provider-agnostic AI:** features call task-level use cases; they never know which model answered.
- **Offline-first:** everything except Claude-routed AI works without network.
- **Battery-aware:** event filtering, early exits, no disk I/O on the accessibility thread.
- **Adaptive UI from day one:** window size classes, so DeX / external displays and landscape just work (Android 17 removes the resizability opt-out on large screens for apps targeting 37).
- **Testable:** time, Android services, network and AI providers are behind interfaces; domain modules have zero Android imports.

## 2. Stack

| Concern | Choice |
|---|---|
| SDK levels | compileSdk / targetSdk 37 (Android 17); minSdk 31 with capability gating, so a later Play build isn't locked to Android 17 |
| Build | Gradle 9.7 with configuration cache, AGP 9.3 (Kotlin built in), Kotlin 2.3, KSP, version catalog, convention plugins in `build-logic/` |
| Language / UI | Kotlin 2.x, Jetpack Compose, Material 3, window size classes |
| DI | Hilt |
| Async | Coroutines + Flow |
| Local data | Room (progress, reviews, events), DataStore (settings) |
| Serialization | kotlinx.serialization |
| Scheduling | `AlarmManager` exact alarm for the 7:30 AM slot; WorkManager for daily / weekly jobs |
| Live status | Live Update notifications (promoted ongoing; ProgressStyle / Android 17 MetricStyle + semantic colours) → Samsung Now Bar |
| Focus modes | `AutomaticZenRule` + `ZenDeviceEffects` (grayscale, dim wallpaper, dark theme) via Do Not Disturb access — "Study" and "Wind-down" rules |
| Widgets | Jetpack Glance (small bitmaps — Android 17 enforces RemoteViews memory limits) |
| Sensors & input | NFC NDEF intent filter; stylus pressure/hover via Compose pointer input |
| Health | Health Connect client (sleep sessions, read-only; Garmin Connect as preferred data origin) |
| AI — cloud | Claude Messages API: Anthropic Java SDK if it runs cleanly on Android, else a thin OkHttp + SSE client |
| AI — on-device | ML Kit GenAI APIs on AICore (Gemini Nano): Prompt, Proofreading, Speech Recognition |
| Diagrams | Mermaid in an offline WebView (v1) → native Compose Canvas renderer (v2) |
| Secrets | Android Keystore, StrongBox-backed when available, with Tink |
| Testing | JUnit, kotlin-test, kotlinx-coroutines-test, Turbine, MockK, Robolectric, Compose UI tests, Roborazzi |
| Quality | ktlint (Spotless), Android Lint, warnings as errors in CI, module-dependency checks, GitHub Actions CI |
| Content tooling | Python 3.11+ compiler / validator (`tools/contentc`), standard library only |

## 3. Module graph

```
:app                         (Hilt wiring, navigation, manifest)

:feature:onboarding  :feature:home     :feature:session   :feature:unlock
:feature:journey     :feature:tutor    :feature:insights  :feature:settings
:feature:widget      (Glance)

:core:domain         pure Kotlin — models, use cases, Clock, capability interfaces
:core:blocking       pure Kotlin — AppClassifier, AccessRule chain, UnlockPolicy
:core:learning       pure Kotlin — FSRS, TopicSchedulingPolicy, SessionLengthPolicy, graders
:core:ai             pure Kotlin — AiTask, AiProvider, AiRouter, RoutingTable,
                     EscalationPolicy, UsageMeter, AI use cases
:core:content        pure Kotlin — content-pack parser, TopicGraph, content use cases
:core:data           Room, DataStore, repositories
:core:designsystem   theme, components, haptic cues
:core:visuals        diagram renderers, stylus canvas

:ai:claude           Claude provider (Messages API, prompt caching, vision, streaming)
:ai:ondevice         Gemini Nano provider (ML Kit GenAI) + on-device speech

:platform:accessibility   AccessibilityService adapter, SurfaceDetectors, overlay enforcer
:platform:usagestats      usage-stats signal source + foreground service
:platform:scheduling      alarms, receivers, workers, device-admin receiver
:platform:device          capability probes, Settings deep links, Live Updates, Modes, NFC,
                          Health Connect, haptics, Advanced Protection monitor

build-logic/         convention plugins: android.application / library / compose,
                     jvm.library, hilt, content-pack
```

**Built in M0:** `:app` (home screen lives here until `:feature:home` is split out), `:core:domain`, `:core:content`, `:core:designsystem`, `:platform:device`.

**Dependency rules (enforced in CI):**
- Features depend on `core` only — never on each other, never on `platform` or `ai:*`.
- `:core:domain`, `:core:content`, `:core:blocking`, `:core:learning`, `:core:ai` are pure Kotlin/JVM modules.
- `platform` and `ai:*` modules implement `core` interfaces; only `:app` sees everything.

## 4. Domain model (key types)

| Type | Notes |
|---|---|
| `Topic` | id, strand, module, hard/soft prereqs, bridges, interviewWeight, roleTags, keyPoints, depth layers, est. minutes |
| `ContentPack` | versioned, immutable; topics + questions + references + glossary + phases |
| `TopicProgress` | LOCKED → AVAILABLE → LEARNED → MASTERED, depth reached |
| `ReviewState` | FSRS fields: stability, difficulty, due, reps, lapses |
| `Question` | sealed: SingleChoice, MultiChoice, OrderSteps, SpotTheBug, ComplexityPick, ScenarioTradeoff |
| `StudySession` | state machine (below) |
| `ReadinessSignals` | last night's sleep (nullable), recent "how did it feel" ratings, misses |
| `DayWindows` | study slot, morning-lock end, free-window grant, wind-down, bedtime — computed per day |
| `ScreenSignal` / `Surface` | what's on screen: package + surface type |
| `AppTier` | NEVER_BLOCK, ESSENTIAL, DUAL_USE, DISTRACTION |
| `AccessDecision`, `UnlockPlan`, `UnlockGrant`, `UrgeEvent` | blocking pipeline types |
| `AiRequest`, `AiResult`, `Usage` | AI layer types (§5) |
| `Capability`, `CapabilityStatus`, `UserAction` | device capability state (§5) |

**Study session state machine** (sealed states + reducer — invalid transitions can't compile):

```
Idle → Prompted → Lesson → Quiz → Reviews → Reflection → Completed
          └──────────────┴──────┴─────────┴──→ Paused / Abandoned
```

Entering `Lesson` asks `FocusModeController` to enter Study mode and `LiveActivityPresenter` to show the timer; leaving the session reverses both. `Completed` ends the morning lock and grants the free window. Side effects live in a coordinator, not in the reducer.

## 5. Extension points (interface sketch)

```kotlin
// ---------- :core:blocking (pure Kotlin) ----------
data class ScreenSignal(val pkg: String, val surface: Surface, val at: Instant)

sealed interface Surface {
    data object WholeApp : Surface
    data object ShortVideo : Surface            // Shorts, Reels
    data object Feed : Surface                  // YouTube home, LinkedIn feed
    data object StatusUpdates : Surface         // WhatsApp / Telegram updates
    data class WebDomain(val host: String) : Surface
    data object GuardedSetting : Surface        // Goalocus app info, a11y toggle, device admin
}

enum class AppTier { NEVER_BLOCK, ESSENTIAL, DUAL_USE, DISTRACTION }

fun interface AppClassifier { fun classify(pkg: String): AppTier? }   // null = not decided here

class CompositeAppClassifier(private val chain: List<AppClassifier>) : AppClassifier {
    override fun classify(pkg: String) = chain.firstNotNullOfOrNull { it.classify(pkg) }
}

fun interface AccessRule { fun evaluate(ctx: AccessContext): AccessDecision? }  // null = pass on

sealed interface AccessDecision {
    data object Allow : AccessDecision
    data object Block : AccessDecision                         // no hatch
    data class Interrupt(val plan: UnlockPlan) : AccessDecision
    data object DelayedDisableFlow : AccessDecision
}

interface UnlockPolicy { fun planFor(ctx: UnlockContext): UnlockPlan }      // escalation
interface ScreenSignalSource { val signals: Flow<ScreenSignal> }            // a11y or usage-stats
interface BlockEnforcer { suspend fun apply(decision: AccessDecision, signal: ScreenSignal) }

// ---------- :platform:accessibility ----------
interface SurfaceDetector {                       // one per dual-use app, bound via @IntoSet
    val packages: Set<String>
    fun detect(root: AccessibilityNodeInfo): Surface?   // null = unrecognised → fail open
}

// ---------- :core:learning ----------
interface TopicSchedulingPolicy { fun pick(state: JourneyState, date: LocalDate): TopicPick }
interface SessionLengthPolicy {
    fun level(history: SessionHistory, readiness: ReadinessSignals, caps: LadderCaps): SessionLevel
}
interface ReviewScheduler { fun schedule(state: ReviewState, grade: Grade, now: Instant): ReviewState }
interface AnswerGrader<Q : Question> { fun grade(q: Q, answer: Answer): GradeResult }

// ---------- :core:content (built) ----------
fun interface ContentSource { suspend fun load(): ContentPack }   // bundled assets now, remote later
// StreamContentSource (parse on an IO dispatcher) wrapped by CachingContentSource (load once,
// failures not cached). GetJourneyOverview: phase for today + "up next" from TopicGraph.frontier.

// ---------- :core:ai (pure Kotlin) ----------
enum class AiTask {
    DEFINE_TERM, QUIZ_HINT, PROOFREAD, EXPLAIN_BACK_PRECHECK, EXPLAIN_BACK_GRADE,
    TOPIC_QA, MOCK_INTERVIEW, SKETCH_REVIEW, WEEKLY_SUMMARY,
}

enum class Sensitivity { SHAREABLE, DEVICE_ONLY }    // DEVICE_ONLY never reaches a cloud provider

sealed interface AiInput {
    data class Text(val text: String) : AiInput
    data class Chat(val turns: List<Turn>) : AiInput
    class Image(val png: ByteArray, val instruction: String) : AiInput   // not a data class: arrays
}

data class AiRequest(
    val task: AiTask,
    val topic: TopicContext?,                 // lesson text + key points; cached where supported
    val input: AiInput,
    val maxOutputTokens: Int,
    val sensitivity: Sensitivity = Sensitivity.SHAREABLE,
)

sealed interface Availability {
    data object Ready : Availability
    data class Unavailable(val reason: Reason) : Availability
    enum class Reason {
        NOT_SUPPORTED, MODEL_DOWNLOADING, NOT_FOREGROUND, QUOTA_EXCEEDED,
        OFFLINE, NO_API_KEY, BUDGET_EXHAUSTED,
    }
}

interface AiProvider {
    val id: ProviderId                        // CLAUDE_HAIKU, CLAUDE_SONNET, GEMINI_NANO, …
    val traits: ProviderTraits                // onDevice, supportsImages, contextWindow, relative cost
    fun supports(task: AiTask): Boolean
    suspend fun availability(): Availability
    fun generate(request: AiRequest): Flow<AiChunk>   // text deltas; final chunk carries Usage
}

fun interface RoutingTable { fun candidates(task: AiTask): List<ProviderId> }   // config + user overrides
fun interface EscalationPolicy { fun shouldEscalate(task: AiTask, result: AiResult): Boolean }

class AiRouter(
    private val providers: Map<ProviderId, AiProvider>,
    private val routing: RoutingTable,
    private val escalation: EscalationPolicy,
    private val usage: UsageMeter,
) {
    /** Tries candidates in order: skips unsupported/unavailable ones, drops cloud providers for
     *  DEVICE_ONLY requests, escalates to the next candidate when the policy rejects a result,
     *  and records tokens used + tokens saved. */
    suspend fun run(request: AiRequest): AiResult = TODO()

    /** Streaming variant for chat-like tasks; no automatic escalation — the UI offers
     *  "Ask Claude instead". */
    fun stream(request: AiRequest): Flow<AiChunk> = TODO()
}

// ---------- :core:domain — device capabilities (built) ----------
enum class Capability { NOTIFICATIONS, EXACT_ALARMS, FOCUS_MODES, LIVE_UPDATES, NFC_TRIGGER,
    ACCESSIBILITY_ENGINE, USAGE_ACCESS, HEALTH_CONNECT, ON_DEVICE_AI, ADVANCED_PROTECTION_MONITOR }

sealed interface CapabilityStatus {               // Ready, NeedsUserAction(action),
}                                                 // Unsupported(reason), NotBuiltYet

interface CapabilityRegistry { fun status(c: Capability): CapabilityStatus; fun snapshot(): Map<…> }
fun interface CapabilityProbe { fun probe(): CapabilityStatus }        // one per capability
class ProbingCapabilityRegistry(probes: Map<Capability, CapabilityProbe>) : CapabilityRegistry
fun interface UserActionLauncher { fun launch(action: UserAction) }    // opens the right Settings screen

// ---------- :core:domain — device features (implemented in :platform:device) ----------
interface FocusModeController {                   // AutomaticZenRule + ZenDeviceEffects
    suspend fun enter(mode: FocusMode)            // STUDY, WIND_DOWN, GREY_UNLOCK
    suspend fun exit(mode: FocusMode)
}
interface LiveActivityPresenter {                 // Live Update → Now Bar
    fun show(activity: LiveActivity)              // SessionTimer, UnlockCountdown, FreeWindow
    fun clear(id: LiveActivityId)
}
interface SessionTriggerSource { val triggers: Flow<Trigger> }      // NFC tag, alarm, widget
interface SleepSignalSource { suspend fun lastNightSleep(): SleepSummary? }  // null = not synced yet
interface SpeechCapture { fun transcribe(locale: Locale): Flow<SpeechResult> }
interface HapticsPlayer { fun play(cue: HapticCue) }                // Correct, Wrong, UnlockEnding
interface ProtectionModeMonitor { val advancedProtectionOn: StateFlow<Boolean> }
```

**Capability registry (built in M0):** `ProbingCapabilityRegistry` asks one `CapabilityProbe` per capability. Probes live in `:platform:device` and read live system state on every call (the user can change permissions while Goalocus is in the background); they're bound into a Hilt map with `@IntoMap @CapabilityKey(...)`. A capability without a probe reports `NotBuiltYet`, so adding one is a new probe plus one binding. M0 probes: notifications, exact alarms, focus modes (Android 15+, Do Not Disturb access), Live Updates (Android 16+, `canPostPromotedNotifications`), NFC. `SettingsLauncher` maps each `UserAction` to its exact Settings screen and falls back to the app-info page when an OEM build lacks that screen. The home screen re-checks on every resume and shows a "Fix" button per missing capability.

From M1, features get either the real implementation or its **NoOp twin** (Null Object) based on that status. Features always get a working object; a missing capability simply does nothing.

**Question types:** `Question` is sealed in the domain (graders get exhaustive `when`). UI renderers are registered in a Hilt map keyed by question class, so the session screen never changes when a type is added.

## 6. Blocking pipeline

```
AccessibilityEvent ─▶ ScreenSignalSource ─▶ AppClassifier ─▶ AccessRule chain ─▶ BlockEnforcer
   (platform)          (+ SurfaceDetectors)   (cached tiers)     (decision)         (overlay / home)
```

### 6.1 Access rule chain (first non-null decision wins)
1. `EssentialAppRule` — Tier 0 / 1 → Allow
2. `GuardedSettingRule` — strict mode → DelayedDisableFlow
3. `AlwaysBlockedSurfaceRule` — Shorts / Reels → Block
4. `NightLockRule` — from bedtime until today's session completes (or the morning fallback time) → distraction → Block
5. `ActiveGrantRule` — unexpired unlock or emergency unlock → Allow
6. `StudySlotRule` — distraction → Interrupt (study-slot plan)
7. `FreeWindowRule` — active earned window → Allow
8. `GatedHoursRule` — distraction → Interrupt (escalating plan)
9. Default → Allow

Wind-down is not a rule: it is a `FocusMode` (grayscale + quiet) scheduled at 11:30 PM; gated-hours rules keep applying. Adding a rule = new class + one line in the chain's Hilt provider. `DayWindows` is computed once per day (and on settings change), so rules do O(1) time checks.

When an unlock is granted, `LiveActivityPresenter` shows the countdown in the Now Bar and (experimental) `FocusModeController` enters `GREY_UNLOCK` for the grant's duration.

### 6.2 Engine selection
`SignalSourceSelector` picks the active `ScreenSignalSource` at runtime:
- Accessibility source when the service is enabled and Advanced Protection is off.
- Usage-stats source otherwise — `ProtectionModeMonitor` (Android 17 `AdvancedProtectionManager`) triggers the switch and a one-time notice that in-app blocking (Shorts, Status, feeds) is paused.

### 6.3 Performance
- **Early exit:** Tier 0 / 1 packages resolved by an O(1) in-memory map before any detector runs.
- **Event filtering:** window-state changes for most packages; content-change events only for the 4–5 dual-use apps, debounced (~200 ms).
- **Classifier cache:** rebuilt on package install / remove / role change broadcasts, not per event.
- **No I/O on the event thread:** grants and tiers live in memory; Room writes go to a background dispatcher.
- **Overlay:** the urge-interrupt UI is Compose hosted in an accessibility overlay window (`TYPE_ACCESSIBILITY_OVERLAY`); lifecycle and saved-state owners must be wired manually for `ComposeView` outside an Activity. It uses no AI (on-device inference isn't allowed there).
- **Live Updates:** one notification per activity, updated at most once per second; countdowns use the notification chronometer rather than per-second posts where possible.

### 6.4 Resilience (keep-alive)
- **Heartbeat:** if the accessibility service is off, an ongoing notification says "Protection off — tap to fix".
- **Samsung battery management:** onboarding adds Goalocus to *Never sleeping apps* and exempts it from battery optimisation (see dontkillmyapp.com for Samsung specifics).
- **Boot:** alarms re-registered by a boot receiver; the accessibility service is restarted by the system; focus-mode rule state re-synced.
- **Sideload:** turn on "Allow restricted settings" for Goalocus before enabling the accessibility service.
- **Exact alarm:** the study-slot alarm needs the "Alarms & reminders" permission; onboarding requests it.
- **Strict mode:** device-admin receiver adds uninstall friction; disabling it is a guarded setting.

### 6.5 Installing dev builds on the S26 Ultra
- **Auto Blocker** (Samsung) blocks sideloading and commands over USB, and is on by default on newer Galaxy phones — turn it off while installing from Android Studio.
- Its **Maximum restrictions** also disable device-admin apps — strict mode needs that off.
- **Advanced Protection** (Android) is separate from Auto Blocker; Goalocus detects it at runtime (§6.2).

## 7. Permissions & onboarding order

| # | Permission / setting | Used for | Required? |
|---|---|---|---|
| 1 | Notifications (+ promoted / live notifications) | Slot prompt, Now Bar timer & countdowns | Yes |
| 2 | Alarms & reminders (exact alarms) | 7:30 AM slot, wind-down, bedtime | Yes |
| 3 | Allow restricted settings → Accessibility | Blocking engine, in-app surfaces | Yes (personal build) |
| 4 | Usage access | Fallback engine, weekly stats | Yes |
| 5 | Do Not Disturb access | Study / wind-down modes: silence + grayscale | Recommended |
| 6 | Never sleeping apps + battery exemption | Keep-alive on Samsung | Recommended |
| 7 | Device admin | Strict mode uninstall friction | Optional |
| 8 | Health Connect: read sleep (Garmin Connect must share sleep to Health Connect) | Sleep-aware ladder, bedtime check | Optional |
| 9 | Microphone | Spoken explain-back | Optional |
| — | NFC | Tag-to-start (no runtime permission) | Optional |
| — | Claude API key | Cloud AI tasks | Optional (on-device tasks still work) |

Each step explains *why* in one line before the system dialog, and onboarding can be resumed — skipping an optional step just leaves that capability on its NoOp twin. The permissions themselves are declared in the manifest of the module that owns the capability (`:platform:device` for notifications, Live Updates, exact alarms, DND and NFC) and merge into the app.

## 8. Learning engine

- **TopicGraph (built):** the content pack is parsed once into an immutable in-memory graph (adjacency lists, cached topological order via Kahn's algorithm with alphabetical tie-breaks). `frontier(learned)` is O(V + E); `missingPrerequisites(target, learned)` powers gate pull-forwards. A few hundred topics → tiny and fast; no DB round-trips to schedule.
- **TopicSchedulingPolicy:** `BraidedPolicy` (default) and `InterviewSprintPolicy` (from 8 Feb); phase weights and role tags come from config.
- **SessionLengthPolicy:** `AdaptiveLadderPolicy` implements the ladder in the product spec §7, including the sleep rule via `ReadinessSignals`.
- **ReviewScheduler:** FSRS (the open algorithm available in Anki since 23.10), target retention 0.9, validated against reference test vectors.
- **Sleep source:** `HealthConnectSleepSource` reads sleep sessions for the night window (e.g., 6 PM–noon), prefers the Garmin Connect data origin (configurable), and de-duplicates overlapping sessions written by other apps (Samsung Health may mirror the same night). Re-read at session start; `null` if not synced yet → no ladder adjustment.
- **Daily plan:** computed by a worker shortly after midnight and on app open — idempotent, so double runs are harmless.

## 9. Content pipeline

Content lives in the repo, separate from code. The authoring guide is [content-format.md](content-format.md).

```
content/
  phases.toml                   # phase dates + weekly strand mix (6 learning days)
  glossary.toml                 # one-line definitions for S Pen hover / DEFINE_TERM fallback
  topics/<strand>/<module>/<slug>.md
tools/contentc/                 # Python 3.11+, standard library only: validate + compile
tools/aieval/                   # (v1) eval sets per AI task
```

**Topic file:** TOML settings between `+++` fences, then Markdown cards headed `## Title {core|extended|deep}`. Questions live in the TOML (`[guess]`, `[[questions]]`), so quiz data is structured and the prose stays readable.

```toml
+++
id = "sys.net.tcp-handshake"
title = "TCP three-way handshake"
strand = "systems"
module = "networking"
role_tags = ["ai-engineer", "backend"]
hard_prereqs = ["sys.net.tcp-ip-model"]
soft_prereqs = ["sys.os.sockets"]
bridges = ["ai.serving.token-streaming-sse"]
interview_weight = 4
key_points = ["…", "…"]
est_minutes = { core = 6, extended = 4, deep = 10 }

[[references]]
title = "…"
url = "https://…"
kind = "video"
start_at = "3:12"
+++
```

**Why TOML:** Python reads it with the standard library (`tomllib`), so the checker needs no packages; it has real dates for phases; and it avoids YAML's implicit typing surprises.

**Validator** (`contentc validate`; errors fail the Gradle build, warnings are listed, `--strict` fails on warnings too):
- Ids match `prefix.module.slug` with the strand's prefix and the file path; unique.
- Hard prerequisites exist and form a DAG (Kahn's algorithm). Unknown soft prerequisites and bridges are warnings, since they may point at topics not written yet.
- Each topic: a single-choice guess question; ≥ 8 questions with unique ids, valid answers and explanations; ≥ 1 core card with a Mermaid diagram; ≥ 2 https references; 2–5 key points; core ≤ 6 min, with a warning if core reading time exceeds it.
- Phases: Monday-to-Sunday weeks, weekly mix sums to 6, no overlaps.
- Glossary: unique terms, definitions under 200 characters.
- *Planned:* each phase has enough available topics per strand for its weekly mix. `contentc stats` reports coverage today.

**Build integration:** the `goalocus.content-pack` convention plugin registers a cacheable `compileContentPack` task that runs `contentc build` and adds `content/content-pack.json` to the app's assets through the variant API. The pack is deterministic JSON with `schemaVersion` and a `packVersion` (first 12 hex characters of its SHA-256). The app re-checks the invariants it relies on when loading (`ContentPackParser`: schema version, answer indexes, prerequisite graph) and fails loudly on a bad pack. Renamed topics will keep an alias map so progress is never lost.

## 10. AI integration

### 10.1 Providers
| Provider | Where | Strengths | Limits |
|---|---|---|---|
| `ClaudeHaikuProvider` | Cloud | Fast, cheap Q&A | Tokens, network |
| `ClaudeSonnetProvider` | Cloud | Grading, mock interviews, vision | Tokens, network |
| `GeminiNanoProvider` | On-device (ML Kit GenAI / AICore) | Free per call, private, offline | Smaller model; only while Goalocus is in the foreground; per-app quota; quality varies by model version |
| *(future)* Samsung / other | — | Plugs in as another `AiProvider` | — |

### 10.2 Default routing table (config; user-overridable per feature)
| Task | Candidates in order | Sensitivity |
|---|---|---|
| `DEFINE_TERM`, `QUIZ_HINT` | Glossary/authored hint → Nano → Haiku | Shareable |
| `PROOFREAD` | Nano | Shareable |
| `EXPLAIN_BACK_PRECHECK` | Nano → Haiku | Shareable |
| `EXPLAIN_BACK_GRADE` | Sonnet | Shareable |
| `TOPIC_QA` | Haiku (stream) | Shareable |
| `MOCK_INTERVIEW` | Sonnet (stream) | Shareable |
| `SKETCH_REVIEW` | Sonnet (vision) | Shareable |
| `WEEKLY_SUMMARY` | Nano | **Device-only** |

### 10.3 Use cases (what features call)
- `GradeExplanationUseCase` — the cascade: (1) on-device pre-check returns which authored key points are covered (structured output); (2) if coverage is clearly low → return "you missed X, Y" with no cloud call; (3) otherwise → Sonnet grades nuance and accuracy.
- `AskTopicUseCase`, `DefineTermUseCase`, `ReviewSketchUseCase`, `MockInterviewUseCase`, `WeeklySummaryUseCase`.
- Features never reference `AiProvider` or model ids.

### 10.4 Provider details
- **Claude:** prompt layout = system (tutor rules + scope guard) → lesson context (topic + bridges + key points) → conversation, with automatic prompt caching via a top-level `cache_control`. Sketches flattened to PNG and downscaled; prompt includes a rubric (components, data flow, bottlenecks, failure modes, cost).
- **On-device:** availability checked before each call (model downloaded, app in foreground); `BUSY` → exponential backoff then fall through; base model name logged with each result so routes can be re-evaluated after system updates. Speech uses on-device recognition; only the transcript is used downstream.
- **Budget:** `UsageMeter` enforces a daily token cap and records tokens used, tokens saved (on-device answers priced at the cloud rate they replaced) and cost per feature.
- **Keys:** Claude key encrypted with a StrongBox-backed key where available. Play Store build swaps the Claude provider for a proxy-backed one — no other code changes.

### 10.5 Eval harness (routes earned by data)
- `tools/aieval` holds ~30 examples per task (input, lesson context, expected key points / reference answer).
- A debug-build **eval runner** on the phone runs each example through every eligible provider (on-device inference only runs on the device) and exports results.
- Results are scored against a rubric (Claude Sonnet as judge, spot-checked by Dev). A task is routed on-device only if its score is within an agreed margin of Claude's.
- Re-run after One UI / model updates and whenever prompts change.

## 11. Data storage

| Store | Contents |
|---|---|
| Room `topic_progress` | topicId, status, depth reached, learnedAt |
| Room `review_card` | questionId, topicId, FSRS state, due |
| Room `quiz_attempt` | questionId, correct, latency, context (session / unlock / mock) |
| Room `study_session` | date, level, planned vs actual minutes, topicId, feeling, trigger (nfc / alarm / widget) |
| Room `urge_event` | package, surface, reason, outcome (unlocked / failed / abandoned / emergency) |
| Room `unlock_grant` | package, surface, kind (quiz / emergency / free window), expiresAt |
| Room `app_rule` | package, tier, source (role / ime / curated / category / user) |
| Room `ai_call` | task, provider, tokens in/out, cached tokens, latency, escalated?, tokens saved |
| Room `tutor_message` | topicId, role, text |
| Room `sketch` | topicId, image path, review summary |
| DataStore | slot time, ladder caps, strict mode, free window, wind-down & bedtime, morning fallback, routing overrides, registered NFC tag IDs, capability toggles |
| Assets | content pack (read-only) |

Sleep data is read on demand from Health Connect and never stored (only a derived "short night" flag per day). Room schema exported and migrations tested; progress keyed by stable topic IDs.

## 12. Security & privacy

- The accessibility service reads only what detectors need, on-device; logs store package + surface type, never screen text.
- Only `SHAREABLE` AI requests can leave the phone; `DEVICE_ONLY` requests (anything touching app usage or sleep) are filtered out of cloud candidates by the router.
- API key never in source control or the APK; hardware-backed where available. Keystores and `secrets.properties` are git-ignored.
- `android:allowBackup="false"`; backups would otherwise carry progress and, later, the encrypted key.

## 13. Testing strategy

| Layer | What | How |
|---|---|---|
| Domain | Rule chain (incl. night/morning lock), unlock escalation, ladder promote/demote (incl. sleep rule), scheduler, FSRS, `DayWindows` across midnight | Fast JVM unit tests with a fake `Clock` |
| Scheduler properties | Over 1,000 random journeys: never violates hard prereqs; weekly strand mix within ±1; never 3 same-strand days in a row | Property-based tests |
| AI router | Fallback order, skip unavailable, escalation, DEVICE_ONLY never reaches cloud, usage accounting | Fake providers with scripted availability/results |
| AI quality | Per-task scores per provider | Eval runner (§10.5) |
| Content | Validator rules, DAG check, budget check, key points | `contentc` tests + CI on every content change |
| Detectors | YouTube Shorts, WhatsApp Status, LinkedIn feed recognition | Recorded node-tree fixtures per app version |
| Capabilities | Registry behaviour (built); each real implementation vs its NoOp twin honours the same contract | JVM tests; shared contract tests run against both |
| ViewModels | State on load, failure + retry, capability refresh | JVM tests with `Dispatchers.setMain` |
| Android | Receivers, workers, alarms | Robolectric |
| UI | Session flow, urge interrupt, settings, phone + landscape + large window | Compose UI tests + Roborazzi screenshots |
| On device (S26 Ultra, One UI 9) | Now Bar display, Study / wind-down grayscale, NFC start, S Pen hover, Garmin sleep read, reboot, battery saver, app updates, Advanced Protection toggle | Manual checklist before each install |

**CI** (GitHub Actions): content job (contentc tests, validate, stats) → Android job (Spotless check, unit tests with warnings as errors, debug APK, Android Lint; APK and lint report uploaded). Weekly link check on lesson references. Dependabot groups toolchain and AndroidX updates.

## 14. MVP build milestones (16 Nov – 6 Dec)

| Milestone | Dates | Scope |
|---|---|---|
| M0 · Skeleton ✅ | 16–18 Nov (done 15 Nov) | Multi-module Gradle with convention plugins, design system, CI, capability registry skeleton, content format + validator, first 7 topics, home screen showing journey phase, up-next topics and phone setup |
| M1 · Blocking core | 19–24 Nov | Classifier chain (roles, keyboards, curated, category), rule chain, accessibility signal source, overlay enforcer, Settings guard, heartbeat, Study mode (DND + grayscale) |
| M2 · Learning core | 25 Nov – 1 Dec | 7:30 AM alarm + notification, NFC tag start, Now Bar session timer, session state machine, lesson cards, 2 question types, FSRS reviews, ladder L1, streak, haptics, self-rated placement |
| M3 · Urge interrupt | 2–6 Dec | Pause + reasons, review-quiz unlock, grants + Now Bar countdown, escalation, emergency unlock, night/morning lock, basic weekly stats, permissions onboarding → **install and start Phase 1 on 7 Dec** |

v1 (to 10 Jan, includes the AI layer), v1.5 (to 7 Feb) and v2 scopes are in the product spec §14.

## 15. Decision log

| ADR | Decision | Why |
|---|---|---|
| 001 | Native Kotlin + Compose (not Flutter) | Blocking and device features need native APIs anyway; one language end to end |
| 002 | Accessibility engine first; usage-stats behind the same interface | Personal build gets instant detection + in-app surfaces; runtime fallback when Advanced Protection is on; Play build swaps engines without touching rules |
| 003 | Content as compiled, versioned packs (no backend) | Offline, reviewable in git, validated in CI |
| 004 | FSRS over SM-2 | Better retention modelling; open algorithm |
| 005 | Provider-agnostic AI with per-task routing (Claude + on-device Gemini Nano) | Claude where depth matters; free, private, offline on-device answers where they're good enough; new providers plug in without feature changes |
| 006 | Device capabilities behind interfaces with NoOp twins | Use everything the S26 Ultra offers without making features depend on it; Play build degrades gracefully |
| 007 | targetSdk 37, minSdk 31 | Unlocks Android 17 APIs (MetricStyle, semantic colours, Advanced Protection API) while keeping a wider Play Store build possible |
| 008 | AI routes decided by an eval set, re-run after updates | Token savings without silent quality loss; on-device quality varies by model version |
| 009 | TOML front matter + Python stdlib checker | No tool dependencies; real dates; no YAML typing surprises; same checker runs locally, in Gradle and in CI |
| 010 | Toolchain versions pinned as a set (AGP, Kotlin, KSP, Hilt, Gradle) and bumped together | These break each other when moved alone; mirrors a combination known to build |
