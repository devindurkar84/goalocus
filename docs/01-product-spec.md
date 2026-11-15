# Goalocus — Product Spec v0.6

*Status: building — M0 done · Updated 15 Nov 2026 · Owner: Dev*
*Related: [Architecture](02-architecture.md) · [App Access Policy](03-app-access-policy.md) · [Content format](content-format.md)*

**v0.6 changes:** the code lives on GitHub at github.com/devindurkar84/goalocus (§15). M0 (skeleton, content format and checker, first 7 topics) is built.

**v0.5 changes:** Option A chosen — Advanced Protection turned off and Auto Blocker set up on Dev's phone, so the personal build uses the full accessibility engine (D1, D10, D16).

---

## 1. Vision

A focus app for people preparing for technical roles, where **learning is the way out of distraction**. The phone locks for a short daily study slot; outside it, distracting apps cost a few revision questions. Every urge becomes a retrieval rep.

## 2. Who it's for

- **Now:** Dev, on his Galaxy S26 Ultra (Android 17 / One UI 9) with a Garmin watch, installed directly — targeting a **hybrid AI Engineer role (SDE with AI)**.
- **Later:** software-engineer / data-science aspirants via Play Store, once it works well for Dev.

## 3. Goals and non-goals

**Goals**
- Study **every day** without burning out — consistency over intensity.
- Cover CS fundamentals → DSA → System Design → ML/DL essentials → AI engineering → LLMOps/MLOps, **interleaved**, interview-focused.
- Cut time on distracting apps and turn urges into practice.
- Be **interview-ready by early February 2027**, offer by ~14 April, join ~mid-June 2027.

**Non-goals (v1)**
- A coding IDE or judge on the phone — DSA coding stays on the laptop; the app tests pattern recognition, complexity and dry-runs.
- Social features, leaderboards, coins.
- A general screen-time dashboard beyond what serves the goal.
- Copying third-party content — lessons are original; sources are linked.

## 4. Locked decisions

| # | Decision | Choice |
|---|---|---|
| D1 | Audience & distribution | Personal build first, installed from Android Studio (sideloaded); Play Store later |
| D2 | Escape hatch | Revision quiz to unlock, with escalating friction |
| D3 | Content | Hand-written, visual-first lessons + curated links; AI tutor scoped to the topic |
| D4 | Journey | Braided strands, CS fundamentals first; DSA never a blocker |
| D5 | Session length | Configurable, starts small, adapts automatically (session ladder) |
| D6 | Day 7 | Review / mock day |
| D7 | Target role | Hybrid AI Engineer (SDE with AI) |
| D8 | Timeline | 60-day notice → offer by ~14 Apr 2027 → join ~13 Jun 2027 |
| D9 | AI providers | Pluggable, routed per feature: Claude for depth; the phone's on-device model (Gemini Nano via ML Kit) for quick, frequent or private tasks; overridable per feature in settings |
| D10 | Blocking engine | **Accessibility service** (personal build); usage-stats engine behind the same interface for the Play Store build, or automatically if Advanced Protection is ever turned back on |
| D11 | Platform | Native Android — Kotlin + Jetpack Compose, targetSdk 37 |
| D12 | Device | Galaxy S26 Ultra, Android 17 / One UI 9 — use its capabilities fully (§11) |
| D13 | Study slot | **7:30 AM**, right after morning chai |
| D14 | Sleep data | Garmin watch → Garmin Connect → Health Connect (read-only) |
| D15 | Night | Wind-down **11:30 PM**, bedtime lock **12:00 AM** |
| D16 | Phone security setup | Advanced Protection **off**; Samsung Auto Blocker **on** between installs (turned off only while installing a new build), Maximum restrictions off |
| D17 | Code | GitHub repo `devindurkar84/goalocus`; CI builds a debug APK on every push |

## 5. The daily loop

1. **Wake** — the morning lock is still on (no feeds before study).
2. **Cue at 7:30 AM** after chai — tap the **NFC tag on your desk** (or the notification).
3. **Study mode switches on:** distractions blocked, non-essential notifications silenced, screen goes grayscale, timer appears in the Now Bar.
4. **Guess question → lesson cards → quiz → due reviews.**
5. **"How did today feel?"** — Easy / Right / Too much → feeds the session ladder.
6. **Teaser for tomorrow's topic**; optional bonus round; free window earned.
7. **Rest of the day:** distractions sit behind the urge interrupt; use the free window whenever you like.
8. **Night:** wind-down at 11:30 PM, bedtime lock at 12:00 AM.
9. **Day 7:** review / mock day.

