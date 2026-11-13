# ETECSA Toolkit — Architecture & Initial Release Plan

App name: **ETECSA Toolkit** · Package: `dev.kurokawa.etecsa.toolkit` (installs beside RoCla) · Android only (no desktop shell — telephony app)

## 1. Repo — public source AND public releases (Ernest, 2026-11-13 update)

Ernest reversed the initial private-source decision: **everything public, one repo.**

| Repo | Visibility | Contents |
|---|---|---|
| `kurokawaerne/etecsa-toolkit` | **Public** | All source, CI, this spec, and GitHub Releases (split APKs per ABI + universal, with checksums) |

Creation note: the Muse GitHub App token cannot create repos (learned on LSEx) — the repo gets created in the signed-in browser.

## 2. Stack — as Ernest specified, with one honest adaptation

- **Kotlin, 100% native Android, Jetpack Compose + Material 3 Expressive** (official components first — the standing MD3E rule from LSEx/UDYAT: ShortNavigationBar, connected ToggleButton groups, SplitButton, official SearchBar, Minus-style Settings; Material Symbols in DEFAULT style, no Rounded; Google Sans Flex, rounded cut for display/headline/title + wordmark).
- **Rust core (`toolkit-core`, via UniFFI/JNI):** the USSD/SMS response **parser** (the whole regex suite for saldo/datos/voz/SMS/bonos, transfer confirmations, plan notices — the most correctness-critical, most testable code), plan-catalogue validation, antifraud sender matching, backup encryption format. Chosen because it is pure, hot, and benefits from fuzzable unit tests against captured ETECSA responses.
- **C core (`libtoolkit_c`, NDK/CMake):** speed-test measurement engine (timing/jitter math), traffic accounting, and the bridge to `libhev-socks5-tunnel` for the firewall — the places where the original already went native and where per-packet/per-sample cost matters.
- **Local DB — DECIDED 2026-11-13, AS BUILT: Android's built-in SQLite via SQLiteOpenHelper (lightweight wins).** Ernest left the choice to Luna as long as the app stays lightweight. Realm (MongoDB's local DB) was rejected on size and future: it ships several MB of native libraries per ABI, and MongoDB deprecated the Realm/Atlas Device SDKs (end of support 2025), so building a new app on it in 2026 would be a dead end. The shipped app talks to Android's built-in SQLite directly through a small `SQLiteOpenHelper` store — **zero added dependency weight at all** (the Room wrapper was dropped during the build for the same reason), and the original app's own tables map 1:1 (`transfers`, `book`, `blocked_sms`, `consult_history`, plus `cards`, `friends`, `sms_queue` and a key/value table). The store interface hides SQLite from the UI, so nothing else depends on this choice.
- **Languages — DECIDED 2026-11-13: English + Spanish, both complete**, live-switchable in Settings plus follow-system; ETECSA's own terms/plan names stay in Spanish in both locales (as in UDYAT).
- Ktor client for ETECSA/Nauta/tienda endpoints + catalogue updates; WorkManager/AlarmManager for monitors; Bubble API, predictive back (`PredictiveBackHandler` / `OnBackInvokedCallback`), `NotificationCompat.BubbleMetadata`.

## 3. Ernest's added features — designed in, not bolted on

