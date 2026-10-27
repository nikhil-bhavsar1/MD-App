package com.example.ui.editor

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultDocuments
import com.example.data.DocumentRepository
import com.example.data.UnicodeRepository
import com.example.data.local.AppDatabase
import com.example.model.EditorFont
import com.example.model.EditorSettings
import com.example.model.MarkdownDocument
import com.example.model.UnicodeItem
import com.example.model.ViewMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.net.Uri
import com.example.data.LocalStorageManager
import com.example.data.LocalVaultFileInfo

data class EditorUiState(
    val activeDocument: MarkdownDocument = DefaultDocuments.initialDocument,
    val textFieldValue: TextFieldValue = TextFieldValue(DefaultDocuments.initialDocument.content),
    val previewText: String = DefaultDocuments.initialDocument.content,
    val viewMode: ViewMode = ViewMode.SPLIT,
    val settings: EditorSettings = EditorSettings(),
    val isCtanSheetOpen: Boolean = false,
    val isGraphDialogOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val isFileTreeGraphOpen: Boolean = false,
    val isSyntaxCatalogOpen: Boolean = false,
    val isUnicodePickerOpen: Boolean = false,
    val isVaultSheetOpen: Boolean = false,
    val isFindReplaceOpen: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchMatchIndices: List<Int> = emptyList(),
    val currentMatchIndex: Int = 0,
    val vaultFiles: List<LocalVaultFileInfo> = emptyList(),
    val vaultDirectoryPath: String = "",
    val statusNotification: String? = null,
    val emojiSuggestions: List<UnicodeItem> = emptyList(),
    val splitRatio: Float = 0.5f
) {
    val editorText: String get() = textFieldValue.text
    val searchMatchCount: Int get() = searchMatchIndices.size
}

