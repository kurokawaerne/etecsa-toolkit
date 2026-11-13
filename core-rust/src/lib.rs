//! toolkit-core: the pure-Rust heart of ETECSA Toolkit.
//!
//! Two jobs, both correctness-critical and both pure functions of
//! their inputs (no I/O, no allocation-heavy machinery, no external
//! crates), so they can be unit-tested exhaustively here and called
//! from Kotlin through the thin JNI layer in `jni_bindings`:
//!
//! * `parser` — reads ETECSA/Cubacel USSD and SMS responses in
//!   Spanish and extracts the resources they report (saldo, datos,
//!   voz, SMS, bonos, roaming), recognising transfer confirmations
//!   and plan purchases.
//! * `antifraud` — decides whether a sender is an official
//!   ETECSA/Cubacel/PAGOxMOVIL/ENZONA sender and whether a message
//!   looks like impersonation of one.

pub mod antifraud;
pub mod parser;

#[cfg(feature = "jni")]
mod jni_bindings;

pub use antifraud::{is_official_sender, looks_like_impersonation, OfficialSender, OFFICIAL_SENDERS};
pub use parser::{parse_response, Kind, ParsedResponse};

/// Serialise a [`ParsedResponse`] as a JSON object, hand-rolled (the
/// crate deliberately has no serde dependency). Field names are the
/// Kotlin-side contract; absent values are `null`.
pub fn response_to_json(r: &ParsedResponse) -> String {
    fn num_f(v: Option<f64>) -> String {
        match v {
            Some(x) => format!("{x}"),
            None => "null".to_string(),
        }
    }
    fn num_u64(v: Option<u64>) -> String {
        match v {
            Some(x) => format!("{x}"),
            None => "null".to_string(),
        }
    }
    fn num_u32(v: Option<u32>) -> String {
        match v {
            Some(x) => format!("{x}"),
            None => "null".to_string(),
        }
    }
    fn string(v: &Option<String>) -> String {
        match v {
            Some(s) => format!("\"{}\"", json_escape(s)),
            None => "null".to_string(),
        }
    }
    format!(
        "{{\"kind\":\"{}\",\"balance_cup\":{},\"data_bytes\":{},\"data_validity\":{},\"voice_seconds\":{},\"sms_count\":{},\"bonus_bytes\":{},\"roaming_cup\":{},\"transfer_amount_cup\":{},\"transfer_number\":{},\"plan_name\":{}}}",
        r.raw_kind.as_str(),
        num_f(r.balance_cup),
        num_u64(r.data_bytes),
        string(&r.data_validity),
        num_u64(r.voice_seconds),
        num_u32(r.sms_count),
        num_u64(r.bonus_bytes),
        num_f(r.roaming_cup),
        num_f(r.transfer_amount_cup),
        string(&r.transfer_number),
        string(&r.plan_name),
    )
}

fn json_escape(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => out.push_str(&format!("\\u{:04x}", c as u32)),
            c => out.push(c),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn json_of_empty_response() {
        let r = parse_response("hola");
        let j = response_to_json(&r);
        assert!(j.contains("\"kind\":\"unknown\""), "{j}");
        assert!(j.contains("\"balance_cup\":null"), "{j}");
    }

    #[test]
    fn json_escapes_quotes_in_plan_name() {
        let r = parse_response("Ha comprado Plan \"Especial\" de datos.");
        let j = response_to_json(&r);
        assert!(j.contains("\\\"Especial\\\""), "{j}");
    }
}
