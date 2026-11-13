# Catalogues

Static data catalogues bundled with the app and refreshable at runtime. This data is **facts about ETECSA's public offering** — USSD codes, plan names and prices — not code or content copied from any app.

## `plans.json`

The Cubacel plan catalogue, in the same schema the original RoCla USSD app downloads from its public `plans.json` endpoint:

```json
{
  "schema": 1,
  "catalogVersion": 1,
  "plans": [
    { "id": "data_6", "category": "data", "title": "6 GB",
      "detail": "60 min · 70 SMS · 300 MB .cu",
      "price": "360 CUP", "code": "*133*1*4*4*1#" }
  ]
}
```

| Field | Meaning |
|---|---|
| `id` | Stable plan identifier |
| `category` | `data`, `voice` or `sms` — drives the Plans sections in the UI |
| `title` | Plan size (GB / minutes / SMS count) |
| `detail` | Bundled extras and validity notes (Spanish, as ETECSA states them) |
| `price` | Price in CUP, exactly as ETECSA publishes it |
| `code` | The USSD purchase code dialled to buy the plan |

Current contents: **15 plans** — 6 data (4.5 GB, 2 GB, 4 GB, 6 GB combined plans, the 1 GB toDus / Correo Nauta pack, and the 200 MB daily bag), 5 voice packs (5–40 min) and 4 SMS packs (20–120 SMS).

Prices and codes change when ETECSA changes them. To update the catalogue, edit this file, bump `catalogVersion`, and the app picks it up on the next catalogue refresh — no app update needed for data-only changes.

## Official senders (antifraud)

The antifraud Protect feature matches incoming SMS senders against a catalogue of **official senders** — the legitimate sender names / numbers used by ETECSA, Cubacel, Pago×Móvil, ENZONA and similar services. Anything that *imitates* one of these brands from an unrecognised sender is flagged as possible fraud.

This mirrors the data behind the original app's public `official-senders` catalogue: the sender identities themselves are public facts (they appear on every official SMS), and the matching itself is performed **locally on the device** by the Rust core — no message content ever leaves the phone.

Each entry records the sender string, the brand it belongs to, and whether it is a verified official channel, so Protect can distinguish "official notice" from "lookalike scam" before warning the user.
