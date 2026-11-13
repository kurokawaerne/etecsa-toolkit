//! Parser for ETECSA/Cubacel USSD & SMS responses (Spanish).
//!
//! Hand-rolled scanning, no regex crate: find a resource anchor word
//! ("saldo", "datos", "voz", "sms", "bono", "roaming"), then read the
//! number (and unit) that belongs to it in a short window after — or
//! just before — the anchor. This mirrors the semantics of the regex
//! suite the original app used, e.g.
//! `Saldo\s*:\s*([0-9]+(?:[.,][0-9]+)?\s*CUP)`,
//! `Datos\s*:\s*([0-9]+(?:[.,][0-9]+)?\s*(?:GB|MB|KB))`,
//! `Voz\s*:\s*([0-9:]+)`, `SMS\s*:\s*([0-9]+)`:
//! a `.` or `,` inside a number is its decimal separator.
//!
//! Data volumes use the operator's decimal units: 1 GB = 10^9 bytes,
//! 1 MB = 10^6, 1 KB = 10^3.

/// What a response fundamentally is.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Kind {
    Balance,
    Data,
    Voice,
    Sms,
    TransferConfirmation,
    PlanPurchase,
    Unknown,
}

impl Kind {
    pub fn as_str(&self) -> &'static str {
        match self {
            Kind::Balance => "balance",
            Kind::Data => "data",
            Kind::Voice => "voice",
            Kind::Sms => "sms",
            Kind::TransferConfirmation => "transfer_confirmation",
            Kind::PlanPurchase => "plan_purchase",
            Kind::Unknown => "unknown",
        }
    }
}

/// Everything one response told us. `None` = the response did not
/// mention that resource (distinct from a real zero).
#[derive(Debug, Clone, Default, PartialEq)]
pub struct ParsedResponse {
    pub balance_cup: Option<f64>,
    pub data_bytes: Option<u64>,
    /// Vigencia of the data bundle as written by the operator:
    /// a date ("12/01/2027") or a duration ("30 dias").
    pub data_validity: Option<String>,
    pub voice_seconds: Option<u64>,
    pub sms_count: Option<u32>,
    pub bonus_bytes: Option<u64>,
    pub roaming_cup: Option<f64>,
    pub transfer_amount_cup: Option<f64>,
    /// 8-digit destination number of a confirmed transfer.
    pub transfer_number: Option<String>,
    pub plan_name: Option<String>,
    pub raw_kind: Kind,
}

impl Default for Kind {
    fn default() -> Self {
        Kind::Unknown
    }
}

/// Parse one USSD/SMS response text.
pub fn parse_response(text: &str) -> ParsedResponse {
    let lower = text.to_lowercase();
    let mut out = ParsedResponse::default();

    // Special message shapes first: they carry their own semantics
    // and their numbers are NOT the account balance.
    if let Some((amount, number)) = parse_transfer(&lower) {
        out.raw_kind = Kind::TransferConfirmation;
        out.transfer_amount_cup = Some(amount);
        out.transfer_number = Some(number);
        // A transfer confirmation often appends the new balance;
        // keep it if the operator stated one.
        out.balance_cup = parse_balance(&lower);
        return out;
    }
    if let Some(plan) = parse_plan_purchase(text, &lower) {
        out.raw_kind = Kind::PlanPurchase;
        out.plan_name = Some(plan);
        out.balance_cup = parse_balance(&lower);
        out.data_bytes = parse_anchored_volume(&lower, &["datos"]);
        return out;
    }

    out.balance_cup = parse_balance(&lower);
    let (data, validity) = parse_data(&lower);
    out.data_bytes = data;
    out.data_validity = validity;
    out.voice_seconds = parse_voice(&lower);
    out.sms_count = parse_sms(&lower);
    out.bonus_bytes = parse_anchored_volume(&lower, &["bono", "bonos"]);
    out.roaming_cup = parse_roaming(&lower);

    out.raw_kind = if out.balance_cup.is_some() {
        Kind::Balance
    } else if out.data_bytes.is_some() {
        Kind::Data
    } else if out.voice_seconds.is_some() {
        Kind::Voice
    } else if out.sms_count.is_some() {
        Kind::Sms
    } else {
        Kind::Unknown
    };
    out
}

