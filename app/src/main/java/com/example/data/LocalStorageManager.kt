package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.MarkdownDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalVaultFileInfo(
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val absolutePath: String
) {
    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
                else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
            }
        }

    val formattedDate: String
        get() {
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))
        }
}

class LocalStorageManager(private val context: Context) {

    /**
     * Directory on device storage where Markdown files are stored as plain text .md files.
     * Uses app-specific external files dir if available, falling back to internal files dir.
     */
    fun getVaultDirectory(): File {
        val externalVault = context.getExternalFilesDir("vault")
        val dir = externalVault ?: File(context.filesDir, "vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Sanitizes a document title to be a safe filesystem filename.
     */
    fun sanitizeFilename(title: String): String {
        val clean = title.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "Untitled" }
        return if (clean.endsWith(".md", ignoreCase = true)) clean else "$clean.md"
    }

    /**
     * Saves a markdown document directly as a .md file on the local filesystem.
     */
    suspend fun saveToVault(document: MarkdownDocument): File = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory()
        val filename = sanitizeFilename(document.title)
        val file = File(dir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(document.content)
            }
        }
        file
    }

    /**
     * Syncs all documents in the database to the local vault as individual .md files.
     */
    suspend fun syncAllToVault(documents: List<MarkdownDocument>): List<File> = withContext(Dispatchers.IO) {
        documents.map { doc ->
            saveToVault(doc)
        }
    }

    /**
     * Reads a .md file from the vault by filename.
     */
    suspend fun readFromVault(filename: String): String? = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory()
        val file = File(dir, sanitizeFilename(filename))
        if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    /**
     * Lists all .md files in the vault with metadata.
     */
    suspend fun listVaultFiles(): List<LocalVaultFileInfo> = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory()
        val files = dir.listFiles { f -> f.isFile && (f.extension.equals("md", ignoreCase = true) || f.extension.equals("txt", ignoreCase = true)) }
            ?: emptyArray()

        files.map { file ->
            LocalVaultFileInfo(
                name = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                absolutePath = file.absolutePath
            )
        }.sortedByDescending { it.lastModified }
    }

    /**
     * Reads text from any SAF Uri (selected via Storage Access Framework file picker).
     * Returns Pair(DocumentTitle, Content).
     */
    suspend fun readTextFromUri(uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        var displayName = "Imported Document"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        displayName = name.removeSuffix(".md").removeSuffix(".markdown").removeSuffix(".txt")
                    }
                }
            }
        }

        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }

        Pair(displayName, stringBuilder.toString())
    }

    /**
     * Writes markdown text to any SAF Uri (selected via CreateDocument picker).
     */
    suspend fun writeTextToUri(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads raw content of a specific file on disk.
     */
    suspend fun readFileContent(file: File): String = withContext(Dispatchers.IO) {
        file.readText(Charsets.UTF_8)
    }

    /**
     * Deletes a .md file from the local vault.
     */
    suspend fun deleteFromVault(filename: String): Boolean = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory()
        val file = File(dir, sanitizeFilename(filename))
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