@OptIn(FlowPreview::class)
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    val unicodeRepository = UnicodeRepository()
    val localStorageManager = LocalStorageManager(application)
    val allDocuments: StateFlow<List<MarkdownDocument>>

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _textStream = MutableStateFlow(DefaultDocuments.initialDocument.content)

    init {
        val db = AppDatabase.getInstance(application)
        repository = DocumentRepository(db.documentDao())

        _uiState.value = _uiState.value.copy(
            vaultDirectoryPath = localStorageManager.getVaultDirectory().absolutePath
        )

        viewModelScope.launch {
            repository.checkAndSeedDefaults()
            try {
                val docs = repository.getDocumentList()
                localStorageManager.syncAllToVault(docs)
                refreshVaultFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        allDocuments = repository.getAllDocuments()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf(DefaultDocuments.initialDocument)
            )

        viewModelScope.launch {
            allDocuments.collect { docs ->
                if (docs.isNotEmpty() && _uiState.value.activeDocument.id == DefaultDocuments.initialDocument.id) {
                    val matching = docs.firstOrNull { it.id == _uiState.value.activeDocument.id } ?: docs.first()
                    _uiState.value = _uiState.value.copy(
                        activeDocument = matching,
                        textFieldValue = TextFieldValue(matching.content),
                        previewText = matching.content
                    )
                    _textStream.value = matching.content
                }
            }
        }

        // Live preview stream
        _textStream
            .debounce(100)
            .onEach { debouncedText ->
                _uiState.value = _uiState.value.copy(previewText = debouncedText)
            }
            .launchIn(viewModelScope)

        // Database & local vault storage autosave
        _textStream
            .debounce(600)
            .onEach { textToSave ->
                val current = _uiState.value.activeDocument
                if (current.content != textToSave) {
                    val updated = current.copy(content = textToSave)
                    _uiState.value = _uiState.value.copy(activeDocument = updated)
                    repository.saveDocument(updated)
                    try {
                        localStorageManager.saveToVault(updated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTextFieldValueChange(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.textFieldValue.text

        // Check for active emoji/unicode query prefix like :roc or :fir or :inf
        val suggestions = detectEmojiSuggestions(newValue)

        _uiState.value = _uiState.value.copy(
            textFieldValue = newValue,
            emojiSuggestions = suggestions
        )

        if (textChanged) {
            _textStream.value = newValue.text
        }
    }

    private fun detectEmojiSuggestions(tfv: TextFieldValue): List<UnicodeItem> {
        val cursor = tfv.selection.start
        val text = tfv.text
        if (cursor <= 0 || cursor > text.length) return emptyList()

        val textBeforeCursor = text.substring(0, cursor)
        val colonIdx = textBeforeCursor.lastIndexOf(':')
        if (colonIdx >= 0) {
            val query = textBeforeCursor.substring(colonIdx)
            // Only suggest if it's a valid shortcode prefix like :ro without spaces or newlines
            if (!query.contains(' ') && !query.contains('\n') && query.length in 2..15) {
                return unicodeRepository.getSuggestionsForPrefix(query)
            }
        }
        return emptyList()
    }

    fun selectEmojiSuggestion(item: UnicodeItem) {
        val tfv = _uiState.value.textFieldValue
        val cursor = tfv.selection.start
        val text = tfv.text
        val textBeforeCursor = text.substring(0, cursor)
        val colonIdx = textBeforeCursor.lastIndexOf(':')

        if (colonIdx >= 0) {
            val newText = text.substring(0, colonIdx) + item.char + " " + text.substring(cursor)
            val newCursor = colonIdx + item.char.length + 1
            val updated = TextFieldValue(newText, TextRange(newCursor))
            onTextFieldValueChange(updated)
        } else {
            insertText(item.char)
        }
        _uiState.value = _uiState.value.copy(emojiSuggestions = emptyList())
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun updateSettings(newSettings: EditorSettings) {
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    fun setSplitRatio(ratio: Float) {
        _uiState.value = _uiState.value.copy(splitRatio = ratio.coerceIn(0.2f, 0.8f))
    }

    fun toggleCtanSheet(open: Boolean) {
        _uiState.value = _uiState.value.copy(isCtanSheetOpen = open)
    }

    fun toggleGraphDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isGraphDialogOpen = open)
    }

    fun toggleSettingsDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = open)
    }

    fun toggleRenameDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isRenameDialogOpen = open)
    }

    fun toggleFileTreeGraph(open: Boolean) {
        _uiState.value = _uiState.value.copy(isFileTreeGraphOpen = open)
    }

    fun toggleSyntaxCatalog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSyntaxCatalogOpen = open)
    }

    fun toggleUnicodePicker(open: Boolean) {
        _uiState.value = _uiState.value.copy(isUnicodePickerOpen = open)
    }

    fun selectDocument(documentId: String) {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId) ?: return@launch
            _uiState.value = _uiState.value.copy(
                activeDocument = doc,
                textFieldValue = TextFieldValue(doc.content),
                previewText = doc.content
            )
            _textStream.value = doc.content
        }
    }

    fun createNewDocument() {
        viewModelScope.launch {
            val newDoc = repository.createNewDocument("Document ${allDocuments.value.size + 1}")
            _uiState.value = _uiState.value.copy(
                activeDocument = newDoc,
                textFieldValue = TextFieldValue(newDoc.content),
                previewText = newDoc.content
            )
            _textStream.value = newDoc.content
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            val remaining = allDocuments.value.filter { it.id != id }
            if (remaining.isNotEmpty() && _uiState.value.activeDocument.id == id) {
                selectDocument(remaining.first().id)
            }
        }
    }

    fun togglePin(doc: MarkdownDocument) {
        viewModelScope.launch {
            repository.togglePin(doc)
        }
    }

    fun renameDocument(newTitle: String) {
        if (newTitle.isBlank()) return
        val current = _uiState.value.activeDocument
        val updated = current.copy(title = newTitle.trim())
        _uiState.value = _uiState.value.copy(activeDocument = updated)
        viewModelScope.launch {
            repository.saveDocument(updated)
        }
    }

    fun toggleCtanPackage(packageId: String) {
        val currentDoc = _uiState.value.activeDocument
        val currentPackages = currentDoc.getInstalledPackageList().toMutableList()
        if (currentPackages.contains(packageId)) {
            currentPackages.remove(packageId)
        } else {
            currentPackages.add(packageId)
        }
        val updatedDoc = currentDoc.copy(
            installedCtanPackages = currentPackages.joinToString(",")
        )
        _uiState.value = _uiState.value.copy(activeDocument = updatedDoc)
        viewModelScope.launch {
            repository.saveDocument(updatedDoc)
        }
    }

    fun insertText(snippet: String) {
        val tfv = _uiState.value.textFieldValue
        val text = tfv.text
        val sel = tfv.selection

        val newText: String
        val newCursor: Int

        if (sel.collapsed) {
            val insertIdx = sel.start.coerceIn(0, text.length)
            newText = text.substring(0, insertIdx) + snippet + text.substring(insertIdx)
            newCursor = insertIdx + snippet.length
        } else {
            val start = sel.min.coerceIn(0, text.length)
            val end = sel.max.coerceIn(0, text.length)
            newText = text.substring(0, start) + snippet + text.substring(end)
            newCursor = start + snippet.length
        }

        val updatedTfv = TextFieldValue(newText, TextRange(newCursor))
        onTextFieldValueChange(updatedTfv)
    }

    fun insertWrappingText(prefix: String, suffix: String) {
        val tfv = _uiState.value.textFieldValue
        val text = tfv.text
        val sel = tfv.selection

        val newText: String
        val newSelection: TextRange

        if (sel.collapsed) {
            val insertIdx = sel.start.coerceIn(0, text.length)
            newText = text.substring(0, insertIdx) + prefix + suffix + text.substring(insertIdx)
            newSelection = TextRange(insertIdx + prefix.length)
        } else {
            val start = sel.min.coerceIn(0, text.length)
            val end = sel.max.coerceIn(0, text.length)
            val selectedPart = text.substring(start, end)
            newText = text.substring(0, start) + prefix + selectedPart + suffix + text.substring(end)
            newSelection = TextRange(start, start + prefix.length + selectedPart.length + suffix.length)
        }

        val updatedTfv = TextFieldValue(newText, newSelection)
        onTextFieldValueChange(updatedTfv)
    }

    fun highlightSelection() {
        insertWrappingText("==", "==")
    }

    // Local Vault Storage Actions
    fun toggleVaultSheet(open: Boolean) {
        _uiState.value = _uiState.value.copy(isVaultSheetOpen = open)
        if (open) {
            refreshVaultFiles()
        }
    }

    fun refreshVaultFiles() {
        viewModelScope.launch {
            val files = localStorageManager.listVaultFiles()
            _uiState.value = _uiState.value.copy(vaultFiles = files)
        }
    }

    fun syncAllToVault() {
        viewModelScope.launch {
            try {
                val docs = repository.getDocumentList()
                localStorageManager.syncAllToVault(docs)
                refreshVaultFiles()
                _uiState.value = _uiState.value.copy(statusNotification = "Synced ${docs.size} .md files to local storage vault")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reloadActiveFromVault() {
        viewModelScope.launch {
            val current = _uiState.value.activeDocument
            val contentOnDisk = localStorageManager.readFromVault(current.title)
            if (contentOnDisk != null) {
                val updated = current.copy(content = contentOnDisk)
                _uiState.value = _uiState.value.copy(
                    activeDocument = updated,
                    textFieldValue = TextFieldValue(contentOnDisk),
                    previewText = contentOnDisk,
                    statusNotification = "Reloaded '${current.title}.md' from local storage"
                )
                _textStream.value = contentOnDisk
                repository.saveDocument(updated)
            } else {
                localStorageManager.saveToVault(current)
                refreshVaultFiles()
            }
        }
    }

    fun openVaultFile(fileInfo: LocalVaultFileInfo) {
        viewModelScope.launch {
            val dir = localStorageManager.getVaultDirectory()
            val file = java.io.File(dir, fileInfo.name)
            if (file.exists()) {
                val content = localStorageManager.readFileContent(file)
                val title = fileInfo.name.removeSuffix(".md").removeSuffix(".txt")

                val existing = allDocuments.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                if (existing != null) {
                    val updated = existing.copy(content = content)
                    repository.saveDocument(updated)
                    selectDocument(existing.id)
                } else {
                    val newDoc = repository.createNewDocument(title).copy(content = content)
                    repository.saveDocument(newDoc)
                    selectDocument(newDoc.id)
                }
                toggleVaultSheet(false)
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val (title, content) = localStorageManager.readTextFromUri(uri)
                val newDoc = repository.createNewDocument(title).copy(content = content)
                repository.saveDocument(newDoc)
                localStorageManager.saveToVault(newDoc)
                selectDocument(newDoc.id)
                refreshVaultFiles()
                toggleVaultSheet(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportActiveToUri(uri: Uri) {
        viewModelScope.launch {
            val currentText = _uiState.value.editorText
            localStorageManager.writeTextToUri(uri, currentText)
        }
    }

    // Find and Replace Actions
    fun toggleFindReplace(open: Boolean) {
        _uiState.value = _uiState.value.copy(isFindReplaceOpen = open)
        if (!open) {
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                replaceQuery = "",
                searchMatchIndices = emptyList(),
                currentMatchIndex = 0
            )
        }
    }

    fun setFindQuery(query: String) {
        val text = _uiState.value.editorText
        val matches = if (query.isEmpty()) emptyList() else {
            val list = mutableListOf<Int>()
            var idx = text.indexOf(query, 0, ignoreCase = true)
            while (idx >= 0) {
                list.add(idx)
                idx = text.indexOf(query, idx + query.length.coerceAtLeast(1), ignoreCase = true)
            }
            list
        }

        val newCurrentIdx = if (matches.isNotEmpty()) 0 else 0
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchMatchIndices = matches,
            currentMatchIndex = newCurrentIdx
        )

        if (matches.isNotEmpty()) {
            jumpToMatch(matches[0], query.length)
        }
    }

    fun setReplaceQuery(query: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = query)
    }

    fun findNext() {
        val matches = _uiState.value.searchMatchIndices
        if (matches.isEmpty()) return
        val nextIdx = (_uiState.value.currentMatchIndex + 1) % matches.size
        _uiState.value = _uiState.value.copy(currentMatchIndex = nextIdx)
        jumpToMatch(matches[nextIdx], _uiState.value.searchQuery.length)
    }

    fun findPrevious() {
        val matches = _uiState.value.searchMatchIndices
        if (matches.isEmpty()) return
        val prevIdx = if (_uiState.value.currentMatchIndex <= 0) matches.size - 1 else _uiState.value.currentMatchIndex - 1
        _uiState.value = _uiState.value.copy(currentMatchIndex = prevIdx)
        jumpToMatch(matches[prevIdx], _uiState.value.searchQuery.length)
    }

    private fun jumpToMatch(startIndex: Int, length: Int) {
        val tfv = _uiState.value.textFieldValue
        val text = tfv.text
        if (startIndex in 0..text.length) {
            val end = (startIndex + length).coerceIn(0, text.length)
            _uiState.value = _uiState.value.copy(
                textFieldValue = TextFieldValue(text, TextRange(startIndex, end))
            )
        }
    }

    fun replaceCurrent() {
        val matches = _uiState.value.searchMatchIndices
        val currentIdx = _uiState.value.currentMatchIndex
        val find = _uiState.value.searchQuery
        val replace = _uiState.value.replaceQuery
        if (matches.isEmpty() || find.isEmpty() || currentIdx !in matches.indices) return

        val matchStart = matches[currentIdx]
        val tfv = _uiState.value.textFieldValue
        val text = tfv.text
        if (matchStart + find.length <= text.length) {
            val newText = text.substring(0, matchStart) + replace + text.substring(matchStart + find.length)
            val newTfv = TextFieldValue(newText, TextRange(matchStart + replace.length))
            onTextFieldValueChange(newTfv)
            setFindQuery(find)
        }
    }

    fun replaceAll() {
        val find = _uiState.value.searchQuery
        val replace = _uiState.value.replaceQuery
        if (find.isEmpty()) return
        val text = _uiState.value.editorText
        val newText = text.replace(find, replace, ignoreCase = true)
        val newTfv = TextFieldValue(newText, TextRange(0))
        onTextFieldValueChange(newTfv)
        setFindQuery(find)
    }
}