// ---------- number primitives ----------

/// First number in `s`: digits with one optional `.`/`,` decimal
/// separator. Returns the value and the remainder after the number.
fn scan_number(s: &str) -> Option<(f64, &str)> {
    let bytes = s.as_bytes();
    let start = bytes.iter().position(|b| b.is_ascii_digit())?;
    let mut end = start;
    while end < bytes.len() && bytes[end].is_ascii_digit() {
        end += 1;
    }
    // Decimal part: one separator followed by at least one digit.
    if end + 1 < bytes.len()
        && (bytes[end] == b'.' || bytes[end] == b',')
        && bytes[end + 1].is_ascii_digit()
    {
        end += 1;
        while end < bytes.len() && bytes[end].is_ascii_digit() {
            end += 1;
        }
    }
    let raw = s[start..end].replace(',', ".");
    raw.parse::<f64>().ok().map(|v| (v, &s[end..]))
}

/// A duration token at the start of `s` (after blanks): `HH:MM:SS`,
/// `MM:SS`, or a bare count of minutes when followed by "min".
fn scan_duration(s: &str) -> Option<u64> {
    // Skip blanks and the ':' that separates an anchor from its value
    // ("Voz: 01:15:00"); inner colons are not leading, so they stay.
    let t = s.trim_start_matches([' ', '\t', ':']);
    // Colon form first: digit groups joined by ':'.
    if t.chars().next().is_some_and(|c| c.is_ascii_digit()) && t.contains(':') {
        let token: String = t
            .chars()
            .take_while(|c| c.is_ascii_digit() || *c == ':')
            .collect();
        let parts: Vec<&str> = token.split(':').collect();
        if (2..=3).contains(&parts.len()) && parts.iter().all(|p| !p.is_empty()) {
            let nums: Option<Vec<u64>> = parts.iter().map(|p| p.parse().ok()).collect();
            if let Some(n) = nums {
                return Some(if n.len() == 3 {
                    n[0] * 3600 + n[1] * 60 + n[2]
                } else {
                    n[0] * 60 + n[1]
                });
            }
        }
    }
    None
}

/// Byte size of `number + unit` at the start of `rest` (blanks
/// skipped). Units are matched case-insensitively on the already
/// lowercased text: gb / mb / kb.
fn scan_volume_after_number(number: f64, rest: &str) -> Option<u64> {
    let r = rest.trim_start();
    let mult = if r.starts_with("gb") {
        1_000_000_000f64
    } else if r.starts_with("mb") {
        1_000_000f64
    } else if r.starts_with("kb") {
        1_000f64
    } else {
        return None;
    };
    Some((number * mult).round() as u64)
}

/// The slice of `lower` to search for an anchor's value: up to
/// `window` bytes starting at byte index `from`, cut at a sentence
/// or line end so one resource never swallows the next one's value.
fn window(lower: &str, from: usize, window: usize) -> &str {
    let end = (from + window).min(lower.len());
    let mut cut = end;
    // Back off to a char boundary.
    while cut > from && !lower.is_char_boundary(cut) {
        cut -= 1;
    }
    let s = &lower[from..cut];
    match s.find(['\n', ';']) {
        Some(i) => &s[..i],
        None => s,
    }
}

/// Number+volume for the first `anchor` occurrence whose following
/// window actually holds a volume ("Datos: 2 GB"), else the volume
/// immediately before the anchor ("le quedan 2 GB de datos").
fn parse_anchored_volume(lower: &str, anchors: &[&str]) -> Option<u64> {
    for anchor in anchors {
        let mut search_from = 0;
        while let Some(rel) = lower[search_from..].find(anchor) {
            let at = search_from + rel;
            let after = window(lower, at + anchor.len(), 48);
            if let Some((n, rest)) = scan_number(after) {
                if let Some(v) = scan_volume_after_number(n, rest) {
                    return Some(v);
                }
            }
            // "2 GB de datos": number ends within 16 chars before.
            let before_start = at.saturating_sub(20);
            let before = &lower[before_start..at];
            if let Some(v) = last_volume_in(before) {
                return Some(v);
            }
            search_from = at + anchor.len();
        }
    }
    None
}

