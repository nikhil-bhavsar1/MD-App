package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.MarkdownDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM markdown_documents ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllDocuments(): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getDocumentList(): List<MarkdownDocument>

    @Query("SELECT COUNT(*) FROM markdown_documents")
    suspend fun getDocumentCount(): Int

    @Query("SELECT * FROM markdown_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): MarkdownDocument?

    @Query("SELECT * FROM markdown_documents WHERE id = :id LIMIT 1")
    fun observeDocumentById(id: String): Flow<MarkdownDocument?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MarkdownDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<MarkdownDocument>)

    @Update
    suspend fun updateDocument(document: MarkdownDocument)

    @Delete
    suspend fun deleteDocument(document: MarkdownDocument)

    @Query("DELETE FROM markdown_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}