---

## 6. Learning journey

### 6.1 Topic map
- A **topic** is one concept, small enough for a 10-minute core lesson.
- Each topic has: strand, module, hard prerequisites, soft prerequisites, bridges, interview weight (1–5), target-role tags, key points (used to check explain-backs), depth layers (core / extended / deep).
- **Hard prerequisite:** the scheduler waits for it. **Soft prerequisite:** a 60-second refresher card inside the lesson instead of blocking.

### 6.2 Strands (tuned for AI Engineer)

| Strand | Path |
|---|---|
| **Systems** | CS fundamentals (OS, Networking, DBMS) + Cloud → System Design → AI System Design |
| **Algorithms** | DSA — one pattern or structure per session, all year, never blocks anything |
| **Intelligence** | LLM primer → Math + ML/DL essentials (just enough for transformers) → LLM fundamentals → RAG → Agents & tool use → Evals & guardrails → Serving & cost → LLMOps / MLOps. Deeper classic ML comes after joining. |

System Design follows Dev's five-module plan: Scaling Fundamentals → Microservices & Communication → Consistency & Distributed State → Architecture Patterns → Advanced / Production, then AI System Design.

### 6.3 Timeline (60-day notice, join ~mid-June 2027)

| Phase | Dates | Weekly mix (6 learning days + Sunday review/mock) | Topics |
|---|---|---|---|
| 0 · Build MVP | 16 Nov – 6 Dec | — (build; start using it the day it installs) | — |
| 1 · Foundations | 7 Dec – 10 Jan | 3 CS + Cloud · 2 DSA · 1 LLM primer | 30 |
| 2 · Core | 11 Jan – 7 Feb | 2 System Design · 2 DSA · 2 Math / ML → DL essentials | 24 |
| 3 · Interview sprint | 8 Feb – 18 Apr | 2 SD incl. AI SD · 1 DSA · 3 LLM / RAG / Agents / Evals; Sunday = full mock | 60 |
| 4 · Notice period | 19 Apr – 13 Jun | 2 AI SD · 1 DSA · 3 Serving / LLMOps / MLOps | 48 |
| After joining | mid-Jun 2027 → | Rest of the map at a relaxed pace | — |

**By 13 June: 162 topics** — Systems 59 · DSA 36 · Intelligence 67.

**Key dates**
- **8 Feb** — start applying; Interview mode on.
- **~14 Apr** — latest offer date for a 60-day notice to end by ~13 Jun.
- **Why AI starts in Phase 1:** with interviews from February, a thin LLM strand from December means LLM, RAG and agent topics have been through spaced review several times before the first AI Engineer round.
- Placement skips topics Dev already knows (likely much of CS and core system design), so real coverage will be higher.
- All weights and dates are config, not code (`content/phases.toml`).

### 6.4 Spiral, bridges and gates
- **Spiral:** topics come back deeper. Caching: CPU/OS caches (Phase 1) → Redis, eviction, write-through (Phase 2) → semantic cache, KV cache, prompt caching (Phase 3–4).
- **Bridges** ("Connects to" section in every lesson):
  - Hashing → consistent hashing → DB sharding
  - B-trees → DB indexes → LSM vs B-tree storage engines
  - OS paging → paged KV cache in LLM serving
  - Graphs → HNSW vector index → RAG retrieval
  - HTTP / TCP → streaming LLM tokens over SSE
- **Unlock gates:** AI System Design case studies unlock when their prerequisites are learned. Examples:
  - *Design a RAG chatbot* ← embeddings, vector search, chunking, caching, rate limiting, LLM basics
  - *Design an LLM gateway* ← rate limiting, caching, routing, observability, token economics
  - *Design an agent platform* ← tool use, queues, idempotency, sandboxing, evals
  - *Design LLM inference serving* (Phase 4) ← batching, KV cache, autoscaling, GPUs basics

  The scheduler **pulls missing prerequisites forward** instead of making you wait, and shows progress ("72% to unlock").

