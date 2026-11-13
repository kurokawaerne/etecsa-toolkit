# ETECSA Toolkit — Reverse Engineering Report: RoCla USSD 5.4.3

Date: 2026-11-13 · Analyst: Luna · Method: clean-room (behaviour/spec extraction only, no decompiled code will be reused or published)

## 1. Identity — verified

| Field | Value |
|---|---|
| Package | `com.rocla.ussd` |
| Version | 5.4.3 (versionCode 454) — matches the publisher's own `update.json` |
| APK size | 2,717,962 bytes (2.6 MB) |
| SHA-256 | `39818c6ed28c2a788bae6b6bd58d31a0a428d5bab74cecde69692f676aa877a6` — identical to the sha256 published in `https://roclaussd.roclahy.com/update.json`, so this is the authentic current release |
| MD5 | `2145f135037a525bbf6fe886281086a5` |
| minSdk / targetSdk | 26 (Android 8.0) / 35 (Android 15) |
| Main activity | `com.rocla.ussd.LauncherDefault` → `MainActivity` |
| Structure | Single `classes.dex` (1.76 MB), 658 classes in the app package, ~131,000 lines after JADX decompilation, names obfuscated to `A0`, `C0015b0`, … with version-suffixed feature classes (`…V461`, `…V477`, `…V534`, `…V540`) |
| Original stack | Plain Java + Android Views (programmatic UI), **not** Kotlin/Compose. No Flutter/React Native. |
| Native code | Only one bundled library: `libhev-socks5-tunnel.so` (hev-socks5-tunnel, MIT, https://github.com/heiher/hev-socks5-tunnel) for arm64-v8a / armeabi-v7a / x86 / x86_64 — this is the engine under the per-app Firewall (local VPN/tunnel) |
| Build | Android Gradle Plugin 8.6.1 |
| Backend | `roclaussd.roclahy.com` (`/api/mi-rocla/`, `/api/mobile/monitor-v1`, `/api/telemetry/active`, `plans.json`, `official-senders(-v2).json`, `update.json`), plus ETECSA: `secure.etecsa.net:8443` (Login/EtecsaQuery/Logout servlets), `www.tienda.etecsa.cu` API, `www.nauta.cu`; speed test against Cloudflare (`speed.cloudflare.com/__down`, `h3.speed.cloudflare.com/__up`) |

Local artefacts (not published): original APK, JADX output (661 files), full string table (18,641 strings), `apk_info.json`, `plans.json`, `official-senders.json` — under `~/workspace/etecsa-toolkit/reverse/`.

## 2. Permissions (27)

CALL_PHONE, READ_PHONE_STATE, READ_CONTACTS, READ_SMS, SEND_SMS, RECEIVE_SMS, RECEIVE_WAP_PUSH, READ_CALL_LOG, WRITE_CALL_LOG, ACCESS_NETWORK_STATE, INTERNET, ACCESS_COARSE/FINE_LOCATION (LTE cell detection), PACKAGE_USAGE_STATS (data usage), QUERY_ALL_PACKAGES (firewall app list), POST_NOTIFICATIONS, FOREGROUND_SERVICE (+SPECIAL_USE), SYSTEM_ALERT_WINDOW (traffic overlay), USE_FULL_SCREEN_INTENT, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, REQUEST_INSTALL_PACKAGES (self-update), VIBRATE, WAKE_LOCK, + its own INTERNAL_BROADCAST. Feature: `android.hardware.telephony`.

Roles it can take: default SMS app, dialer / in-call service, call-screening service, accessibility-driven USSD response reader, VPN service (firewall).

## 3. Components

**20 activities:** Main, Antifraud + AntifraudHistory, CallBlocker, Calls (V477), Cards (V4156 — recharge cards), EtecsaServices (V4180), ForceLteBridge, LteMonitor, Messages (V477), SmartSms, NautaHub, NautaMailbox, NautaPortal, ResourceWidgetConfig, RoClaFirewall (V540), RoClaInCall (V484), RoClaMonitor (V535), RoClaSpeed (V534), UpdateInstall (V461).

**17 services:** AntifraudProtection, CallScreening, CoverageSmsJob, EtecsaAutoSyncJob, EvidenceCaptureJob, LteMonitor, NautaQuickConnect + NautaQuickTile, RespondViaMessage, RoClaFirewall, RoClaInCall, SmartSmsQueue, SmartVerification, SmsNotificationReadJob, SmsStatusReplayJob, TrafficOverlay, WidgetRefreshJob.

**22 receivers:** SMS (default/status/WAP/reply-reminder), scheduled SMS, antifraud, evidence capture, network-monitor restore, reminder, call actions, firewall install, update download, trial expiry, traffic-overlay restore, and 3 home-screen widgets (Resource, Resource Compact, Etecsa Multi-Line).

## 4. Feature inventory — grouped the way ETECSA Toolkit will present them

### A. Consultas (USSD engine)
Codes found verbatim in the dex: `*222#` (saldo), `*222*264#`, `*222*266#`, `*222*328#`, `*222*468#`, `*222*732#`, `*222*767#`, `*222*869#`, `*111#`, `*250#`, plus the `*133*…#` tree below and `*234*3*1#` (transfer PIN change).
Smart USSD engine: runs a code, reads the operator response (TelephonyManager USSD API + accessibility fallback), parses it with a large regex suite (saldo CUP, datos GB/MB with vigencia, voz/min, SMS count, roaming, bonos) and stores every consult in `consult_history` + `resource_evidence` (per-SIM: sub_id, slot, line_key). Background auto-sync, plan-expiry reminders, per-line history.

### B. Planes — Comprar / Gestionar (`*133#` tree)
Remote catalogue `plans.json` v1 (15 plans, prices verified 2026-11-13):
- **Datos combinados:** 4.5 GB `*133*1*4*1*1#` 240 CUP · 2 GB (+15 min/20 SMS) `*133*1*4*2*1#` 120 CUP · 4 GB (+35/40) `*133*1*4*3*1#` 240 CUP · 6 GB (+60/70) `*133*1*4*4*1#` 360 CUP — all +300 MB .cu
- **Datos sueltos:** 1 GB toDus/Correo Nauta `*133*1*2*1#` 25 CUP · Bolsa diaria 200 MB/24 h `*133*1*3*1#` 25 CUP
- **Voz:** 5/10/15/25/40 min — `*133*3*1*1#` … `*133*3*5*1#` — 37.50/72.50/105/162.50/250 CUP
- **SMS:** 20/50/90/120 — `*133*2*1*1#` … `*133*2*4*1#` — 15/30/50/60 CUP
Plus: LTE data pack `*133*1*1*2#`, tarifa por consumo toggle, plan state tracking (dirty/verified timestamps per SIM), expiry notices.

### C. Transferencias y recargas
Transferir saldo (with confirmation flow, never auto-presses "1"), historial inteligente de transferencias (states USSD/SMS/ABO/PEN/MMI/RED, `transfers` table: number, cents, ts), Cambiar PIN de transferencia, Adelanta Saldo, recargar con tarjeta (voucher), recargar por llamada, avisos de recarga nacional disponible, monthly transfer totals. ETECSA's own limit noted in-app: 3 transfers/day.

### D. Plan Amigos
Consultar / Activar / Desactivar Plan Amigos, lista de amigos (`book`-style contacts), consult history specific to it.

### E. SMS inteligente (RoCla Protect)
Default-SMS inbox/sent, Smart SMS queue (send later, **send when coverage returns**), scheduled SMS, auto-reply on keyword, quick replies, SMSC editing, per-SIM send choice, segment/cost counter (15 CUP/SMS international), blocked SMS store (`blocked_sms` table, >10,000 cap with purge), block/silence non-contacts, discard ETECSA spam/promotions while keeping important notices.
**Antifraud / Protect:** scans incoming (and on-demand the whole inbox), detects impersonation of ETECSA/Cubacel/Pago×Móvil/ENZONA senders using the signed remote catalogue `official-senders-v2.json`, warns with bundled alert sounds, keeps antifraud history, can auto-block detected fraud senders. All filtering is local.

### F. Llamadas
Call log, call blocker (numbers/patterns, per mobile-data state rules), call screening, in-call screen with call-duration cut timer ("El tiempo empieza cuando se conecta la llamada. El corte es irreversible"), respond-via-message, "remind me if they don't answer back", birthday reminders from contacts.

### G. Nauta
Nauta Hub: portal Wi-Fi login (`secure.etecsa.net:8443`), session timer with remaining-time sync, quick-connect tile/service, auto-disconnect, Nauta mailbox (multiple accounts, saved-bytes tracking), account CRUD.

### H. Servicios ETECSA (online account)
Login against tienda.etecsa.cu / secure.etecsa.net, linked lines management, balance/plan sync (EtecsaAutoSync), identity-document (carnet) verification flow, session handling.

### I. Red — Monitor, LTE, Velocidad, Firewall
- **Monitor de red (V535):** periodic snapshots via `/api/mobile/monitor-v1`, alerts, coverage-loss/restore detection ("Cobertura confirmada tras falta de datos", "LTE confirmada tras falta de datos").
- **LTE monitor:** registered LTE cell detection (precise location permission, location itself not stored), Force LTE bridge.
- **RoCla Speed (V534):** Cloudflare speed test (download/upload/ping/jitter), `measurements` table with scope/mode/partial results, history.
- **Traffic overlay:** floating speed indicator + status-bar speed, data usage since midnight (Wi-Fi / mobile), per-app usage.
- **Firewall (V540):** per-app block on mobile data / Wi-Fi / for new apps, built on a local VPN + hev-socks5-tunnel, `attempts` table logging blocked connection attempts (pkg, ip, host, port, protocol, action).

### J. Sistema
3 widgets, self-update from `update.json` (signature-checked), encrypted backup/restore of history/favourites/services/plans (password, format "seguro de RoCla USSD 4.4.1"), themes incl. Material You, icon variants, recents blocking, support/ambassadors, telemetry (`/api/telemetry/active`).

## 5. Local data model (original, SQLite)

`transfers(id, number, cents, ts)` · `book(number PK, name, created_at)` — its favourites/friends address book · `blocked_sms(_id, sender_key, address, body, date, seen)` + `blocked_sms_meta` · `consult_history(_id, resource, value, sub_id, slot, ts, automatic, line_key, source)` · `resource_evidence(id, kind, number, raw, billing, source, sub_id, slot, units, direction, at, received_at, duration_ms, amount, result_balance, sim_exact, meta)` · `measurements(…speed test…)` · `attempts(…firewall…)` — all heavily indexed per SIM/line. Everything else lives in SharedPreferences (hundreds of keys: `nauta_*`, `plan_*`, `monitor_*`, `trial_*`…).

## 6. Pro / Trial gating — to be removed entirely

24-hour free trial (HMAC-sealed, `pro_trial_*_v403/v4163`), then Pro annual **250 CUP / 365 days**, paid by saldo transfer, restored by code/license `RCLP2`, with a Telegram bot (`t.me/RoClaUSSDPro_bot`).
Gated features found in strings (non-exhaustive): Material You / adaptive colours, widgets, block & silence non-contact SMS, discard ETECSA spam, send-when-coverage-returns SMS, smart mode, "remind if no reply", antifraud/network/traffic extras restored after trial (`trial_restore_*_v462`), extra Nauta mailbox accounts (1 free), several monitor alerts.
**ETECSA Toolkit decision (Ernest, 2026-11-13): no Pro, no trial, no licence — every feature unlocked.**

## 7. What cannot / will not be copied

- The decompiled code itself: obfuscated, and RoCla's own. ETECSA Toolkit is a **clean-room reimplementation from this spec** — same USSD codes, plan catalogue, regex parsing behaviour and feature set, all-new code.
- RoCla's backend (`roclaussd.roclahy.com`) and its Pro payment/licence system: not reproduced. Plan/sender catalogues are re-hosted as static JSON in our own repo (the data — prices, codes, official sender names — is ETECSA fact, not RoCla IP).
- RoCla name/branding: replaced by ETECSA Toolkit + a new icon (ETECSA mark redrawn in Google's gradient app-icon style, adaptive/themed variants).
