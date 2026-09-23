package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class OrganizeCategoryConfig(
    val id: String,
    val displayName: String,
    val folderName: String,
    val iconEmoji: String,
    val extensions: Set<String>,
    val isEnabled: Boolean = true
)

data class OrganizeFolderTarget(
    val displayName: String,
    val path: String,
    val isSelected: Boolean = true
)

data class OrganizeProgress(
    val currentFileName: String = "",
    val scannedCount: Int = 0,
    val movedCount: Int = 0,
    val currentCategory: String = "",
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val targetDestinationPath: String = ""
)

object AutoOrganizeManager {

    val DEFAULT_CATEGORIES: List<OrganizeCategoryConfig> = listOf(
        OrganizeCategoryConfig(
            id = "images",
            displayName = "Images",
            folderName = "Images",
            iconEmoji = "🖼️",
            extensions = setOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic", "tiff", "ico")
        ),
        OrganizeCategoryConfig(
            id = "videos",
            displayName = "Videos",
            folderName = "Videos",
            iconEmoji = "🎬",
            extensions = setOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "3gp", "webm", "m4v", "ts")
        ),
        OrganizeCategoryConfig(
            id = "documents",
            displayName = "Documents",
            folderName = "Documents",
            iconEmoji = "📄",
            extensions = setOf("txt", "md", "doc", "docx", "pdf", "rtf", "odt", "tex", "pages", "epub")
        ),
        OrganizeCategoryConfig(
            id = "code",
            displayName = "Code Files",
            folderName = "Code",
            iconEmoji = "💻",
            extensions = setOf("java", "kt", "kts", "html", "htm", "css", "scss", "yml", "yaml", "json", "xml", "py", "js", "ts", "jsx", "tsx", "cpp", "c", "h", "hpp", "sh", "bash", "sql", "rs", "go", "php", "rb", "dart", "gradle", "properties", "toml")
        ),
        OrganizeCategoryConfig(
            id = "sheets",
            displayName = "Sheets",
            folderName = "Sheets",
            iconEmoji = "📊",
            extensions = setOf("xls", "xlsx", "csv", "ods", "tsv", "numbers", "xlsm")
        ),
        OrganizeCategoryConfig(
            id = "presentations",
            displayName = "Presentations",
            folderName = "Presentations",
            iconEmoji = "📽️",
            extensions = setOf("ppt", "pptx", "odp", "key", "pps", "ppsx")
        ),
        OrganizeCategoryConfig(
            id = "audio",
            displayName = "Audio & Music",
            folderName = "Audio",
            iconEmoji = "🎵",
            extensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "wma", "mid", "midi")
        ),
        OrganizeCategoryConfig(
            id = "archives",
            displayName = "Archives",
            folderName = "Archives",
            iconEmoji = "📦",
            extensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz")
        ),
        OrganizeCategoryConfig(
            id = "apps",
            displayName = "Apps & APKs",
            folderName = "Apps",
            iconEmoji = "📱",
            extensions = setOf("apk", "xapk", "apks")
        )
    )

    fun getStandardStorageFolders(context: Context, currentBrowsingPath: String? = null): List<OrganizeFolderTarget> {
        val targets = mutableListOf<OrganizeFolderTarget>()

        if (!currentBrowsingPath.isNullOrBlank() && File(currentBrowsingPath).exists()) {
            val name = File(currentBrowsingPath).name.ifEmpty { "Current Folder" }
            targets.add(OrganizeFolderTarget("Current Folder ($name)", currentBrowsingPath, isSelected = true))
        }

        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads.exists() && targets.none { it.path == downloads.absolutePath }) {
                targets.add(OrganizeFolderTarget("Downloads", downloads.absolutePath, isSelected = true))
            }

            val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documents.exists() && targets.none { it.path == documents.absolutePath }) {
                targets.add(OrganizeFolderTarget("Documents", documents.absolutePath, isSelected = true))
            }

            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (pictures.exists() && targets.none { it.path == pictures.absolutePath }) {
                targets.add(OrganizeFolderTarget("Pictures", pictures.absolutePath, isSelected = true))
            }

            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (dcim.exists() && targets.none { it.path == dcim.absolutePath }) {
                targets.add(OrganizeFolderTarget("DCIM (Camera)", dcim.absolutePath, isSelected = false))
            }

            val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (movies.exists() && targets.none { it.path == movies.absolutePath }) {
                targets.add(OrganizeFolderTarget("Movies", movies.absolutePath, isSelected = false))
            }

            val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (music.exists() && targets.none { it.path == music.absolutePath }) {
                targets.add(OrganizeFolderTarget("Music", music.absolutePath, isSelected = false))
            }

            if (targets.none { it.path == extStorage.absolutePath }) {
                targets.add(OrganizeFolderTarget("Storage Root (${extStorage.name.ifEmpty { "Internal" }})", extStorage.absolutePath, isSelected = false))
            }
        }

        // App's files dir
        val appFiles = context.filesDir
        if (appFiles.exists() && targets.none { it.path == appFiles.absolutePath }) {
            targets.add(OrganizeFolderTarget("App Files Directory", appFiles.absolutePath, isSelected = false))
        }

        return targets
    }

    /**
     * Checks whether a directory or file should be skipped to protect system,
     * app packages, hidden system paths, and avoid recursion.
     */
    fun isProtectedOrSystemPath(file: File, destinationBaseDir: File): Boolean {
        val path = file.absolutePath
        val name = file.name

        // Do not organize or recurse into destination folder itself
        if (file.absolutePath == destinationBaseDir.absolutePath || file.absolutePath.startsWith(destinationBaseDir.absolutePath + File.separator)) {
            return true
        }

        // System directories
        val systemRoots = listOf("/system", "/proc", "/sys", "/dev", "/etc", "/apex", "/vendor", "/odm", "/product")
        if (systemRoots.any { path == it || path.startsWith("$it/") }) {
            return true
        }

        // App-private package data like Android/data/com.xyz, Android/obb/com.xyz
        if (path.contains("/Android/data") || path.contains("/Android/obb") || path.contains("/Android/sandbox")) {
            return true
        }

        // Hidden app cache/config folders or package-like folders
        if (name.startsWith(".") && name != ".auto_organized") {
            return true
        }

        // Avoid touching package-named folders (e.g., com.google.android.gms)
        if (name.contains(".") && (name.startsWith("com.") || name.startsWith("org.") || name.startsWith("io.") || name.startsWith("net."))) {
            return true
        }

        return false
    }

    /**
     * Executes the Auto-Organize process.
     */
    suspend fun executeAutoOrganize(
        context: Context,
        sourcePaths: List<String>,
        destinationBasePath: String,
        enabledCategories: List<OrganizeCategoryConfig>,
        onProgress: (OrganizeProgress) -> Unit
    ): OrganizeProgress = withContext(Dispatchers.IO) {
        val destinationBaseDir = File(destinationBasePath)
        if (!destinationBaseDir.exists()) {
            destinationBaseDir.mkdirs()
        }

        // Build extension -> category folder mapping
        val extensionToFolder = mutableMapOf<String, OrganizeCategoryConfig>()
        for (cat in enabledCategories) {
            for (ext in cat.extensions) {
                extensionToFolder[ext.lowercase(Locale.ROOT)] = cat
            }
        }

        val categoryCounts = mutableMapOf<String, Int>()
        enabledCategories.forEach { categoryCounts[it.displayName] = 0 }

        var scannedCount = 0
        var movedCount = 0

        val db = AppDatabase.getDatabase(context)

        // Queue files to process
        val filesToProcess = mutableListOf<File>()

        for (sourcePath in sourcePaths) {
            val sourceDir = File(sourcePath)
            if (!sourceDir.exists()) continue

            if (sourceDir.isFile) {
                if (!isProtectedOrSystemPath(sourceDir, destinationBaseDir)) {
                    filesToProcess.add(sourceDir)
                }
            } else {
                collectCandidateFiles(sourceDir, destinationBaseDir, filesToProcess)
            }
        }

        val totalCandidates = filesToProcess.size

        for (file in filesToProcess) {
            scannedCount++
            val ext = file.extension.lowercase(Locale.ROOT)
            val matchedCategory = extensionToFolder[ext]

            if (matchedCategory != null && file.exists() && file.isFile && file.canRead()) {
                // Ensure target category subfolder exists
                val targetSubfolder = File(destinationBaseDir, matchedCategory.folderName)
                if (!targetSubfolder.exists()) {
                    targetSubfolder.mkdirs()
                }

                // If file is already inside target category subfolder, skip moving
                if (file.parentFile?.absolutePath == targetSubfolder.absolutePath) {
                    continue
                }

                // Target destination file with conflict resolution
                val destFile = resolveUniqueDestinationFile(targetSubfolder, file.name)
                val oldPath = file.absolutePath
                val newPath = destFile.absolutePath

                val success = moveFileSafely(file, destFile)
                if (success) {
                    movedCount++
                    val currentCount = categoryCounts[matchedCategory.displayName] ?: 0
                    categoryCounts[matchedCategory.displayName] = currentCount + 1

                    // Update DB path if file was tracked in database
                    try {
                        val dbFile = db.fileDao().getFileByPath(oldPath)
                        if (dbFile != null) {
                            db.fileDao().updateFile(dbFile.copy(filePath = newPath, fileName = destFile.name))
                        }
                    } catch (e: Exception) {
                        // ignore DB sync errors
                    }

                    onProgress(
                        OrganizeProgress(
                            currentFileName = file.name,
                            scannedCount = scannedCount,
                            movedCount = movedCount,
                            currentCategory = "${matchedCategory.iconEmoji} ${matchedCategory.displayName}",
                            isRunning = true,
                            isCompleted = false,
                            categoryCounts = categoryCounts.toMap(),
                            targetDestinationPath = destinationBaseDir.absolutePath
                        )
                    )
                }
            } else {
                if (scannedCount % 5 == 0 || scannedCount == totalCandidates) {
                    onProgress(
                        OrganizeProgress(
                            currentFileName = file.name,
                            scannedCount = scannedCount,
                            movedCount = movedCount,
                            currentCategory = "Scanning...",
                            isRunning = true,
                            isCompleted = false,
                            categoryCounts = categoryCounts.toMap(),
                            targetDestinationPath = destinationBaseDir.absolutePath
                        )
                    )
                }
            }
        }

        val finalProgress = OrganizeProgress(
            currentFileName = "Completed",
            scannedCount = scannedCount,
            movedCount = movedCount,
            currentCategory = "Finished",
            isRunning = false,
            isCompleted = true,
            categoryCounts = categoryCounts.toMap(),
            targetDestinationPath = destinationBaseDir.absolutePath
        )

        onProgress(finalProgress)
        return@withContext finalProgress
    }

    private fun collectCandidateFiles(
        dir: File,
        destinationBaseDir: File,
        resultList: MutableList<File>,
        depth: Int = 0
    ) {
        if (depth > 6) return // limit depth to avoid deep recursion
        if (isProtectedOrSystemPath(dir, destinationBaseDir)) return

        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        for (child in children) {
            if (isProtectedOrSystemPath(child, destinationBaseDir)) continue

            if (child.isDirectory) {
                collectCandidateFiles(child, destinationBaseDir, resultList, depth + 1)
            } else if (child.isFile) {
                resultList.add(child)
            }
        }
    }

    private fun resolveUniqueDestinationFile(targetDir: File, originalName: String): File {
        var targetFile = File(targetDir, originalName)
        if (!targetFile.exists()) {
            return targetFile
        }

        val nameWithoutExt = targetFile.nameWithoutExtension
        val ext = targetFile.extension
        val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""

        var counter = 1
        while (targetFile.exists()) {
            targetFile = File(targetDir, "${nameWithoutExt}_$counter$extSuffix")
            counter++
        }
        return targetFile
    }

    private fun moveFileSafely(src: File, dst: File): Boolean {
        return try {
            // First attempt native rename
            if (src.renameTo(dst)) {
                true
            } else {
                // Fallback copy + delete for cross-filesystem moves
                src.inputStream().use { input ->
                    dst.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                src.delete()
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
