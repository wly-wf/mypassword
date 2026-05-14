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
        put("title", title)
        put("username", username)
        put("password", password)
        put("url", url)
        put("note", note)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupEntry = BackupEntry(
            title = json.getString("title"),
            username = json.getString("username"),
            password = json.getString("password"),
            url = json.optString("url", ""),
            note = json.optString("note", ""),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt")
        )
    }
}

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val entries: List<BackupEntry> = emptyList()
) {
    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("exportedAt", exportedAt)
        put("entries", JSONArray().also { arr ->
            entries.forEach { arr.put(it.toJson()) }
        })
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): BackupData {
            val json = JSONObject(jsonStr)
            val entriesArray = json.getJSONArray("entries")
            val entries = (0 until entriesArray.length()).map {
                BackupEntry.fromJson(entriesArray.getJSONObject(it))
            }
            return BackupData(
                version = json.optInt("version", 1),
                exportedAt = json.optLong("exportedAt", System.currentTimeMillis()),
                entries = entries
            )
        }
    }
}
