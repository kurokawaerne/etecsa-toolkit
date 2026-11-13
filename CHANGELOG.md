# Changelog

All notable changes to ETECSA Toolkit are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/), versions follow [Semantic Versioning](https://semver.org/).

## [1.0.0] — Initial release

The first release of ETECSA Toolkit: a clean-room Material 3 Expressive rewrite of RoCla USSD 5.4.3 (feature inventory in [docs/REVERSE-ENGINEERING.md](docs/REVERSE-ENGINEERING.md)), with **every feature free — no Pro, no trial, no licence**. The original gates features behind a 250 CUP / 365-day Pro subscription (Material You, widgets, non-contact SMS blocking, send-on-coverage, extra Nauta accounts, and more); all of them are simply included here.

### Added

**Consults & plans**
- Smart USSD engine (`*222#` family and related codes) with automatic parsing of balance, data, voice, SMS, bonuses and expiry dates; per-SIM consult history, background auto-sync and plan-expiry reminders
- Plan catalogue with 15 plans (combined data, toDus / Nauta-mail, daily bag, voice, SMS) with ETECSA prices and one-tap purchase via the `*133#` tree

**Transfers, recharges & Plan Amigos**
- Balance transfers with confirmation flow and smart transfer history; transfer PIN change; Adelanta Saldo; recharge by voucher card and by call; national-recharge availability notices
- Plan Amigos consult / activate / deactivate with friends-list management

**Messaging & calls**
- Smart SMS: default-SMS inbox, send queue, scheduled SMS, send-when-coverage-returns, blocked-SMS store, non-contact block / silence, ETECSA spam filtering, SMSC editing, per-SIM sending, segment & cost counter, auto-replies
- Antifraud Protect: local detection of sender impersonation (ETECSA, Cubacel, Pago×Móvil, ENZONA, …) against the official-sender catalogue, alert sounds, antifraud history
- Call log, call blocker, call screening and an in-call screen with duration cut timer

**Nauta & ETECSA services**
- Nauta portal login with session timer, quick-connect tile and mailbox with multiple accounts
- ETECSA online-services account with linked lines and balance / plan sync

**Network**
- Network monitor, LTE / cell info, Cloudflare speed test with history, data-usage tracking with a floating traffic overlay, and a per-app firewall over a local VPN

**New in this rewrite (not in the original)**
- **Coverage-back alarm** (optional toggle): sound + vibration the moment coverage returns after a loss; queued SMS send on the same event
- **Always-visible SIM 1 / SIM 2 slot selector**, usable even with one SIM or a slot the system mis-detects
- **Contacts with photos** via the Android Contacts API across transfers, Plan Amigos, history, calls and antifraud
- **Global search**: one index over consults, transfers, SMS, calls, plans, services, contacts and settings, behind the top SearchBar (app-icon logo + expressive wordmark + circled avatar opening Play-Store-style Settings)
- Services grouped in categories; pencil = edit and history = history icons instead of text labels
- Animated splash screen, predictive back, notification bubbles
- New gradient ETECSA adaptive icon with a Material You themed variant

**Platform & design**
- Kotlin + Jetpack Compose, Material 3 Expressive official components, Material Symbols (default style), Google Sans Flex, Minus-style themes with Material You
- Rust core for the USSD/SMS parser and antifraud matching; C core for the speed / traffic engine; SQLite local database (Android built-in)
- Fully bilingual UI: English + Spanish
- Split APKs per ABI (arm64-v8a, armeabi-v7a, x86_64) plus a universal APK

### Notes
- Clean-room reimplementation: no RoCla USSD code was reused. USSD codes, plan data and prices are ETECSA's public facts.
- Package `dev.kurokawa.etecsa.toolkit` installs alongside RoCla USSD.