### 6.5 Scheduler rules
- Only pick topics whose hard prerequisites are learned.
- Match the phase's strand mix each week (±1 session).
- Never the same strand 3 days in a row.
- Within a strand: gate pull-forwards first, then higher interview weight for the AI Engineer role.
- One topic swap per day (autonomy).
- **Interview mode** (policy swap, on from 8 Feb): weight shifts to high-frequency topics, weekly full mocks, and reviews of weak areas before each scheduled interview.

### 6.6 Placement
At onboarding, rate each module: *Know well / Somewhat / New*. "Know well" topics are marked learned and go straight into spaced review; a wrong review answer reopens the topic.

---

## 7. Session ladder (adaptive session length)

**Why start small:** in Lally et al. (2010), a new daily behaviour took a median of **66 days** to become automatic (range 18–254), and missing a single day didn't materially set people back. So the first two months optimise for *showing up*, not minutes.

| Level | Earliest | Core minutes | What fits |
|---|---|---|---|
| L1 · Show up | Day 1 | 10 | Guess question + core cards + 3 quiz Qs + 2 reviews |
| L2 · Settle | Day 15 | 15 | + 5-question quiz, more reviews |
| L3 · Build | Day 25 | 20 | + extended cards (trade-offs, interview angle) |
| L4 · Deep | Day 35 | 25 | + AI explain-back (typed or spoken), mixed reviews |
| L5 · Stretch (opt-in) | Day 45 | 25 + 5 break + 15 | + deep layer: go-deeper video/article, sketch-a-design or mock follow-ups |

**Rules**
- **Promote** only when: minimum days at the level are met, ≥ 6 of the last 7 sessions done, quiz average ≥ 70%, and no "Too much" in the last 3 sessions.
- **Step down** one level after 2 missed days in a week or 2 "Too much" in 5 sessions — shown as a "lighter week", never as failure.
- **Sleep-aware:** after a short night (< 6 h from the Garmin, via Health Connect), today's session is offered one level lighter — you can decline. If last night's sleep hasn't synced by 7:30 AM, no adjustment is made.
- **Cap:** never auto-promote past your cap (default 25 min). L5 is always opt-in.
- **End on time** — leave wanting more; anything extra is an optional bonus round.
- **Day 7** uses the same budget; Phase 3 adds an opt-in 30–45 min full mock.

With a 7 Dec start, L4 (25 min) is reachable around 10 Jan — before Interview mode begins.

---

## 8. Habit design

### 8.1 Urge interrupt (gated hours)
1. **Pause:** 5-second breathing ring, then "What are you here for?" — *Bored · Habit · Quick check · Earned break*.
2. **Review quiz** from your spaced-repetition queue (never new material). Wrong answers show the explanation — still learning.
3. **Pass** → timed unlock; the countdown shows in the Now Bar and turns to a caution colour near the end.
4. **Fail** → 2-minute cooldown.

| Unlock # today | Pause | Questions (to pass) | Unlock length |
|---|---|---|---|
| 1–2 | 5 s | 3 (2 right) | 10 min |
| 3–4 | 10 s | 4 (3 right) | 8 min |
| 5+ | 20 s | 5 (4 right) | 5 min |

- **During the study slot:** 5 harder questions + 30 s wait (not offered while the morning lock is on).
- **Grey unlocks (experimental):** the screen stays grayscale while a quiz-earned unlock runs — feeds are less rewarding without colour.
- **Emergency unlock:** 3/week, 60 s wait + typed reason, no quiz (see Access Policy).
- **Payoff:** 15 urges/day ≈ 45 extra retrieval reps.

### 8.2 Principle → feature

