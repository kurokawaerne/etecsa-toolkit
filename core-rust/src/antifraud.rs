//! Antifraud sender matching.
//!
//! The operator and the payment platforms send from a small, known
//! set of alphanumeric/numeric sender ids. Anything that *claims*
//! one of those brands but does not come from the official id is
//! treated as impersonation — the classic Cuban SMS scam pattern
//! ("ETECSA" prizes, fake PAGOxMOVIL receipts, ENZONA links).
//!
//! The built-in catalogue mirrors the publisher-signed
//! `official-senders-v2.json` (catalogue v4) that the original app
//! downloaded from roclaussd.roclahy.com; the app can pass a fresher
//! catalogue to [`is_official_sender`] at runtime.

/// One official sender id and the brand it belongs to.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct OfficialSender {
    pub sender: &'static str,
    pub brand: &'static str,
}

/// Built-in official senders (sender id, brand).
pub const OFFICIAL_SENDERS: &[OfficialSender] = &[
    OfficialSender { sender: "ETECSA", brand: "ETECSA" },
    OfficialSender { sender: "383272", brand: "ETECSA" },
    OfficialSender { sender: "Cubacel", brand: "Cubacel" },
    OfficialSender { sender: "2822235", brand: "Cubacel" },
    OfficialSender { sender: "PAGOxMOVIL", brand: "PAGOxMOVIL" },
    OfficialSender { sender: "7246966845", brand: "PAGOxMOVIL" },
    OfficialSender { sender: "ENZONA", brand: "ENZONA" },
    OfficialSender { sender: "369662", brand: "ENZONA" },
    OfficialSender { sender: "Nauta", brand: "Nauta" },
    OfficialSender { sender: "62882", brand: "Nauta" },
    OfficialSender { sender: "Soberania", brand: "Soberania" },
    OfficialSender { sender: "762372642", brand: "Soberania" },
    OfficialSender { sender: "FACEBOOK", brand: "Facebook" },
    OfficialSender { sender: "32232665", brand: "Facebook" },
    OfficialSender { sender: "*2266", brand: "Atencion al cliente" },
];

/// Brand keywords a scammer would claim in a sender id or a body.
const BRAND_WORDS: &[&str] = &[
    "etecsa",
    "cubacel",
    "pagoxmovil",
    "pago por movil",
    "enzona",
    "nauta",
    "transfermovil",
    "banco central",
];

/// Normalise a sender id for comparison: trim, drop spaces/dashes,
/// uppercase, and drop a leading "+53" (Cuba) or "53" country prefix
/// on long numeric ids so "+53 5 123 4567" and "51234567" compare
/// equal. Alphanumeric ids are compared case-insensitively.
fn normalize_sender(sender: &str) -> String {
    let mut s: String = sender
        .trim()
        .chars()
        .filter(|c| !c.is_whitespace() && *c != '-')
        .collect::<String>()
        .to_uppercase();
    if s.chars().all(|c| c.is_ascii_digit() || c == '+' || c == '*') {
        if let Some(rest) = s.strip_prefix("+53") {
            s = rest.to_string();
        } else if s.len() > 8 {
            if let Some(rest) = s.strip_prefix("53") {
                s = rest.to_string();
            }
        }
    }
    s
}

/// Is `sender` one of the `catalogue` ids (exact, after
/// normalisation)? No prefix/substring matching — "ETECSA-CU" is
/// not ETECSA.
pub fn is_official_sender(sender: &str, catalogue: &[OfficialSender]) -> bool {
    let n = normalize_sender(sender);
    if n.is_empty() {
        return false;
    }
    catalogue
        .iter()
        .any(|o| normalize_sender(o.sender) == n)
}

/// Convenience wrapper over the built-in [`OFFICIAL_SENDERS`].
pub fn is_official_sender_default(sender: &str) -> bool {
    is_official_sender(sender, OFFICIAL_SENDERS)
}

