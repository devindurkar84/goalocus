# Goalocus — App Access Policy v0.4

*Status: draft for Dev's review · Updated 15 Nov 2026 · Source of truth for `app-access-policy.json`*
*Related: [Product spec](01-product-spec.md) · [Architecture](02-architecture.md)*

**v0.4 changes:** bedtime lock at 12:00 AM, wind-down 11:30 PM; note that Tier 2 needs Advanced Protection off.

**Principle:** Goalocus never gets between you and anything you genuinely need — calls, OTPs, payments, sign-ins, travel, work. It only makes *distractions* expensive. Every installed app lands in one of four tiers.

---

## 1. How an app gets its tier (first match wins)

1. **Android roles** — whatever is currently your default Phone, SMS and Home app → Tier 0. If you switch apps, the rule follows automatically.
2. **Enabled keyboards** → Tier 0.
3. **Enabled accessibility tools** (TalkBack etc.) → Tier 0.
4. **Your overrides** in Goalocus settings (can move apps between Tiers 1–3; can't touch Tier 0).
5. **Curated list** (this doc), matched by package name — verified on your phone with `adb shell pm list packages`.
6. **Android app category**, when the app declares one — Social, Video, Game, News → Tier 3; others → Tier 1.
7. **Still unknown** → allowed, and Goalocus asks you once to classify it.

---

## 2. Tier 0 — Never blocked (locked, cannot be changed)

| Group | Apps |
|---|---|
| Calls & emergency | Default phone app (likely Samsung Phone), emergency call / SOS, Contacts |
| Messages & OTPs | Default SMS app (likely Samsung Messages) |
| Typing | Every enabled keyboard (Samsung Keyboard, Gboard …) |
| System | Home launcher (One UI Home), System UI, Google Play services, package installer, Settings\* |
| Time | Clock / alarms |
| Accessibility | TalkBack and other enabled accessibility tools |
| Self | Goalocus |

\* **Strict mode guards three Settings screens:** Goalocus's App info (force stop / uninstall), Accessibility → Goalocus, and Device admin apps. Opening them starts the **24-hour delayed-disable flow**, not a quiz — "calm you" decides, not "craving you". (Needs the accessibility engine.)

---

## 3. Tier 1 — Essential (allowed by default; you can move apps out)

| Group | Apps |
|---|---|
| Payments & UPI | Google Pay, PhonePe, Paytm, BHIM, CRED, Samsung Wallet, your bank apps (e.g., YONO SBI, HDFC, ICICI iMobile) |
| Sign-in & security | Google Authenticator, Microsoft Authenticator, Authy, Okta Verify, Duo, your password manager |
| Government & ID | DigiLocker, mAadhaar, UMANG |
| Navigation & travel | Google Maps, Uber, Ola, Rapido, Namma Yatri, IRCTC Rail Connect, metro & airline apps |
| Daily needs | Swiggy, Zomato, Blinkit, Zepto, pharmacy apps |
| Utilities | Camera, Calendar, My Files, Calculator, Gallery / Photos, Drive, Samsung Notes, Samsung Health, Garmin Connect |
| Work | Gmail, Outlook, Slack, Teams, Zoom, Meet |
| Focus music | Spotify, YouTube Music, JioSaavn |

Option: **"Quiet work apps during study slot"** — off by default.

---

## 4. Tier 2 — Dual-use (app allowed; specific screens treated as distractions)

Needs the accessibility engine, which needs **Advanced Protection off**. If the engine falls back to usage-stats (Advanced Protection on, or the Play Store build), these apps are treated as whole apps — allowed.

| App | Always allowed | Treated as distraction |
|---|---|---|
| WhatsApp, Telegram | Chats, calls | Status / Updates / Channels |
| YouTube | Videos opened from a Goalocus lesson link | Home feed, search, **Shorts (blocked all day, no unlock — toggle)** |
| Chrome, Samsung Internet & other browsers | All other sites | Editable domain list: instagram.com, facebook.com, x.com, reddit.com, youtube.com/shorts … |
| LinkedIn | Messages, Jobs, your profile | Feed — you'll need LinkedIn for the switch, just not the feed |

**Reels:** even when Instagram is unlocked, Reels stays blocked (toggle).

**Fail-safe:** screen detectors depend on each app's layout, which changes with app updates. If a detector can't recognise a screen, Goalocus **allows** it (never locks you out of a WhatsApp chat) and shows "detector needs update" in settings.

---

## 5. Tier 3 — Distractions

| Group | Apps |
|---|---|
| Social | Instagram, Facebook, X, Threads, Snapchat, Reddit, Pinterest, Quora, ShareChat, Moj, Josh |
| Video & OTT | Netflix, Prime Video, JioHotstar, SonyLIV, MX Player |
| Games | Everything in the Games category |
| Shopping | Amazon, Flipkart, Myntra, Meesho, Ajio, Nykaa (move to Tier 1 if you need order tracking) |
| News feeds | Inshorts, Dailyhunt, Google News |

---

## 6. What happens when (a typical day)

| Window | Default time | Tier 0 & 1 | Distractions (Tier 3 + Tier 2 distraction screens) |
|---|---|---|---|
| **Night + morning lock** | 12:00 AM → until today's session is done (lifts 10:00 AM if skipped) | Open | Blocked, no quiz hatch — emergency unlock only |
| **Study slot** | 7:30 AM, after chai | Open | Blocked (covered by the morning lock); notifications quiet except calls, SMS, alarms |
| **Gated hours** | After the session → 11:30 PM | Open | Urge interrupt: pause → review quiz → timed unlock (escalates through the day) |
| **Free window** | 30 min, earned by today's session; you start it any time before 11:30 PM | Open | Open — except Shorts & Reels |
| **Wind-down** | 11:30 PM – 12:00 AM | Open | Gated-hours rules + grayscale, dim wallpaper, quiet notifications |

**Study-mode notifications:** during the slot, Goalocus's Study mode lets through calls, SMS (so OTPs always arrive) and alarms; other notifications wait until the session ends.

**Emergency unlock:** 3 per week · 60-second wait + type a reason · no quiz · 15 minutes · listed in the weekly report. For real needs, like an app you forgot to allowlist.

Escalation details for the urge interrupt live in the [Product spec §8](01-product-spec.md).

---

## 7. To confirm on your phone

- [ ] Add your bank apps and password manager by name.
- [ ] Confirm package names: `adb shell pm list packages | grep -i <name>`.
- [ ] Check which Phone / Messages apps are your defaults (Samsung vs Google).
- [ ] Wind-down 11:30 PM, bedtime 12:00 AM (confirmed); morning fallback 10:00 AM and free window 30 min — keep or change?
- [ ] Any app here you'd move to a different tier?