| Principle | In Goalocus |
|---|---|
| Prompt anchored to a routine (habit stacking) | 7:30 AM slot right after morning chai |
| Physical cue | NFC tag on the desk starts the session |
| Implementation intention | Onboarding asks you to write when / where / what |
| Make it easy | One tap (tag, notification or widget); focus already on |
| Make the bad habit unattractive | Grayscale during study, wind-down and (experimental) unlocks |
| Don't start the day reactive | Morning lock until today's session is done |
| Protect sleep | Wind-down + bedtime lock; Garmin data shows if it's working |
| Tiny habit for bad days | "Minimum day" = 3 review cards keeps the streak |
| Interrupt the autopilot | Pause screen before any distraction |
| Hot–cold empathy gap | Loosening strict mode takes effect only after 24 h |
| Loss aversion without "what-the-hell" collapse | Streak + earned freezes + "never miss twice" |
| Premack principle | Finishing today's session earns the free window |
| Goal-gradient & endowed progress | Placement pre-fills the map; gate progress bars; Now Bar timer |
| Open loops (Zeigarnik) | Every lesson ends with a teaser for tomorrow |
| Identity-based habits | "41 votes for becoming an AI engineer" — not points |
| Self-insight | Weekly report: urges by reason and hour |
| Retrieval practice (testing effect) | Daily quiz + every unlock |
| Spacing | FSRS-scheduled reviews |
| Interleaving | Braided strands |
| Pretesting | One guess question before each lesson |
| Dual coding | Every concept has a visual; S Pen sketching |
| Elaboration / Feynman | "Connects to" bridges + spoken or typed explain-back |
| Sleep & consolidation | Sleep-aware session ladder |

### 8.3 Guardrails
- **Minimum day** keeps the streak alive on bad days.
- **Streak freezes:** earn 1 per 7-day streak, hold max 2.
- **Never miss twice:** after a miss, the next session is one level lighter.
- **No coins or points** — rewards are mastery, unlocks and the free window (external points erode intrinsic motivation).
- **No shaming copy**, anywhere.

### 8.4 Free window, bedtime lock, morning lock *(bedtime confirmed; other values are defaults that stand unless changed)*

**Free window — earned, planned leisure**
- **Earned** by finishing today's session; a minimum day earns half.
- **Default 30 min**, one continuous block per day. Start it whenever you like after the session and before wind-down (11:30 PM); once started it runs to the end (no pausing to nibble).
- **Everything opens** except Shorts and Reels; countdown in the Now Bar.
- **No rollover** to the next day.
- **Why:** the Premack principle — a more-wanted activity rewards a less-wanted one. A planned indulgence also removes the "forbidden fruit" pull and collapses many small urges into one bounded block, and choosing when gives you control.

**Bedtime lock — protect sleep**
- **Wind-down from 11:30 PM:** grayscale, dim wallpaper, quiet notifications, one gentle nudge. Gated-hours rules still apply.
- **From 12:00 AM:** distractions blocked, no quiz hatch — emergency unlock only. Calls, alarms, UPI, maps and other Tier 0/1 apps stay open.
- **Why:** midnight to a 7:30 AM session leaves about 7 hours, so the lock time matters. Late scrolling is the most common thing that pushes bedtime back, and sleep is when the day's learning is consolidated. The weekly report compares lock time with the Garmin's recorded sleep time, so you see whether it's holding.

**Morning lock — don't start the day reactive**
- The bedtime lock **carries on after you wake until today's session is done**. If the session is skipped, it lifts at **10:00 AM** and the normal gated-hours rules take over.
- **Why:** wake → chai → tap the tag → study → then the phone opens up. Morning scrolling becomes the reward for studying instead of the thing that replaces it.

---

## 9. Lessons, quizzes, reviews

### 9.1 Lesson structure
1. **Guess first** — one question before reading.
2. **Hook** — why it matters in the real world.
3. **Core idea + visual.**
4. **Step-through** — e.g., TCP handshake, LRU eviction.
5. **Trade-offs & failure modes.** *(extended layer)*
6. **Interview angle** — how it's asked, usual follow-ups. *(extended layer)*
7. **Connects to** — bridges to earlier topics.
8. **Go deeper** — 2–3 curated links (best article / video with timestamp). *(deep layer)*

Depth shown scales with the session ladder level. Hovering the S Pen over a key term shows its one-line definition. The file format is in [content-format.md](content-format.md).

### 9.2 Visuals
- **v1:** authored Mermaid / SVG diagrams — cheap to write.
- **v2:** a small step-animation spec (nodes, edges, steps) in the content, rendered natively in Compose — one renderer for rings, queues, trees, request flows.

### 9.3 Question types (v1)
Single choice · Multiple choice · Order the steps · Spot the bug · Pick the complexity · Scenario trade-off.
Every question is tagged with interview frequency and target role, and written the way current interviews ask — scenarios and trade-offs over trivia. A pen scratchpad is available on every question for dry-runs and complexity working.

### 9.4 Spaced reviews
FSRS scheduler, target retention 90%. Review cards come from sessions, unlocks and Sunday reviews.