1. **Coverage-back alarm:** `CoverageMonitorService` watches `TelephonyCallback` (service state / signal) per selected SIM; on a lost→restored transition, if the toggle is ON: full-screen-intent alarm notification + sound + vibration (user-pickable tone, default: one of the expressive bundled tones). Debounced (≥30 s of stable coverage) so border flapping doesn't scream. This generalises the original's "coverage confirmed" detection and its Pro-gated "send when coverage returns" — here both are free, and queued SMS auto-send on the same event.
2. **Manual SIM selector that always shows both slots:** a persistent SIM switcher (connected ToggleButton group: SIM 1 / SIM 2) in the top section of Consultas, Transferencias and SMS — driven by *slot index*, not by "detected subscriptions". If a slot has no detected subscription, the selector still offers it and the app explains/retries instead of silently hiding it (the original explicitly refused to guess a subscriptionId; we keep the safety, remove the dead end: user-forced slot → `createForSubscriptionId` attempt with clear error surface). All history stays keyed by slot + line, like the original's `line_key`.
3. **Contacts with photos:** `ContactsContract` API throughout — transfer/Plan Amigos/favourites pickers and lists show contact photo (or expressive initial avatar), name lookup by number for history antifraud and call log; favourites are *references* to system contacts, not a private copy (the original kept a separate `book`; we keep a local fallback only for numbers with no system contact).
4. **Everything searchable:** a single index (in-memory index over services, plans and screens, plus store queries over consults, transfers, SMS and calls) behind the top **SearchBar**.
5. **Top bar, LSEx-style:** MD3E SearchBar as the app header containing the new app icon (as logo), "**ETECSA Toolkit**" wordmark in Google Sans Flex rounded, and on the right the account avatar with a circle container underneath — tapping it opens Settings.
6. **Settings, Play-Store-account style:** tapping the avatar opens a sheet/page modelled on Google Play's account panel: profile header card (user's name/number(s) + SIM chips), then grouped sections in rounded containers — Apariencia (Minus themes, copied the same way as LSEx/UDYAT), Consultas y SIM, SMS y llamadas, Red y monitor, Nauta y Servicios ETECSA, Datos y copia de seguridad, Acerca de. Pencil icon = edit actions, History icon = history actions everywhere (no text labels for those two, per Ernest).
7. **Services broken into categories** on Home: Consultas · Planes · Transferencias y Recargas · Plan Amigos · Mensajería · Llamadas · Nauta · Servicios ETECSA · Red (Monitor/LTE/Velocidad/Firewall) · Herramientas — each a category section with expressive cards, all reachable from search and nav.
8. **Animated splash:** single animated splash (no double splash — the UDYAT lesson), new icon drawing itself in with the gradient sweep, then morphing into the top-bar logo.
9. **Predictive back:** enabled app-wide (`android:enableOnBackInvokedCallback`), Compose predictive-back handlers on sheets, search and in-call/USSD flows so the peek animation shows the real destination.
10. **Bubbles (Android 17 feature set / Notification Bubbles API):** ongoing USSD consults, Nauta session timer, coverage monitor and speed test can pop into bubbles; conversations (SMS) declare `ShortcutInfo` + `BubbleMetadata` so message threads bubble properly. Graceful fallback on OEMs without bubble support.
11. **New icon:** ETECSA logo redrawn as an adaptive icon in Google's current gradient style — gradient foreground on themed background, monochrome layer for Material You themed icons, and a dynamic variant (foreground sheen tied to the app theme). Delivered as adaptive XML + Play-style 512 px asset.

## 4. Module map

```
app/                Compose UI, navigation, splash, bubbles, widgets, tiles
core-rust/          toolkit-core (parser, antifraud matching, catalogue, crypto) — cargo + UniFFI
core-c/             libtoolkit_c (speed engine, traffic, hev-socks5 bridge) — CMake/NDK
data/               SQLite store (SQLiteOpenHelper) + repositories, Contacts gateway, catalogue sync
telephony/          USSD engine, SMS engine, call screening/in-call, SIM/slot resolver
services/           Coverage monitor, LTE monitor, Nauta, ETECSA online, firewall VPN, speed, overlay
```

## 5. Milestones (sized for Ernest's power windows, one batched release each)

- **M0 — Scaffold:** repos, Gradle + Rust/C toolchains in GitHub Actions CI (build compiler of record, per the LSEx lesson — local Gradle on Luna's VM is proxy-blocked), MD3E shell: splash, top SearchBar + avatar→Settings, ShortNavigationBar, Minus themes, SIM selector, empty category home. Split APKs in CI.
- **M1 — Consultas + Planes:** USSD engine + Rust parser with a test corpus built from real ETECSA response formats, history in SQLite, plan catalogue (bundled `plans.json` + updateable), purchase flows.
- **M2 — Transferencias + Plan Amigos + Contacts:** transfers, PIN change, Adelanta Saldo, recharge cards, friends, contact photos everywhere.
- **M3 — SMS + Llamadas + Protect:** default-SMS, smart queue (send-on-coverage), scheduled/autoreply, blocked SMS, antifraud vs official-senders catalogue, call blocker/screening/in-call.
- **M4 — Nauta + Servicios ETECSA:** portal, mailbox, quick tile, online account sync.
- **M5 — Red:** coverage-back alarm, LTE monitor, Cloudflare speed test (C engine), traffic overlay, firewall (hev-socks5).
- **M6 — Polish & v1.0:** bubbles, predictive-back audit, widgets, encrypted backup, self-update pointing at this repo's GitHub Releases, icon final, v1.0 release.

## 6. Decisions — all closed 2026-11-13

1. Visibility: **public source + public releases, single repo** (Ernest).
2. Local DB: **built-in SQLite (SQLiteOpenHelper)**, Luna's choice under Ernest's lightweight constraint.
3. Language: **EN + ES, both complete**, live switch + follow-system (Ernest).