/// Last `<number><unit>` inside `s` ("… 512 MB de ").
fn last_volume_in(s: &str) -> Option<u64> {
    let mut rest = s;
    let mut found = None;
    while let Some((n, tail)) = scan_number(rest) {
        if let Some(v) = scan_volume_after_number(n, tail) {
            found = Some(v);
        }
        // Advance past this number to look for a later one.
        let advanced = tail.len();
        if advanced == rest.len() {
            break;
        }
        rest = tail;
    }
    found
}

// ---------- resources ----------

/// Saldo principal in CUP. "Saldo principal" wins over a bare
/// "saldo"; a saldo only counts when its number is denominated in
/// CUP (so "saldo de datos" never becomes money).
fn parse_balance(lower: &str) -> Option<f64> {
    for anchor in ["saldo principal", "saldo disponible", "saldo"] {
        let mut search_from = 0;
        while let Some(rel) = lower[search_from..].find(anchor) {
            let at = search_from + rel;
            // Roaming is reported separately; skip its saldo here.
            let after = window(lower, at + anchor.len(), 48);
            if !after.trim_start().starts_with("roaming") {
                if let Some((n, rest)) = scan_number(after) {
                    if rest.trim_start().starts_with("cup") {
                        return Some(n);
                    }
                }
            }
            search_from = at + anchor.len();
        }
    }
    None
}

fn parse_roaming(lower: &str) -> Option<f64> {
    let at = lower.find("roaming")?;
    let after = window(lower, at + "roaming".len(), 48);
    let (n, rest) = scan_number(after)?;
    if rest.trim_start().starts_with("cup") {
        Some(n)
    } else if after.contains("cup") {
        Some(n)
    } else {
        None
    }
}

fn parse_data(lower: &str) -> (Option<u64>, Option<String>) {
    let bytes = parse_anchored_volume(lower, &["datos", "data"]);
    if bytes.is_none() {
        return (None, None);
    }
    // Vigencia: an explicit "vence <date>" anywhere, else a
    // "<n> dias" duration in the window right after the volume.
    let mut validity = None;
    if let Some(at) = lower.find("vence") {
        let after = window(lower, at + "vence".len(), 32);
        if let Some((_, _)) = scan_number(after) {
            // Capture the date token: digits and its separators.
            let t = after.trim_start();
            let token: String = t
                .chars()
                .take_while(|c| c.is_ascii_digit() || matches!(c, '/' | '-' | '.'))
                .collect();
            if token.chars().any(|c| c.is_ascii_digit()) {
                validity = Some(token);
            }
        }
    }
    if validity.is_none() {
        if let Some(at) = lower.find("dia") {
            // Walk back to the number that counts the days.
            let before = &lower[at.saturating_sub(8)..at];
            if let Some((n, _)) = scan_number(before) {
                if n >= 1.0 {
                    validity = Some(format!("{} dias", n as u64));
                }
            }
        }
    }
    (bytes, validity)
}

fn parse_voice(lower: &str) -> Option<u64> {
    for anchor in ["voz", "minutos", "min"] {
        let mut search_from = 0;
        while let Some(rel) = lower[search_from..].find(anchor) {
            let at = search_from + rel;
            let after = window(lower, at + anchor.len(), 40);
            if let Some(d) = scan_duration(after) {
                return Some(d);
            }
            if let Some((n, _)) = scan_number(after) {
                // A bare number after "voz"/"minutos" is minutes.
                return Some((n * 60.0).round() as u64);
            }
            search_from = at + anchor.len();
        }
    }
    // "le quedan 45 minutos": number right before the word.
    if let Some(at) = lower.find("minutos") {
        let before = &lower[at.saturating_sub(12)..at];
        if let Some((n, _)) = scan_number(before) {
            return Some((n * 60.0).round() as u64);
        }
    }
    None
}

