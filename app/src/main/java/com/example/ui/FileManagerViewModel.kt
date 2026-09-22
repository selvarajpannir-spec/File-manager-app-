package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.data.repository.FileManagerRepository
import com.example.util.AudioPlayerState
import com.example.util.AudioPreviewManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FileUiState(
    val searchQuery: String = "",
    val selectedTagFilterIds: Set<Long> = emptySet(),
    val selectedFileForPreview: FileWithTags? = null,
    val fileForTaggingSheet: FileWithTags? = null,
    val showCreateFileDialog: Boolean = false,
    val fileForDetailsDialog: FileWithTags? = null,
    val isIndexing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FileManagerRepository(database)
    val audioManager = AudioPreviewManager(application)

    private val _uiState = MutableStateFlow(FileUiState())
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    val allTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val audioState: StateFlow<AudioPlayerState> = audioManager.playerState

    private val searchQueryFlow = MutableStateFlow("")
    private val tagFilterIdsFlow = MutableStateFlow<Set<Long>>(emptySet())

    val filteredFiles: StateFlow<List<FileWithTags>> = combine(
        searchQueryFlow,
        tagFilterIdsFlow
    ) { query, tagIds ->
        Pair(query, tagIds)
    }.flatMapLatest { (query, tagIds) ->
        repository.getFilteredAndSearchedFiles(query, tagIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.initializeSampleDataIfEmpty(application)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQueryFlow.value = query
    }

    fun onTagFilterToggled(tagId: Long) {
        val current = _uiState.value.selectedTagFilterIds
        val updated = if (tagId in current) current - tagId else current + tagId
        _uiState.value = _uiState.value.copy(selectedTagFilterIds = updated)
        tagFilterIdsFlow.value = updated
    }

    fun onClearTagFilters() {
        _uiState.value = _uiState.value.copy(selectedTagFilterIds = emptySet())
        tagFilterIdsFlow.value = emptySet()
    }

    fun onSelectFileForPreview(file: FileWithTags?) {
        // If selecting same file that is already selected, close it
        val target = if (_uiState.value.selectedFileForPreview?.file?.id == file?.file?.id) {
            null
        } else {
            file
        }
        _uiState.value = _uiState.value.copy(selectedFileForPreview = target)
        if (target == null) {
            audioManager.stop()
        }
    }

    fun openTagSheet(file: FileWithTags) {
        _uiState.value = _uiState.value.copy(fileForTaggingSheet = file)
    }

    fun closeTagSheet() {
        _uiState.value = _uiState.value.copy(fileForTaggingSheet = null)
    }

    fun toggleTagForFile(tag: TagEntity) {
        val activeFile = _uiState.value.fileForTaggingSheet ?: return
        val isAssigned = activeFile.tags.any { it.tagId == tag.tagId }

        viewModelScope.launch {
            repository.toggleTagForFile(activeFile.file.id, tag.tagId, isAssigned)
            // Update the preview file if it's the same
            if (_uiState.value.selectedFileForPreview?.file?.id == activeFile.file.id) {
                val updatedTags = if (isAssigned) {
                    activeFile.tags.filter { it.tagId != tag.tagId }
                } else {
                    activeFile.tags + tag
                }
                _uiState.value = _uiState.value.copy(
                    selectedFileForPreview = activeFile.copy(tags = updatedTags),
                    fileForTaggingSheet = activeFile.copy(tags = updatedTags)
                )
            } else {
                val updatedTags = if (isAssigned) {
                    activeFile.tags.filter { it.tagId != tag.tagId }
                } else {
                    activeFile.tags + tag
                }
                _uiState.value = _uiState.value.copy(
                    fileForTaggingSheet = activeFile.copy(tags = updatedTags)
                )
            }
        }
    }

    fun createAndAssignTag(name: String, colorHex: String) {
        val activeFile = _uiState.value.fileForTaggingSheet ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            val newTagId = repository.createNewTag(name, colorHex)
            repository.addTagToFile(activeFile.file.id, newTagId)
            val newTag = TagEntity(tagId = newTagId, tagName = name.trim().removePrefix("#"), colorHex = colorHex)
            _uiState.value = _uiState.value.copy(
                fileForTaggingSheet = activeFile.copy(tags = activeFile.tags + newTag)
            )
        }
    }

    fun importFiles(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIndexing = true)
            var lastImportedId = -1L
            for (uri in uris) {
                val id = repository.importDocumentUri(uri, context)
                if (id != -1L) lastImportedId = id
            }
            _uiState.value = _uiState.value.copy(isIndexing = false)
        }
    }

    fun createNewNote(fileName: String, content: String, tags: List<String>, context: Context) {
        viewModelScope.launch {
            repository.createNoteFile(fileName, content, tags, context)
            _uiState.value = _uiState.value.copy(showCreateFileDialog = false)
        }
    }

    fun openCreateFileDialog() {
        _uiState.value = _uiState.value.copy(showCreateFileDialog = true)
    }

    fun closeCreateFileDialog() {
        _uiState.value = _uiState.value.copy(showCreateFileDialog = false)
    }

    fun openFileDetails(file: FileWithTags) {
        _uiState.value = _uiState.value.copy(fileForDetailsDialog = file)
    }

    fun closeFileDetails() {
        _uiState.value = _uiState.value.copy(fileForDetailsDialog = null)
    }

    fun deleteFile(file: FileWithTags, context: Context) {
        viewModelScope.launch {
            if (_uiState.value.selectedFileForPreview?.file?.id == file.file.id) {
                _uiState.value = _uiState.value.copy(selectedFileForPreview = null)
                audioManager.stop()
            }
            repository.deleteFile(file, context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stop()
    }
}
