package com.mypassword.app.data.crypto

import org.json.JSONArray
import org.json.JSONObject

data class BackupEntry(
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title); put("username", username); put("password", password)
        put("url", url); put("note", note)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupEntry = BackupEntry(
            title = json.getString("title"), username = json.getString("username"),
            password = json.getString("password"), url = json.optString("url", ""),
            note = json.optString("note", ""),
            createdAt = json.getLong("createdAt"), updatedAt = json.getLong("updatedAt")
        )
    }
}

data class BackupAddress(
    val title: String,
    val type: String,
    val address: String,
    val name: String,
    val phone: String,
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title); put("type", type); put("address", address)
        put("name", name); put("phone", phone); put("note", note)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupAddress = BackupAddress(
            title = json.getString("title"), type = json.getString("type"),
            address = json.getString("address"), name = json.optString("name", ""),
            phone = json.optString("phone", ""), note = json.optString("note", ""),
            createdAt = json.getLong("createdAt"), updatedAt = json.getLong("updatedAt")
        )
    }
}

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val entries: List<BackupEntry> = emptyList(),
    val addresses: List<BackupAddress> = emptyList()
) {
    private fun JSONArray.addAll(items: List<JSONObject>) {
        items.forEach { put(it) }
    }

    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("exportedAt", exportedAt)
        put("entries", JSONArray().also { arr -> entries.forEach { arr.put(it.toJson()) } })
        put("addresses", JSONArray().also { arr -> addresses.forEach { arr.put(it.toJson()) } })
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): BackupData {
            val json = JSONObject(jsonStr)
            fun parseArray(key: String) = if (json.has(key)) (0 until json.getJSONArray(key).length()).map { json.getJSONArray(key).getJSONObject(it) } else emptyList()
            return BackupData(
                version = json.optInt("version", 1),
                exportedAt = json.optLong("exportedAt", System.currentTimeMillis()),
                entries = parseArray("entries").map { BackupEntry.fromJson(it) },
                addresses = parseArray("addresses").map { BackupAddress.fromJson(it) }
            )
        }
    }
}