/// Heuristic impersonation check for one incoming SMS.
///
/// True when the message trades on an operator/payment brand but
/// does not come from that brand's official sender id:
/// * the sender id itself wears a brand name (and is not official),
/// * the body claims a brand while the sender is not official,
/// * the body carries a URL and claims a brand / talks about saldo,
///   recargas or account verification, from an unofficial sender.
///
/// Official senders are never flagged, and ordinary person-to-person
/// messages without brand claims are never flagged.
pub fn looks_like_impersonation(sender: &str, body: &str) -> bool {
    if is_official_sender_default(sender) {
        return false;
    }
    let sender_norm = normalize_sender(sender).to_lowercase();
    let body_lower = body.to_lowercase();

    let sender_claims_brand = BRAND_WORDS
        .iter()
        .any(|b| sender_norm.contains(&b.replace(' ', "")));
    if sender_claims_brand {
        return true;
    }

    let body_claims_brand = BRAND_WORDS.iter().any(|b| body_lower.contains(b));
    let has_url = body_lower.contains("http://")
        || body_lower.contains("https://")
        || body_lower.contains("www.")
        || body_lower.contains(".cu/")
        || body_lower.contains(".com/");

    if body_claims_brand {
        // A brand-claiming message from an unofficial sender is
        // impersonation, with or without a link.
        return true;
    }
    if has_url
        && (body_lower.contains("saldo")
            || body_lower.contains("recarga")
            || body_lower.contains("cuenta")
            || body_lower.contains("verific"))
    {
        return true;
    }
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn official_alpha_and_numeric_ids() {
        assert!(is_official_sender("ETECSA", OFFICIAL_SENDERS));
        assert!(is_official_sender("etecsa", OFFICIAL_SENDERS));
        assert!(is_official_sender("383272", OFFICIAL_SENDERS));
        assert!(is_official_sender("Cubacel", OFFICIAL_SENDERS));
        assert!(is_official_sender("2822235", OFFICIAL_SENDERS));
        assert!(is_official_sender("PAGOxMOVIL", OFFICIAL_SENDERS));
        assert!(is_official_sender("7246966845", OFFICIAL_SENDERS));
        assert!(is_official_sender("ENZONA", OFFICIAL_SENDERS));
        assert!(is_official_sender("369662", OFFICIAL_SENDERS));
        assert!(is_official_sender("Nauta", OFFICIAL_SENDERS));
        assert!(is_official_sender("62882", OFFICIAL_SENDERS));
    }

    #[test]
    fn near_miss_ids_are_not_official() {
        assert!(!is_official_sender("ETECSA-CU", OFFICIAL_SENDERS));
        assert!(!is_official_sender("ETECSAA", OFFICIAL_SENDERS));
        assert!(!is_official_sender("383273", OFFICIAL_SENDERS));
        assert!(!is_official_sender("51234567", OFFICIAL_SENDERS));
        assert!(!is_official_sender("", OFFICIAL_SENDERS));
    }

    #[test]
    fn custom_catalogue_is_honoured() {
        let cat = [OfficialSender { sender: "MITIENDA", brand: "MiTienda" }];
        assert!(is_official_sender("mitienda", &cat));
        assert!(!is_official_sender("ETECSA", &cat));
    }

    #[test]
    fn official_sender_is_never_impersonation() {
        assert!(!looks_like_impersonation(
            "Cubacel",
            "Cubacel: ha recibido una recarga de 100 CUP"
        ));
        assert!(!looks_like_impersonation(
            "383272",
            "ETECSA informa: consulte su saldo en https://www.etecsa.cu"
        ));
    }

    #[test]
    fn brand_body_from_unknown_sender() {
        assert!(looks_like_impersonation(
            "51234567",
            "ETECSA: Felicidades, gano un premio. Reclame su saldo ahora"
        ));
        assert!(looks_like_impersonation(
            "+5351234567",
            "PAGOxMOVIL: su transferencia fue recibida"
        ));
    }

    #[test]
    fn brand_wearing_sender_id() {
        assert!(looks_like_impersonation("ETECSA-CU", "Su premio le espera"));
        assert!(looks_like_impersonation("Cubacel5G", "Recarga gratis hoy"));
    }

    #[test]
    fn url_scams() {
        // Operator-like lure: brand + link, unofficial sender.
        assert!(looks_like_impersonation(
            "55667788",
            "ENZONA: verifique su cuenta en http://enzona.cu.fake.com/login"
        ));
        // No brand named, but a link plus account/saldo bait.
        assert!(looks_like_impersonation(
            "55667788",
            "Su saldo expira hoy. Verifique su cuenta: www.recarga-rapida.cu/entrar"
        ));
    }

    #[test]
    fn ordinary_messages_are_clean() {
        assert!(!looks_like_impersonation("51234567", "Hola, nos vemos a las 5"));
        assert!(!looks_like_impersonation(
            "Mama",
            "Recuerda comprar el pan cuando salgas"
        ));
        assert!(!looks_like_impersonation(
            "87654321",
            "Te envie el enlace de la tienda: https://www.ejemplo.com/oferta"
        ));
    }
}