fn parse_sms(lower: &str) -> Option<u32> {
    for anchor in ["sms", "mensajes"] {
        let mut search_from = 0;
        while let Some(rel) = lower[search_from..].find(anchor) {
            let at = search_from + rel;
            // After the anchor: "SMS: 20", "SMS disponibles: 35".
            let after = window(lower, at + anchor.len(), 32);
            if let Some((n, _)) = scan_number(after) {
                return Some(n as u32);
            }
            // Before the anchor: "le quedan 20 SMS".
            let before = &lower[at.saturating_sub(12)..at];
            if let Some((n, _)) = scan_number(before) {
                return Some(n as u32);
            }
            search_from = at + anchor.len();
        }
    }
    None
}

// ---------- special shapes ----------

/// "Usted ha transferido 250.00 CUP al numero 51234567"
/// -> (250.00, "51234567"). Requires the 8-digit destination.
fn parse_transfer(lower: &str) -> Option<(f64, String)> {
    let at = lower.find("ha transferido")?;
    let after = window(lower, at, 96);
    let (amount, rest) = scan_number(after)?;
    if !rest.contains("cup") && !after.contains("cup") {
        return None;
    }
    // The destination: an 8-digit run after "numero".
    let num_at = rest.find("numero").or_else(|| after.find("numero"))?;
    let hay = if rest.contains("numero") { rest } else { after };
    let _ = num_at;
    let digits: Vec<String> = hay
        .split(|c: char| !c.is_ascii_digit())
        .filter(|t| t.len() == 8)
        .map(|t| t.to_string())
        .collect();
    digits.into_iter().next().map(|n| (amount, n))
}