### 9.5 Day 7 — review / mock
- **Phases 1–2:** all due reviews + 10-question mixed quiz from the week + weekly report + next-week preview.
- **Phase 3+:** full mock (30–45 min, opt-in length), rotating weekly: DSA walkthrough → System Design → AI System Design (sketched with the S Pen, reviewed by Claude).

---

## 10. AI tutor (multi-provider)

Two providers today, more can plug in later:
- **Claude** (cloud) — best reasoning, vision; costs tokens.
- **On-device model** — the S26 Ultra's Gemini Nano, reached through Google's ML Kit GenAI APIs. Free per call, private, works offline; smaller and less capable. *Samsung's own Galaxy AI features (Note Assist etc.) have no public SDK for other apps as of Nov 2026; a Samsung provider can be added if one appears.*

### 10.1 Default routing

| Feature | Default route | Why | Release |
|---|---|---|---|
| Define a term / quick hint | On-device | Short, frequent, free, offline | v1 |
| Proofread a typed answer | On-device | Trivial task | v1 |
| Spoken answer → text | On-device speech recognition | Private, free | v1 |
| Explain-back | On-device pre-check against the lesson's key points → Claude Sonnet only if it passes or is borderline | Most "you missed X" feedback costs nothing | v1 |
| Ask about this topic | Claude Haiku (lesson cached) → "Go deeper" uses Sonnet | Quality matters | v1 |
| Weekly report summary | On-device **only** | Uses your app-usage data — it never leaves the phone | v1 |
| Mock interviewer | Claude Sonnet | Multi-turn reasoning | v1.5 |
| Sketch review | Claude Sonnet (vision) | Diagram reasoning | v1.5 |
| Lesson & question drafting | Claude, at authoring time on the laptop | Quality; not done on the phone | ongoing |

### 10.2 Rules
- **Config, not code:** the routing table lives in config; each feature can be overridden in settings (e.g., "always Claude for Ask").
- **Fallback:** if the first choice is unavailable — model downloading, app not in the foreground, quota hit, offline, no API key, daily budget spent — the next provider is tried.
- **Escape hatch:** every on-device answer has "Ask Claude instead".
- **Private data stays private:** tasks marked device-only never fall back to the cloud.
- **Routes earned by evals, not vibes:** a small eval set (~30 examples per task) runs both providers; a task moves on-device only if it scores close to Claude. (Good AI Engineer interview material in itself.)
- **Usage meter:** tokens used, tokens saved by on-device routing, and cost per feature in the weekly report; daily token cap in settings.
- **Keys:** Claude API key pasted in settings and stored in hardware-backed encrypted storage — never in source control or the APK. A Play Store build will need a small proxy backend instead.

### 10.3 On-device limits to design around
- It runs only while Goalocus is the app on screen — so not inside the urge-interrupt overlay or background jobs (the weekly summary is written when you open the report).
- Per-app request quota — the router backs off and falls through.
- Quality varies by model version on the device — routes are re-checked with the eval set after system updates.

## 11. Built for Galaxy S26 Ultra (Android 17 / One UI 9)

| Capability | Goalocus feature | Why it helps | Release |
|---|---|---|---|
| Live Updates → Now Bar | Session timer, unlock and free-window countdowns on the lock screen and status bar; caution colour near the end | Progress visible without opening the app; the ticking unlock makes the cost of scrolling felt | MVP |
| Modes API (DND + display effects) | "Goalocus Study" and "Wind-down" modes: silence non-essential notifications, grayscale, dim wallpaper, dark theme | A grey screen is less tempting; fewer pings = fewer urges | MVP |
| NFC | Tap a sticker on your desk to start the session | A physical cue makes the routine automatic | MVP |
| Haptics | Distinct feel for correct / wrong / unlock ending | Instant feedback without looking | MVP |
| Hardware-backed keystore | API key protected by the secure hardware | Safer key storage | MVP |
| On-device AI (Gemini Nano via ML Kit) | Hints, definitions, proofreading, explain-back pre-check, speech-to-text, private weekly summary | Saves Claude tokens; private; offline | v1 |
| Home-screen widget | Today's topic, streak, due reviews, gate progress; tap to start | A cue every time you look at the home screen | v1 |
| S Pen | Scratchpad for dry-runs; hover a term for its definition; handwriting works in answer fields | Working by hand aids recall; hover keeps you in flow | v1 |
| Voice | Spoken explain-back, transcribed on the phone | Teaching out loud is faster and deeper than typing | v1 |
| Health Connect | Sleep-aware ladder and bedtime-lock check using Garmin sleep data | Avoids "too much" days; shows whether bedtime is holding | v1 |
| Advanced Protection detection | If Advanced Protection is ever turned back on, switch to the usage-stats engine and say what's paused | Android 17 revokes accessibility access for non-accessibility apps in that mode | v1 |
| S Pen + Claude vision | Sketch-a-design for system design and AI system design mocks | Practice for whiteboard rounds | v1.5 |
| Adaptive layouts | Two-pane lesson + diagram in landscape and on DeX / external displays | Android 17 requires resizable layouts on large screens for apps targeting it | v2 |

