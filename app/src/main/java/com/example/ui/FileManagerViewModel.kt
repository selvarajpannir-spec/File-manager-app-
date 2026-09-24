package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.data.model.FileSystemItem
import com.example.data.repository.FileManagerRepository
import com.example.service.DeepSearchForegroundService
import com.example.util.AppSettings
import com.example.util.AudioPlayerState
import com.example.util.AudioPreviewManager
import com.example.util.AutoOrganizeManager
import com.example.util.FileCategory
import com.example.util.FontSizeScale
import com.example.util.FileUtil
import com.example.util.NotificationHelper
import com.example.util.OrganizeCategoryConfig
import com.example.util.OrganizeFolderTarget
import com.example.util.OrganizeProgress
import com.example.util.SearchMatchResult
import com.example.util.StorageLocation
import com.example.util.StorageSearchScanner
import com.example.util.StorageStats
import com.example.util.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class FileExplorerTab {
    BROWSER,
    KEYWORD_SEARCH,
    TAGGED_FILES,
    STORAGE_INFO
}

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC,
    TYPE
}

enum class ViewLayoutMode {
    LIST,
    GRID
}

data class FileUiState(
    val currentPath: String = "",
    val pathHistory: List<String> = emptyList(),
    val forwardHistory: List<String> = emptyList(),
    val currentItems: List<FileSystemItem> = emptyList(),
    val isLoading: Boolean = false,
    val showHiddenFiles: Boolean = true,
    val activeTab: FileExplorerTab = FileExplorerTab.BROWSER,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val viewLayoutMode: ViewLayoutMode = ViewLayoutMode.LIST,
    // Explorer Tab Fast File Search
    val searchQuery: String = "",
    val selectedTagFilterIds: Set<Long> = emptySet(),
    // Dedicated Keyword Search Tab
    val keywordSearchInput: String = "",
    val keywordSearchRootPath: String? = null,
    val keywordSearchMatchAll: Boolean = false,
    val isKeywordSearching: Boolean = false,
    val keywordSearchResults: List<SearchMatchResult> = emptyList(),
    val keywordSearchScannedCount: Int = 0,
    val keywordSearchFoundCount: Int = 0,
    val keywordSearchProgressPercent: Int = 0,
    val lastScannedKeywords: List<String> = emptyList(),
    // Auto Organize Feature State
    val showAutoOrganizeDialog: Boolean = false,
    val organizeProgress: OrganizeProgress = OrganizeProgress(),
    val organizeFolderTargets: List<OrganizeFolderTarget> = emptyList(),
    // Dialogs & Previews
    val selectedItemForPreview: FileSystemItem? = null,
    val itemForOpenFilePrompt: FileSystemItem? = null,
    val openPromptKeywords: List<String> = emptyList(),
    val itemForFullScreenViewer: FileSystemItem? = null,
    val fullScreenKeywords: List<String> = emptyList(),
    val itemForTaggingSheet: FileSystemItem? = null,
    val itemForDetailsDialog: FileSystemItem? = null,
    val itemForRenameDialog: FileSystemItem? = null,
    val showCreateFolderDialog: Boolean = false,
    val showCreateFileDialog: Boolean = false,
    val showFontSizeDialog: Boolean = false,
    val showPermissionPromptDialog: Boolean = false,
    val storageStats: StorageStats = StorageStats(0, 0, 0, 0),
    val storageLocations: List<StorageLocation> = emptyList(),
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val hasStoragePermission: Boolean = true
)

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FileManagerRepository(database)
    val audioManager = AudioPreviewManager(application)
    val appSettings = AppSettings.getInstance(application)

    private val _uiState = MutableStateFlow(FileUiState())
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = appSettings.themeMode
    val fontScale: StateFlow<FontSizeScale> = appSettings.fontScale

    val allTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDbFilesWithTags: StateFlow<List<FileWithTags>> = repository.getAllFilesWithTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val audioState: StateFlow<AudioPlayerState> = audioManager.playerState

    private var keywordSearchJob: Job? = null
    private var organizeJob: Job? = null

    init {
        val locations = FileUtil.getAvailableStorageLocations(application)
        val stats = FileUtil.getStorageStats()

        val initialPath = locations.firstOrNull { it.path == Environment.getExternalStorageDirectory()?.absolutePath }?.path
            ?: locations.firstOrNull { it.path == "/" }?.path
            ?: application.filesDir.absolutePath

        val hasPerm = checkStoragePermission(application)

        _uiState.value = _uiState.value.copy(
            currentPath = initialPath,
            storageLocations = locations,
            storageStats = stats,
            hasStoragePermission = hasPerm,
            showPermissionPromptDialog = !hasPerm
        )

        viewModelScope.launch {
            repository.initializeDefaultTagsAndCleanupSamples(application)
            loadDirectory(initialPath)
        }

        // Observe background foreground service deep search progress
        viewModelScope.launch {
            DeepSearchForegroundService.searchState.collect { sState ->
                if (sState.isRunning) {
                    _uiState.value = _uiState.value.copy(
                        isKeywordSearching = true,
                        keywordSearchResults = sState.results,
                        keywordSearchScannedCount = sState.scannedCount,
                        keywordSearchFoundCount = sState.foundCount,
                        keywordSearchProgressPercent = sState.progressPercent,
                        lastScannedKeywords = sState.keywords
                    )
                } else if (sState.isFinished) {
                    _uiState.value = _uiState.value.copy(
                        isKeywordSearching = false,
                        keywordSearchResults = sState.results,
                        keywordSearchScannedCount = sState.scannedCount,
                        keywordSearchFoundCount = sState.foundCount,
                        keywordSearchProgressPercent = 100,
                        lastScannedKeywords = sState.keywords,
                        statusMessage = if (sState.statusMessage.isNotEmpty()) sState.statusMessage else _uiState.value.statusMessage
                    )
                } else if (sState.isCancelled) {
                    _uiState.value = _uiState.value.copy(
                        isKeywordSearching = false,
                        statusMessage = "Keyword search cancelled."
                    )
                }
            }
        }
    }

    fun checkStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun refreshStoragePermission(context: Context) {
        val hasPerm = checkStoragePermission(context)
        _uiState.value = _uiState.value.copy(
            hasStoragePermission = hasPerm,
            showPermissionPromptDialog = if (hasPerm) false else _uiState.value.showPermissionPromptDialog
        )
        refreshCurrentDirectory()
    }

    fun dismissPermissionPrompt() {
        _uiState.value = _uiState.value.copy(showPermissionPromptDialog = false)
    }

    fun toggleTheme() {
        appSettings.toggleLightDarkTheme()
    }

    fun setFontScale(scale: FontSizeScale) {
        appSettings.setFontScale(scale)
    }

    fun openFontSizeDialog() {
        _uiState.value = _uiState.value.copy(showFontSizeDialog = true)
    }

    fun closeFontSizeDialog() {
        _uiState.value = _uiState.value.copy(showFontSizeDialog = false)
    }

    fun loadDirectory(path: String, addToHistory: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val dir = File(path)
            if (!dir.exists()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Directory does not exist: $path"
                )
                return@launch
            }

            val items = repository.getDirectoryItems(path, _uiState.value.showHiddenFiles)
            val sorted = sortItems(items, _uiState.value.sortMode)

            val updatedHistory = if (addToHistory && _uiState.value.currentPath.isNotEmpty() && _uiState.value.currentPath != path) {
                _uiState.value.pathHistory + _uiState.value.currentPath
            } else {
                _uiState.value.pathHistory
            }

            _uiState.value = _uiState.value.copy(
                currentPath = path,
                pathHistory = updatedHistory,
                currentItems = sorted,
                isLoading = false,
                storageStats = FileUtil.getStorageStats()
            )
        }
    }

    fun navigateUp() {
        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadDirectory(parent.absolutePath)
        } else if (_uiState.value.currentPath != "/") {
            loadDirectory("/")
        }
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.activeTab != FileExplorerTab.BROWSER) {
            setActiveTab(FileExplorerTab.BROWSER)
            return true
        }

        val history = _uiState.value.pathHistory
        if (history.isNotEmpty()) {
            val previous = history.last()
            val newHistory = history.dropLast(1)
            _uiState.value = _uiState.value.copy(pathHistory = newHistory)
            loadDirectory(previous, addToHistory = false)
            return true
        }
        return false
    }

    fun refreshCurrentDirectory() {
        if (_uiState.value.currentPath.isNotEmpty()) {
            loadDirectory(_uiState.value.currentPath, addToHistory = false)
        }
    }

    fun toggleShowHiddenFiles() {
        val next = !_uiState.value.showHiddenFiles
        _uiState.value = _uiState.value.copy(showHiddenFiles = next)
        refreshCurrentDirectory()
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(
            sortMode = mode,
            currentItems = sortItems(_uiState.value.currentItems, mode)
        )
    }

    fun toggleViewLayout() {
        val next = if (_uiState.value.viewLayoutMode == ViewLayoutMode.LIST) ViewLayoutMode.GRID else ViewLayoutMode.LIST
        _uiState.value = _uiState.value.copy(viewLayoutMode = next)
    }

    fun setActiveTab(tab: FileExplorerTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == FileExplorerTab.BROWSER) {
            refreshCurrentDirectory()
        }
    }

    // --- Fast File Search in Explorer Tab ---
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onTagFilterToggled(tagId: Long) {
        val current = _uiState.value.selectedTagFilterIds
        val updated = if (tagId in current) current - tagId else current + tagId
        _uiState.value = _uiState.value.copy(selectedTagFilterIds = updated)
    }

    fun onClearTagFilters() {
        _uiState.value = _uiState.value.copy(selectedTagFilterIds = emptySet())
    }

    // --- Dedicated Keyword Search Tab Operations ---
    fun onKeywordSearchInputChanged(query: String) {
        _uiState.value = _uiState.value.copy(keywordSearchInput = query)
    }

    fun setKeywordSearchRoot(path: String?) {
        _uiState.value = _uiState.value.copy(keywordSearchRootPath = path)
    }

    fun toggleKeywordMatchAll() {
        val next = !_uiState.value.keywordSearchMatchAll
        _uiState.value = _uiState.value.copy(keywordSearchMatchAll = next)
    }

    fun startBackgroundKeywordSearch() {
        val query = _uiState.value.keywordSearchInput
        val keywords = StorageSearchScanner.parseKeywords(query)
        if (keywords.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter comma-separated keyword(s) to scan.")
            return
        }

        val rootPath = _uiState.value.keywordSearchRootPath
        val matchAll = _uiState.value.keywordSearchMatchAll

        _uiState.value = _uiState.value.copy(
            isKeywordSearching = true,
            keywordSearchResults = emptyList(),
            keywordSearchScannedCount = 0,
            keywordSearchFoundCount = 0,
            keywordSearchProgressPercent = 0,
            lastScannedKeywords = keywords,
            statusMessage = "Deep keyword scan running via Foreground Service..."
        )

        DeepSearchForegroundService.startSearch(
            context = getApplication(),
            keywords = keywords,
            rootPath = rootPath,
            matchAll = matchAll
        )
    }

    fun cancelKeywordSearch() {
        DeepSearchForegroundService.cancelSearch(getApplication())
        com.example.util.NotificationHelper.cancelForegroundNotification(getApplication())
        _uiState.value = _uiState.value.copy(
            isKeywordSearching = false,
            statusMessage = "Keyword scan cancelled."
        )
    }

    fun clearKeywordSearchResults() {
        DeepSearchForegroundService.clearState()
        com.example.util.NotificationHelper.cancelAllSearchNotifications(getApplication())
        _uiState.value = _uiState.value.copy(
            isKeywordSearching = false,
            keywordSearchResults = emptyList(),
            keywordSearchScannedCount = 0,
            keywordSearchFoundCount = 0,
            keywordSearchProgressPercent = 0,
            keywordSearchInput = ""
        )
    }

    // --- AUTO ORGANIZE FILES FEATURE ---
    fun openAutoOrganizeDialog() {
        val targets = AutoOrganizeManager.getStandardStorageFolders(getApplication(), _uiState.value.currentPath)
        _uiState.value = _uiState.value.copy(
            showAutoOrganizeDialog = true,
            organizeFolderTargets = targets,
            organizeProgress = OrganizeProgress()
        )
    }

    fun closeAutoOrganizeDialog() {
        organizeJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showAutoOrganizeDialog = false,
            organizeProgress = OrganizeProgress()
        )
    }

    fun cancelAutoOrganize() {
        organizeJob?.cancel()
        _uiState.value = _uiState.value.copy(
            organizeProgress = _uiState.value.organizeProgress.copy(
                isRunning = false,
                errorMessage = "Auto Organize cancelled by user."
            ),
            statusMessage = "Auto Organize cancelled."
        )
    }

    fun startAutoOrganize(
        selectedFolders: List<String>,
        selectedCategories: List<OrganizeCategoryConfig>
    ) {
        if (selectedFolders.isEmpty() || selectedCategories.isEmpty()) return

        val extStorage = Environment.getExternalStorageDirectory()
        val destinationBasePath = if (extStorage != null && extStorage.exists() && extStorage.canWrite()) {
            File(extStorage, "AutoOrganized").absolutePath
        } else {
            File(getApplication<Application>().filesDir, "AutoOrganized").absolutePath
        }

        organizeJob?.cancel()
        organizeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                organizeProgress = OrganizeProgress(
                    isRunning = true,
                    targetDestinationPath = destinationBasePath
                )
            )

            val finalResult = AutoOrganizeManager.executeAutoOrganize(
                context = getApplication(),
                sourcePaths = selectedFolders,
                destinationBasePath = destinationBasePath,
                enabledCategories = selectedCategories,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(organizeProgress = progress)
                }
            )

            _uiState.value = _uiState.value.copy(
                organizeProgress = finalResult,
                statusMessage = "Auto Organized ${finalResult.movedCount} files into /AutoOrganized."
            )

            // Refresh current folder view if it was modified
            refreshCurrentDirectory()
        }
    }

    // --- File Preview and Item Actions ---
    fun onSelectFileForPreview(item: FileSystemItem?, keywords: List<String> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            selectedItemForPreview = item,
            lastScannedKeywords = if (keywords.isNotEmpty()) keywords else _uiState.value.lastScannedKeywords
        )
        if (item == null) {
            audioManager.stop()
        }
    }

    fun openFileChoicePrompt(item: FileSystemItem, keywords: List<String> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            itemForOpenFilePrompt = item,
            openPromptKeywords = keywords
        )
    }

    fun closeFileChoicePrompt() {
        _uiState.value = _uiState.value.copy(itemForOpenFilePrompt = null)
    }

    fun openFullScreenViewer(item: FileSystemItem, keywords: List<String> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            itemForFullScreenViewer = item,
            fullScreenKeywords = keywords,
            itemForOpenFilePrompt = null
        )
    }

    fun closeFullScreenViewer() {
        _uiState.value = _uiState.value.copy(
            itemForFullScreenViewer = null,
            fullScreenKeywords = emptyList()
        )
    }

    fun openTagSheet(item: FileSystemItem) {
        _uiState.value = _uiState.value.copy(itemForTaggingSheet = item)
    }

    fun closeTagSheet() {
        _uiState.value = _uiState.value.copy(itemForTaggingSheet = null)
    }

    fun toggleTagForItem(tag: TagEntity) {
        val activeItem = _uiState.value.itemForTaggingSheet ?: return
        val isAssigned = activeItem.tags.any { it.tagId == tag.tagId }

        viewModelScope.launch {
            repository.toggleTagForFilePath(activeItem.file, tag, isAssigned)
            val updatedTags = if (isAssigned) {
                activeItem.tags.filter { it.tagId != tag.tagId }
            } else {
                activeItem.tags + tag
            }
            val updatedItem = activeItem.copy(tags = updatedTags)

            _uiState.value = _uiState.value.copy(
                itemForTaggingSheet = updatedItem,
                selectedItemForPreview = if (_uiState.value.selectedItemForPreview?.path == activeItem.path) updatedItem else _uiState.value.selectedItemForPreview,
                currentItems = _uiState.value.currentItems.map { if (it.path == activeItem.path) updatedItem else it },
                keywordSearchResults = _uiState.value.keywordSearchResults.map {
                    if (it.item.path == activeItem.path) it.copy(item = updatedItem) else it
                }
            )
        }
    }

    fun createAndAssignTag(name: String, colorHex: String) {
        val activeItem = _uiState.value.itemForTaggingSheet ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            val newTagId = repository.createNewTag(name, colorHex)
            val newTag = TagEntity(tagId = newTagId, tagName = name.trim().removePrefix("#"), colorHex = colorHex)
            repository.toggleTagForFilePath(activeItem.file, newTag, isAssigned = false)

            val updatedItem = activeItem.copy(tags = activeItem.tags + newTag)
            _uiState.value = _uiState.value.copy(
                itemForTaggingSheet = updatedItem,
                selectedItemForPreview = if (_uiState.value.selectedItemForPreview?.path == activeItem.path) updatedItem else _uiState.value.selectedItemForPreview,
                currentItems = _uiState.value.currentItems.map { if (it.path == activeItem.path) updatedItem else it },
                keywordSearchResults = _uiState.value.keywordSearchResults.map {
                    if (it.item.path == activeItem.path) it.copy(item = updatedItem) else it
                }
            )
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) return
        viewModelScope.launch {
            val success = repository.createFolder(_uiState.value.currentPath, folderName)
            if (success) {
                _uiState.value = _uiState.value.copy(showCreateFolderDialog = false, statusMessage = "Created folder: $folderName")
                refreshCurrentDirectory()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create folder. Check storage permissions.")
            }
        }
    }

    fun createTextFile(fileName: String, content: String) {
        if (fileName.isBlank()) return
        viewModelScope.launch {
            val success = repository.createTextFile(_uiState.value.currentPath, fileName, content)
            if (success) {
                _uiState.value = _uiState.value.copy(showCreateFileDialog = false, statusMessage = "Created file: $fileName")
                refreshCurrentDirectory()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create file. Check storage permissions.")
            }
        }
    }

    fun deleteItem(item: FileSystemItem) {
        viewModelScope.launch {
            if (_uiState.value.selectedItemForPreview?.path == item.path) {
                _uiState.value = _uiState.value.copy(selectedItemForPreview = null)
                audioManager.stop()
            }
            val success = repository.deleteFileOrFolder(item.file)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Deleted: ${item.name}",
                    keywordSearchResults = _uiState.value.keywordSearchResults.filter { it.item.path != item.path }
                )
                refreshCurrentDirectory()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not delete ${item.name}. May be write-protected.")
            }
        }
    }

    fun renameItem(newName: String) {
        val item = _uiState.value.itemForRenameDialog ?: return
        if (newName.isBlank() || newName == item.name) {
            _uiState.value = _uiState.value.copy(itemForRenameDialog = null)
            return
        }

        viewModelScope.launch {
            val success = repository.renameFileOrFolder(item.file, newName)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    itemForRenameDialog = null,
                    statusMessage = "Renamed to $newName"
                )
                refreshCurrentDirectory()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Rename failed. File system may be read-only.")
            }
        }
    }

    fun importUris(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            for (uri in uris) {
                repository.importDocumentUri(uri, context, _uiState.value.currentPath)
            }
            _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = "Imported ${uris.size} file(s)")
            refreshCurrentDirectory()
        }
    }

    fun openCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(showCreateFolderDialog = true)
    }

    fun closeCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(showCreateFolderDialog = false)
    }

    fun openCreateFileDialog() {
        _uiState.value = _uiState.value.copy(showCreateFileDialog = true)
    }

    fun closeCreateFileDialog() {
        _uiState.value = _uiState.value.copy(showCreateFileDialog = false)
    }

    fun openDetailsDialog(item: FileSystemItem) {
        _uiState.value = _uiState.value.copy(itemForDetailsDialog = item)
    }

    fun closeDetailsDialog() {
        _uiState.value = _uiState.value.copy(itemForDetailsDialog = null)
    }

    fun openRenameDialog(item: FileSystemItem) {
        _uiState.value = _uiState.value.copy(itemForRenameDialog = item)
    }

    fun closeRenameDialog() {
        _uiState.value = _uiState.value.copy(itemForRenameDialog = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, statusMessage = null)
    }

    private fun sortItems(items: List<FileSystemItem>, mode: SortMode): List<FileSystemItem> {
        return when (mode) {
            SortMode.NAME_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
            SortMode.NAME_DESC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })).reversed()
            SortMode.DATE_DESC -> items.sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified }))
            SortMode.DATE_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
            SortMode.SIZE_DESC -> items.sortedWith(compareBy({ !it.isDirectory }, { -it.size }))
            SortMode.SIZE_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            SortMode.TYPE -> items.sortedWith(compareBy({ !it.isDirectory }, { it.category.name }, { it.name.lowercase(Locale.ROOT) }))
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stop()
        keywordSearchJob?.cancel()
        organizeJob?.cancel()
    }
}
