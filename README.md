# ETECSA Toolkit

**Every ETECSA / Cubacel tool you need — balances, plans, transfers, smart SMS, Nauta, network monitoring and a firewall — rebuilt in Material 3 Expressive, and completely free.**

**Todas las herramientas de ETECSA / Cubacel que necesitas — saldo, planes, transferencias, SMS inteligente, Nauta, monitor de red y firewall — rehechas en Material 3 Expressive, y totalmente gratis.**

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android_8.0%2B-green)
![Language](https://img.shields.io/badge/UI-EN%20%2F%20ES-orange)

ETECSA Toolkit is a clean-room Material 3 Expressive rewrite of RoCla USSD 5.4.3. Same jobs, same USSD codes — all-new code, a modern expressive interface, and **no Pro, no trial, no locked features**. The original gates part of its feature set behind a 250 CUP/year Pro subscription; this rewrite removes that entirely. Everything below is available to everyone, forever.

- **Package:** `dev.kurokawa.etecsa.toolkit` — installs alongside RoCla USSD, nothing is overwritten
- **Fully bilingual:** English + Spanish, both complete, switchable live in Settings (or follow system)
- **Lightweight by design:** SQLite (built into Android) for local data — no heavyweight database engine bundled into the APK

---

## ✨ Features

Services are grouped into categories on the Home screen, and everything is searchable from the top SearchBar.

### 📊 Consults — Smart USSD
- Smart USSD engine for the `*222#` family and related codes: main balance, data, voice minutes, SMS, bonuses and expiry dates, parsed automatically
- Per-SIM history of every consult, with the resources identified from each operator response
- Background auto-sync and plan-expiry reminders
- **Always-visible SIM 1 / SIM 2 slot selector** — switch which SIM is checked even with a single SIM, and even when a slot isn't detected correctly

### 📦 Plans
- Full plan catalogue (15 plans in [`catalog/plans.json`](catalog/README.md)): combined data plans, the toDus / Nauta-mail 1 GB pack, the daily bag, voice packs and SMS packs — with current ETECSA prices and one-tap purchase codes (`*133#` tree)
- Plan state tracking per SIM, with validity / expiry notices

### 💸 Transfers & recharges
- Balance transfers with an explicit confirmation flow and a smart transfer history
- Transfer PIN change, **Adelanta Saldo**, recharge with voucher cards, recharge by call
- National-recharge availability notices

### 👥 Plan Amigos
- Consult, activate and deactivate Plan Amigos, and manage your friends list

### 💬 Smart SMS
- Default-SMS inbox with a smart send queue: send later, **scheduled SMS**, and **send automatically when coverage returns**
- Blocked-SMS store, block / silence non-contacts, ETECSA spam filtering that keeps important notices
- SMS centre (SMSC) editing, per-SIM sending, segment & cost counter, auto-replies and quick replies

### 🛡️ Antifraud — Protect
- Scans incoming SMS (and, on demand, your whole inbox) for impersonation of ETECSA, Cubacel, Pago×Móvil, ENZONA and other official senders
- Verified against the official-sender catalogue (see [`catalog/README.md`](catalog/README.md)); warnings with alert sounds, antifraud history, optional auto-blocking of fraud senders
- All filtering happens **locally on your device**

### 📞 Calls
- Call log, call blocker and call screening
- In-call screen with a call-duration cut timer

### 🌐 Nauta
- Nauta Wi-Fi portal login with session timer and remaining-time sync
- Quick-connect tile, automatic disconnect, and a Nauta mailbox with multiple accounts

### 🏢 ETECSA online services
- Account login and linked-line management against ETECSA's online services, with balance / plan sync

### 📶 Network
- **Coverage-back alarm (new):** an optional toggle — when coverage is lost and then restored, the app fires an alarm (sound + vibration) so you know the moment you're back. Queued SMS go out on the same event
- Network monitor with coverage-loss / restore detection, LTE & cell info, and Force-LTE helper
- Cloudflare-based speed test (download / upload / ping / jitter) with history
- Data-usage tracking and a floating traffic / speed overlay
- **Per-app firewall** over a local VPN: block apps on mobile data, on Wi-Fi, or block new apps by default

### 🧩 System
- Home-screen widgets, encrypted backup & restore, and self-update from this repo's GitHub Releases
- Contacts everywhere via the Android Contacts API — **with contact photos** in transfers, Plan Amigos, history, calls and antifraud
- Animated splash screen, predictive back throughout, and notification **bubbles** for consults, Nauta sessions, the network monitor and SMS conversations
- New app icon: the ETECSA mark redrawn in Google's gradient style, as an adaptive icon with a Material You themed (monochrome) variant

### 🎨 Design
- Material 3 Expressive with official components first: `ShortNavigationBar`, top `SearchBar`, connected ToggleButton groups, split buttons — no replicas where an official component exists
- Top SearchBar houses the app icon as logo, the **ETECSA Toolkit** wordmark, and a circled account avatar that opens Settings
- Settings redesigned in the Google Play account-panel style: profile header, then grouped sections in rounded containers
- Material Symbols in the default style throughout; pencil icon = edit, history icon = history — no text labels for those actions
- Google Sans Flex, rounded where it matters (display, headlines, titles and the wordmark), with Minus-style themes including Material You

---

## 📸 Screenshots

> Screenshots coming with the v1.0.0 release.

| Home | Consults | Plans | Settings |
|---|---|---|---|
| _soon_ | _soon_ | _soon_ | _soon_ |

---

## 📥 Download & install

Grab the APK for your device from [**Releases**](https://github.com/kurokawaerne/etecsa-toolkit/releases). Releases ship **split APKs per ABI plus a universal APK** — on a metered connection, download only the one you need:

| File | Pick it if… |
|---|---|
| `*-arm64-v8a.apk` | Your phone is 64-bit ARM — almost every phone from the last several years (e.g. Redmi Note 11). **This is the one most people want.** |
| `*-armeabi-v7a.apk` | Your phone is an older 32-bit ARM device. |
| `*-x86_64.apk` | You're running Android on an x86_64 device or emulator. |
| `*-universal.apk` | You're not sure — it works everywhere, it's just bigger. |

Not sure which ABI you have? Check *Settings → About phone*, or just take the universal APK.

Install it like any sideloaded APK (allow "install from this source" when Android asks). ETECSA Toolkit installs next to RoCla USSD — your old app and its data stay untouched.

---

## 🛠️ Build

**GitHub Actions is the compiler of record** — CI builds and verifies every change. Local builds work too:

Requirements: JDK 21, Android SDK (compileSdk 36), Rust (stable) for the native core, CMake / NDK for the C core.

```bash
# Android app (debug)
./gradlew :app:assembleDebug

# Rust core — parser & antifraud test suite
cd core-rust && cargo test

# C core — speed / traffic engine
cmake -S core-c -B core-c/build && cmake --build core-c/build
```

Release builds are signed from CI secrets (`ETK_KEYSTORE_FILE`, `ETK_STORE_PASSWORD`, `ETK_KEY_ALIAS`, `ETK_KEY_PASSWORD`); without them, builds stay debug-signed.

---

## 🏗️ Architecture

One Android app, three layers: a Kotlin/Compose UI, a Rust core for the correctness-critical parsing, and a C core for the measurement-hot paths. Local data lives in SQLite — Android's built-in database — behind repositories, so the UI never touches SQL directly.

```
app/          Android shell: MainActivity, splash, widgets, tiles, bubbles
shared/       Compose UI (Material 3 Expressive), navigation, themes, EN/ES strings
core-rust/    toolkit-core — USSD/SMS response parser, plan catalogue,
              antifraud sender matching, backup crypto (UniFFI/JNI)
core-c/       libtoolkit_c — speed-test engine, traffic accounting,
              firewall tunnel bridge (hev-socks5-tunnel)
catalog/      plans.json + official-senders data (see catalog/README.md)
docs/         Architecture and reverse-engineering reports
```

Full details: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** · How the original app was analysed: **[docs/REVERSE-ENGINEERING.md](docs/REVERSE-ENGINEERING.md)**

---

## 🧼 Clean-room note

ETECSA Toolkit is an independent, clean-room reimplementation. **No code from RoCla USSD was reused, decompiled into this repo, or republished** — the original app is obfuscated and remains its author's work. What carries over is behaviour and facts: USSD codes, plan names, prices and official sender names are ETECSA's public facts, not any app's intellectual property. RoCla's backend, branding and its Pro payment/licence system are not reproduced.

This project is not affiliated with ETECSA, Cubacel or RoCla.

---

## 📄 License

Licensed under the **Apache License 2.0** — see [LICENSE](LICENSE).

The bundled `libhev-socks5-tunnel` used by the firewall is MIT-licensed (© heiher); its license ships inside the APK assets. Google Sans Flex is distributed under the SIL Open Font License.
