package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val isPrivate: Boolean = false
)

@Entity(tableName = "osint_reports")
data class OsintReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val target: String,
    val rawData: String,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vpn_profiles")
data class VpnProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val serverHost: String,
    val serverPort: Int = 51820,
    val protocol: String = "WIREGUARD",
    val countryCode: String = "BR",
    val username: String = "",
    val dnsServer: String = "1.1.1.1",
    val isDefault: Boolean = false
)
