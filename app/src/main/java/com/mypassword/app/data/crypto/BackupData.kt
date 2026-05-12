package com.mypassword.app.data.crypto

import kotlinx.serialization.Serializable

@Serializable
data class BackupEntry(
    val type: String,
    val target: String,
    val username: String,
    val password: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val entries: List<BackupEntry> = emptyList()
)
