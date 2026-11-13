//! JNI bindings for the Kotlin `RustCore` object.
//!
//! Kotlin side (app module, package `dev.kurokawa.etecsa.toolkit`):
//!
//! ```kotlin
//! object RustCore {
//!     external fun nativeParseResponse(text: String): String
//!     external fun nativeIsOfficialSender(sender: String): Boolean
//!     external fun nativeLooksLikeImpersonation(sender: String, body: String): Boolean
//! }
//! ```
//!
//! Compiled only with `--features jni`. Every function is a thin,
//! panic-free wrapper over the pure functions in the crate root:
//! results cross the boundary as JSON / booleans, and the parse
//! entry point falls back to the JSON of an empty (Unknown)
//! response on any JNI error — nothing may panic across JNI, an
//! unwind into the JVM is undefined behaviour.

use jni::objects::{JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;

use crate::{
    is_official_sender, looks_like_impersonation, parse_response, response_to_json,
    ParsedResponse, OFFICIAL_SENDERS,
};

fn read_string(env: &mut JNIEnv, s: &JString) -> Option<String> {
    env.get_string(s).ok().map(|j| j.to_string_lossy().into_owned())
}

#[no_mangle]
pub extern "system" fn Java_dev_kurokawa_etecsa_toolkit_RustCore_nativeParseResponse<'local>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    text: JString<'local>,
) -> JString<'local> {
    let json = match read_string(&mut env, &text) {
        Some(t) => response_to_json(&parse_response(&t)),
        None => response_to_json(&ParsedResponse::default()),
    };
    match env.new_string(json) {
        Ok(s) => s,
        Err(_) => {
            let _ = env.exception_clear();
            env.new_string("{}")
                .expect("failed to create fallback Java string")
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_kurokawa_etecsa_toolkit_RustCore_nativeIsOfficialSender<'local>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    sender: JString<'local>,
) -> jboolean {
    match read_string(&mut env, &sender) {
        Some(s) => is_official_sender(&s, OFFICIAL_SENDERS) as jboolean,
        None => 0,
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_kurokawa_etecsa_toolkit_RustCore_nativeLooksLikeImpersonation<
    'local,
>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    sender: JString<'local>,
    body: JString<'local>,
) -> jboolean {
    let (Some(s), Some(b)) = (read_string(&mut env, &sender), read_string(&mut env, &body))
    else {
        return 0;
    };
    looks_like_impersonation(&s, &b) as jboolean
}