/// "Ha comprado Plan X ..." / "Se ha comprado el Plan X ..." ->
/// the plan name as written (original casing), trimmed of the
/// trailing sentence ("... vence el ...", final '.').
fn parse_plan_purchase(original: &str, lower: &str) -> Option<String> {
    if !(lower.contains("ha comprado") || lower.contains("ha adquirido")) {
        return None;
    }
    let plan_at = lower.find("plan")?;
    let raw = &original[plan_at + "plan".len()..];
    let mut name = raw.trim_start_matches(|c: char| c == ' ' || c == ':' );
    // Drop a leading article: "el Plan de Datos" -> "de Datos" reads
    // wrong; keep "el" out when it is just the article.
    if let Some(stripped) = name.strip_prefix("el ") {
        name = stripped;
    }
    // Sentence end: a '.' only counts when followed by blank/end
    // (so "4.5GB" inside a plan name is not a cut point).
    let cut = name
        .char_indices()
        .find(|(i, c)| {
            *c == '\n'
                || (*c == '.'
                    && name[i + 1..].chars().next().is_none_or(|n| n == ' '))
        })
        .map(|(i, _)| i)
        .unwrap_or_else(|| name.len().min(64));
    let mut plan = name[..cut].trim().to_string();
    if let Some(v) = plan.to_lowercase().find(" vence") {
        plan.truncate(v);
        plan = plan.trim().to_string();
    }
    if plan.is_empty() {
        None
    } else {
        Some(plan)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn balance_only() {
        let r = parse_response("Saldo principal: 125.50 CUP");
        assert_eq!(r.balance_cup, Some(125.50));
        assert_eq!(r.raw_kind, Kind::Balance);
    }

    #[test]
    fn balance_comma_decimal() {
        let r = parse_response("Su saldo disponible es de 75,25 CUP");
        assert_eq!(r.balance_cup, Some(75.25));
    }

    #[test]
    fn full_consult_all_resources() {
        let r = parse_response(
            "Saldo: 250.00 CUP. Datos: 2.5 GB. Voz: 45 min. SMS: 20. Vence 12/01/2027",
        );
        assert_eq!(r.balance_cup, Some(250.0));
        assert_eq!(r.data_bytes, Some(2_500_000_000));
        assert_eq!(r.voice_seconds, Some(45 * 60));
        assert_eq!(r.sms_count, Some(20));
        assert_eq!(r.data_validity.as_deref(), Some("12/01/2027"));
        assert_eq!(r.raw_kind, Kind::Balance);
    }

    #[test]
    fn data_mb_and_kb() {
        assert_eq!(parse_response("Datos: 512 MB").data_bytes, Some(512_000_000));
        assert_eq!(parse_response("Datos disponibles: 300 KB").data_bytes, Some(300_000));
        assert_eq!(parse_response("Datos: 1,5 GB").data_bytes, Some(1_500_000_000));
        assert_eq!(parse_response("Datos: 512 MB").raw_kind, Kind::Data);
    }

    #[test]
    fn data_number_before_anchor() {
        let r = parse_response("Le quedan 2 GB de datos nacionales");
        assert_eq!(r.data_bytes, Some(2_000_000_000));
    }

    #[test]
    fn data_validity_duration() {
        let r = parse_response("Datos: 1 GB valido por 35 dias");
        assert_eq!(r.data_validity.as_deref(), Some("35 dias"));
    }

    #[test]
    fn voice_minutes_and_colon() {
        assert_eq!(parse_response("Voz: 30 min").voice_seconds, Some(1800));
        assert_eq!(parse_response("Minutos: 120").voice_seconds, Some(7200));
        assert_eq!(parse_response("Voz: 01:15:00").voice_seconds, Some(4500));
        assert_eq!(parse_response("Voz: 12:30").voice_seconds, Some(750));
        assert_eq!(parse_response("Voz: 30 min").raw_kind, Kind::Voice);
    }

    #[test]
    fn sms_counts() {
        assert_eq!(parse_response("SMS: 20").sms_count, Some(20));
        assert_eq!(parse_response("SMS disponibles: 35").sms_count, Some(35));
        assert_eq!(parse_response("Le quedan 15 SMS").sms_count, Some(15));
        assert_eq!(parse_response("Mensajes: 10").sms_count, Some(10));
        assert_eq!(parse_response("SMS: 20").raw_kind, Kind::Sms);
    }

    #[test]
    fn bonus_is_not_main_data() {
        let r = parse_response("Datos: 2 GB. Bono: 300 MB");
        assert_eq!(r.data_bytes, Some(2_000_000_000));
        assert_eq!(r.bonus_bytes, Some(300_000_000));
    }

    #[test]
    fn roaming_separate_from_balance() {
        let r = parse_response("Saldo principal: 100.00 CUP. Saldo roaming: 10.50 CUP");
        assert_eq!(r.balance_cup, Some(100.0));
        assert_eq!(r.roaming_cup, Some(10.50));
    }

    #[test]
    fn transfer_confirmation() {
        let r = parse_response(
            "Usted ha transferido 250.00 CUP al numero 51234567. Saldo: 40.00 CUP",
        );
        assert_eq!(r.raw_kind, Kind::TransferConfirmation);
        assert_eq!(r.transfer_amount_cup, Some(250.0));
        assert_eq!(r.transfer_number.as_deref(), Some("51234567"));
        assert_eq!(r.balance_cup, Some(40.0));
    }

    #[test]
    fn plan_purchase() {
        let r = parse_response("Ha comprado Plan 6 GB con 60 min y 70 SMS. Vence 10/02/2027");
        assert_eq!(r.raw_kind, Kind::PlanPurchase);
        assert_eq!(r.plan_name.as_deref(), Some("6 GB con 60 min y 70 SMS"));
    }

    #[test]
    fn plan_purchase_se_ha_comprado() {
        let r = parse_response("Se ha comprado el Plan de Datos 4.5GB.");
        assert_eq!(r.raw_kind, Kind::PlanPurchase);
        assert!(r.plan_name.as_deref().unwrap().contains("Datos"));
    }

    #[test]
    fn unknown_text() {
        let r = parse_response("El servicio no esta disponible en este momento");
        assert_eq!(r.raw_kind, Kind::Unknown);
        assert_eq!(r.balance_cup, None);
        assert_eq!(r.data_bytes, None);
    }

    #[test]
    fn saldo_de_datos_is_not_money() {
        // No CUP after the number -> not a balance.
        let r = parse_response("Saldo de datos agotado, compre un plan");
        assert_eq!(r.balance_cup, None);
    }
}
