package dev.kurokawa.etecsa.toolkit.platform

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.kurokawa.etecsa.toolkit.core.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** SQLite (Android built-in — zero added weight) + SharedPreferences +
 *  AndroidKeyStore AES-GCM secrets. The whole local data layer. */
class EtkStore(private val ctx: Context) : LocalStore {
    private val prefs = ctx.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
    private val db = object : SQLiteOpenHelper(ctx, "etk.db", null, 1) {
        override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
            db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, title TEXT, body TEXT, ts INTEGER, slot INTEGER)")
            db.execSQL("CREATE TABLE blocked(number TEXT PRIMARY KEY)")
            db.execSQL("CREATE TABLE friends(number TEXT PRIMARY KEY, name TEXT, photo TEXT)")
            db.execSQL("CREATE TABLE cards(label TEXT PRIMARY KEY, masked TEXT)")
            db.execSQL("CREATE TABLE sms_queue(id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT, body TEXT, slot INTEGER, on_cov INTEGER)")
        }
        override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, o: Int, n: Int) {}
    }

    override fun addHistory(kind: String, title: String, body: String, slot: Int) {
        try {
            val v = ContentValues().apply {
                put("kind", kind); put("title", title); put("body", body)
                put("ts", System.currentTimeMillis()); put("slot", slot)
            }
            db.writableDatabase.insert("history", null, v)
            // Widget + quick glance
            if (kind == "ussd") prefs.edit().putString("last_balance", body.take(120)).apply()
        } catch (_: Exception) {}
    }
    override fun history(limit: Int): List<HistoryEntry> = try {
        val out = mutableListOf<HistoryEntry>()
        db.readableDatabase.query("history", arrayOf("id","kind","title","body","ts","slot"),
            null, null, null, null, "id DESC", limit.toString()).use { c ->
            while (c.moveToNext()) out += HistoryEntry(c.getLong(0), c.getString(1) ?: "",
                c.getString(2) ?: "", c.getString(3) ?: "", c.getLong(4), c.getInt(5))
        }; out
    } catch (_: Exception) { emptyList() }
    override fun clearHistory() { try { db.writableDatabase.delete("history", null, null) } catch (_: Exception) {} }

    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }

    override fun blockedNumbers(): Set<String> = try {
        val out = mutableSetOf<String>()
        db.readableDatabase.query("blocked", arrayOf("number"), null, null, null, null, null).use { c ->
            while (c.moveToNext()) out += c.getString(0)
        }; out
    } catch (_: Exception) { emptySet() }
    override fun setBlocked(number: String, blocked: Boolean) {
        try {
            if (blocked) {
                val v = ContentValues().apply { put("number", number) }
                db.writableDatabase.insertWithOnConflict("blocked", null, v,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
            } else db.writableDatabase.delete("blocked", "number=?", arrayOf(number))
        } catch (_: Exception) {}
    }

    override fun friends(): List<ContactInfo> = try {
        val out = mutableListOf<ContactInfo>()
        db.readableDatabase.query("friends", arrayOf("name","number","photo"), null, null, null, null, "name").use { c ->
            while (c.moveToNext()) out += ContactInfo(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2))
        }; out
    } catch (_: Exception) { emptyList() }
    override fun setFriend(contact: ContactInfo, add: Boolean) {
        try {
            if (add) {
                val v = ContentValues().apply {
                    put("number", contact.number); put("name", contact.name); put("photo", contact.photoUri)
                }
                db.writableDatabase.insertWithOnConflict("friends", null, v,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            } else db.writableDatabase.delete("friends", "number=?", arrayOf(contact.number))
        } catch (_: Exception) {}
    }

    override fun saveCard(label: String, number: String) {
        putSecret("card_$label", number)
        try {
            val masked = "•••• " + number.takeLast(4)
            val v = ContentValues().apply { put("label", label); put("masked", masked) }
            db.writableDatabase.insertWithOnConflict("cards", null, v,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Exception) {}
    }
    override fun cards(): List<Pair<String, String>> = try {
        val out = mutableListOf<Pair<String, String>>()
        db.readableDatabase.query("cards", arrayOf("label","masked"), null, null, null, null, "label").use { c ->
            while (c.moveToNext()) out += (c.getString(0) ?: "") to (c.getString(1) ?: "")
        }; out
    } catch (_: Exception) { emptyList() }
    override fun deleteCard(label: String) {
        try { db.writableDatabase.delete("cards", "label=?", arrayOf(label)) } catch (_: Exception) {}
        prefs.edit().remove("secret_card_$label").apply()
    }

    override fun queueSms(number: String, text: String, slot: Int, onCoverage: Boolean) {
        try {
            val v = ContentValues().apply {
                put("number", number); put("body", text); put("slot", slot); put("on_cov", if (onCoverage) 1 else 0)
            }
            db.writableDatabase.insert("sms_queue", null, v)
        } catch (_: Exception) {}
    }
    override fun queuedSms(): List<Triple<String, String, Boolean>> = try {
        val out = mutableListOf<Triple<String, String, Boolean>>()
        db.readableDatabase.query("sms_queue", arrayOf("number","body","on_cov"), null, null, null, null, "id").use { c ->
            while (c.moveToNext()) out += Triple(c.getString(0) ?: "", c.getString(1) ?: "", c.getInt(2) == 1)
        }; out
    } catch (_: Exception) { emptyList() }
    override fun clearQueuedSms() { try { db.writableDatabase.delete("sms_queue", null, null) } catch (_: Exception) {} }

    // AndroidKeyStore AES-GCM
    private fun secretKey(aliasName: String): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(aliasName, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(aliasName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return gen.generateKey()
    }
    override fun putSecret(key: String, value: String) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey("etk_secrets"))
            val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            prefs.edit().putString("secret_$key", packed).apply()
        } catch (_: Exception) {}
    }
    override fun getSecret(key: String): String? = try {
        val packed = prefs.getString("secret_$key", null) ?: return null
        val (ivB64, dataB64) = packed.split(":", limit = 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey("etk_secrets"),
            GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(dataB64, Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (_: Exception) { null }
}
