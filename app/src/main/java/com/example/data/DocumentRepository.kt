package com.example.data

import com.example.data.local.DocumentDao
import com.example.model.MarkdownDocument
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DocumentRepository(private val documentDao: DocumentDao) {

    suspend fun checkAndSeedDefaults() {
        if (documentDao.getDocumentCount() == 0) {
            documentDao.insertAll(
                listOf(
                    DefaultDocuments.initialDocument,
                    DefaultDocuments.graphDocument,
                    DefaultDocuments.ctanLabDocument
                )
            )
        }
    }

    fun getAllDocuments(): Flow<List<MarkdownDocument>> = documentDao.getAllDocuments()

    suspend fun getDocumentList(): List<MarkdownDocument> = documentDao.getDocumentList()

    fun observeDocument(id: String): Flow<MarkdownDocument?> = documentDao.observeDocumentById(id)

    suspend fun getDocument(id: String): MarkdownDocument? = documentDao.getDocumentById(id)

    suspend fun saveDocument(document: MarkdownDocument) {
        val updated = document.copy(updatedAt = System.currentTimeMillis())
        documentDao.insertDocument(updated)
    }

    suspend fun createNewDocument(title: String = "Untitled Document"): MarkdownDocument {
        val newDoc = MarkdownDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            content = "# $title\n\nWrite your thoughts, LaTeX equations, or graphs here...\n",
            installedCtanPackages = "amsmath,physics",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        documentDao.insertDocument(newDoc)
        return newDoc
    }

    suspend fun deleteDocument(id: String) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun togglePin(document: MarkdownDocument) {
        val updated = document.copy(
            isPinned = !document.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        documentDao.insertDocument(updated)
    }
}
