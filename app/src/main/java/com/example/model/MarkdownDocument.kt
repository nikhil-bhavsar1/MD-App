package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "markdown_documents")
data class MarkdownDocument(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val installedCtanPackages: String = "amsmath,physics", // Comma-separated package IDs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tags: String = "markdown,latex"
) {
    fun getInstalledPackageList(): List<String> {
        return installedCtanPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
