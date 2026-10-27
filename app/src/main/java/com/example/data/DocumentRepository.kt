package com.example.data

import com.example.data.local.DocumentDao
import com.example.model.MarkdownDocument
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DocumentRepository(private val documentDao: DocumentDao) {

    suspend fun checkAndSeedDefaults() {
        val count = documentDao.getDocumentCount()
        if (count == 0) {
            documentDao.insertDocument(DefaultDocuments.initialDocument)
        } else {
            // Update old showcase document if it still contains "Cyberpunk" title
            val showcase = documentDao.getDocumentById("showcase-doc-001")
            if (showcase != null && showcase.title.contains("Cyberpunk", ignoreCase = true)) {
                documentDao.insertDocument(
                    showcase.copy(
                        title = DefaultDocuments.initialDocument.title,
                        content = DefaultDocuments.initialDocument.content
                    )
                )
            }
            // If untouched demo documents graphs-doc-002 or ctan-doc-003 exist, remove them so user starts with clean 1 file
            val graphDoc = documentDao.getDocumentById("graphs-doc-002")
            if (graphDoc != null && graphDoc.title.contains("Cytoscape", ignoreCase = true)) {
                documentDao.deleteDocumentById("graphs-doc-002")
            }
            val ctanDoc = documentDao.getDocumentById("ctan-doc-003")
            if (ctanDoc != null && ctanDoc.title.contains("CTAN", ignoreCase = true)) {
                documentDao.deleteDocumentById("ctan-doc-003")
            }
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