## 12. App access

See **[App Access Policy](03-app-access-policy.md)** — tiers, time windows, emergency unlock.

## 13. Insights & success measures

**Weekly report:** study days, minutes, topics learned, quiz accuracy, reviews done, urges by reason and hour, unlocks and minutes on distractions, free-window use, emergency unlocks, ladder level change, bedtime-lock time vs Garmin sleep time, AI tokens used and saved, next week preview.

**Success measures (personal):**
- ≥ 6 of 7 study days, every week
- Minutes on distractions trending down
- Unlocks per day trending down
- Review retention ≈ 90%
- Topics learned vs phase plan
- Asleep within ~30 min of the 12:00 AM lock on most nights
- Mock scores trending up from February

---

## 14. Roadmap

| Release | When | Scope |
|---|---|---|
| **MVP** | 16 Nov – 6 Dec | Blocking core + essential-app tiers, study-slot alarm, Study mode (DND + grayscale), Now Bar session/unlock countdown, NFC start, haptics, session flow (lesson, quiz, FSRS reviews), streak, session ladder, quiz-to-unlock with escalation, emergency unlock, morning + bedtime lock, self-rated placement, 2 weeks of Phase 1 content |
| **v1** (weekly drops) | 7 Dec – 10 Jan | AI router with Claude + on-device providers and eval set, Ask + explain-back (typed/spoken), journey map + gates, Mermaid visuals, widget, S Pen scratchpad + hover glossary, sleep-aware ladder (Garmin), Advanced Protection fallback, Day-7 review, Shorts / Status / LinkedIn-feed / browser detectors, weekly report, strict mode, free window, wind-down, grey unlocks (experimental) |
| **v1.5 · Interview ready** | 11 Jan – 7 Feb | Interview mode, mock interviewer, sketch-a-design with Claude vision, AI System Design content |
| **v2** | Feb – Apr | Compose step-animations, adaptive two-pane / DeX layouts, deeper insights |
| **Play Store prep** | After joining | Usage-stats engine default, policy disclosure flows, Claude proxy backend, multi-user onboarding, downloadable content packs |

**Content plan:** Phase 1 needs 30 topics by 10 Jan; stay ≥ 2 weeks ahead (12 topics ready by 6 Dec; 7 written in M0). AI System Design case studies needed by 7 Feb. Claude drafts, Dev reviews ~10–12 topics a week.

---

## 15. Open items & setup checklist

**Resolved**
- **Where the code lives:** GitHub, `devindurkar84/goalocus` (D17).

**Open items**
1. **Defaults that stand unless changed:** free window 30 min (any time after the session, before 11:30 PM); morning-lock fallback 10:00 AM; ~10–12 topics/week of content review.

**Setup checklist (before the 6 Dec install)**
- [x] **Advanced Protection** turned off.
- [x] **Auto Blocker** set up: on between installs, off only while installing a new build; Maximum restrictions off.
- [x] **GitHub** connected to Claude.
- [ ] **Claude GitHub App** installed on the goalocus repo (so Claude can push).
- [ ] **Android Studio** on the laptop (for building and installing the app).
- [ ] **Python 3.11+** on the laptop (the build compiles lessons with it).
- [ ] **Garmin Connect → Health Connect:** allow sleep sharing, so Goalocus can read it.
- [ ] **NFC stickers** (NTAG213/215) for the desk.
- [ ] **Claude API key** ready to paste.
