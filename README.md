# File Manager — System Explorer, Tagging, Multi-Format Search & Auto Organizer

A modern, high-performance Android File Manager built with **Jetpack Compose**, **Material 3**, **Room Database with FTS4**, and **Kotlin Coroutines / Flow**.

---

## 🌟 Key Features

1. **System & Storage File Explorer (Fast Search & Navigation)**:
   - Full access and navigation across Root (`/`), System (`/system`, `/etc`, `/proc`, `/sys`), and Internal/External storage (`/sdcard`, `Downloads`, `Documents`, `DCIM`, `Pictures`, `Music`, `Movies`).
   - Breadcrumb path navigation bar with instant folder jumping.
   - Fast file name and tag filter header within current directories.
   - Hidden files toggle (`.filename`) and multi-criteria sorting (Name, Date, Size, Type).

2. **3-Dot Overflow Drawer & Top Bar Streamlining**:
   - Clean, decluttered top app bar with all control options grouped into a Material 3 3-dot overflow menu (`MoreVert`):
     - ✨ **Auto Organize Files** (Smart extension organizer)
     - ☀️ / 🌙 **Theme Switcher** (Light & Dark theme)
     - 🔤 **Font Size Adjustment**
     - 👁️ **Hidden Files Toggle**
     - 🔃 **Sort Files By...** (Name, Date, Size, Type)
     - 🗄️ **Storage Partitions Quick Jump**
     - 🔄 **Refresh Directory**

3. **Auto Organize Files (Smart Extension-Based Categorization)**:
   - **Safety Warning Alert**: Prominently warns users about moving files and changing locations before any action is taken.
   - **Customizable Scope**: Select specific folders (`Downloads`, `Documents`, `Pictures`, `DCIM`, `Movies`, `Music`, `Storage Root`) or toggle "Select All Folders".
   - **Automatic Folder Creation**: Inspects file extensions, creates missing category subfolders, and moves files cleanly.
   - **Supported Extension Mappings**:
     - 🖼️ **Images**: `jpg`, `jpeg`, `png`, `gif`, `webp`, `svg`, `bmp`, `heic`, `tiff`, `ico` → `/AutoOrganized/Images/`
     - 🎬 **Videos**: `mp4`, `mkv`, `avi`, `mov`, `flv`, `wmv`, `3gp`, `webm`, `m4v`, `ts` → `/AutoOrganized/Videos/`
     - 📄 **Documents**: `txt`, `md`, `doc`, `docx`, `pdf`, `rtf`, `odt`, `tex`, `pages`, `epub` → `/AutoOrganized/Documents/`
     - 💻 **Code Files**: `java`, `kt`, `kts`, `html`, `htm`, `css`, `scss`, `yml`, `yaml`, `json`, `xml`, `py`, `js`, `ts`, `jsx`, `tsx`, `cpp`, `c`, `h`, `sh`, `sql`, `rs`, `go`, `php`, `rb`, `dart`, `gradle`, `properties`, `toml` → `/AutoOrganized/Code/`
     - 📊 **Sheets**: `xls`, `xlsx`, `csv`, `ods`, `tsv`, `numbers`, `xlsm` → `/AutoOrganized/Sheets/`
     - 📽️ **Presentations**: `ppt`, `pptx`, `odp`, `key`, `pps`, `ppsx` → `/AutoOrganized/Presentations/`
     - 🎵 **Audio & Music**: `mp3`, `wav`, `ogg`, `m4a`, `flac`, `aac`, `opus`, `wma`, `mid`, `midi` → `/AutoOrganized/Audio/`
     - 📦 **Archives**: `zip`, `rar`, `7z`, `tar`, `gz`, `bz2`, `xz`, `iso`, `tgz` → `/AutoOrganized/Archives/`
     - 📱 **Apps**: `apk`, `xapk`, `apks` → `/AutoOrganized/Apps/`
   - **System Protection**: Strictly ignores and protects system paths (`/system`, `/proc`, `/sys`, `/dev`, `/apex`, `/vendor`, `/etc`) and app-private package folders (`/Android/data/com.*`, `/Android/obb/com.*`).
   - **Name Conflict Handling**: Safely handles existing files with automatic rename indexing (`document_1.pdf`) and updates Room database tag associations.
   - **Live Progress & Summary**: Live progress indicator displaying current file, scanning counts, moved count, and category breakdown. Direct "Open Organized Folder" button upon completion.

4. **Dedicated Background Keyword Search Page (Multi-Format)**:
   - Separate dedicated navigation page for heavy content scanning across the entire file system.
   - Supports searching by single or **comma-separated multiple keywords** (e.g. `report, 2024, invoice` or `error, null, config`).
   - Deep storage scanner for finding keywords across all formats:
     - **Text & Code**: `.txt`, `.md`, `.log`, `.csv`, `.json`, `.xml`, `.kt`, `.java`, `.py`, `.sh`, `.conf`, `.ini`, `.properties`, `.yaml`, `.sql`, etc.
     - **Office Open XML Documents**: `.docx`, `.xlsx`, `.pptx`, `.odt`, `.ods` (in-memory XML stream scanner).
     - **PDF Documents**: Content stream and text token analysis.
     - **Binary & System Files**: ASCII string inspection.
   - **Background Running**: Runs asynchronously in the background while user browses other files or minimizes the app.
   - **System Notification & Direct Access**: Triggers Android notification (`POST_NOTIFICATIONS`) when the scan finishes, allowing users to tap the notification and open the Keyword Search results page directly.
   - Supports **Match ANY Keyword** or **Match ALL Keywords** mode with matched keyword badges, line snippets, and instant preview.

5. **Interactive Color-Coded Tagging & Bookmarks**:
   - Custom tags with vibrant color badges (`#system`, `#config`, `#important`, `#media`, `#code`, `#docs`, `#work`).
   - Fast filtering by single or multiple tags.
   - Room SQLite persistence with `FileTagCrossRef` relational mapping.

6. **Multi-Format Instant Previews**:
   - **Text / Code**: Monospace line previews with syntax readability.
   - **PDF Viewer**: High-fidelity bitmap page rendering via native Android `PdfRenderer`.
   - **Binary & System Files**: Formatted 16-byte Hex Dump viewer (`Offset | Hex Bytes | ASCII`).
   - **Audio Player**: Integrated audio preview manager with play/pause, duration, and scrubber.
   - **Images & Photos**: High-res Coil image rendering.

7. **Theming & Global Typography Scaling**:
   - **Theme Toggle (☀️ / 🌙)**: Instant switch between Light and Dark mode.
   - **Font Size & Display Scale (🔤)**: Dynamic typography scaling (Small 85%, Normal 100%, Large 115%, Extra Large 130%, Huge 150%) persisted via `AppSettings`.

8. **Permissions & Security**:
   - Persistent `MANAGE_EXTERNAL_STORAGE` verification on app start and resume (`onResume`).
   - Automated permission guidance window when access is not yet granted.
   - Zero sample file pollution; operates exclusively on real device files.

---

## 🛠️ Error Log & Solutions Reference (Knowledge Base)

This section documents all technical challenges and errors encountered during development and how each was resolved for future reference.

---

### 1. Kotlin Type Inference & Unresolved Stream Lambdas
* **Symptom / Error**: Build errors on `FileOutputStream` and `InputStream` in lambda scopes (e.g. `unresolved reference: FileOutputStream`, `cannot infer type for input.copyTo(output)`).
* **Root Cause**: Missing explicit imports for `java.io.FileOutputStream` and `java.io.InputStream` when chaining IO extension functions.
* **Resolution**:
  - Explicitly imported `java.io.FileOutputStream` and `java.io.InputStream`.
  - Used Kotlin's `.use { ... }` block scoping to ensure guaranteed stream closing and auto-flushing.

---

### 2. Android 11+ (API 30+) Scoped Storage & System Directory Visibility
* **Symptom / Error**: The app could not list system directories (`/system`, `/etc`) or external shared storage (`/sdcard/Downloads`), returning empty arrays or throwing `SecurityException: Permission denied`.
* **Root Cause**: Android 10+ scoped storage restrictions restrict standard `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions to media collections only.
* **Resolution**:
  - Declared `MANAGE_EXTERNAL_STORAGE` in `AndroidManifest.xml` with `tools:ignore="ScopedStorage"`.
  - Added runtime check using `Environment.isExternalStorageManager()`.
  - Implemented an `onResume` lifecycle hook that checks storage permission every time the user opens or resumes the app and triggers `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` if access is missing.

---

### 3. Native `PdfRenderer` Lifecycle & Seekable Descriptors
* **Symptom / Error**: `java.io.IOException: file not seekable` or native crash when rendering PDF files from content URIs or external storage.
* **Root Cause**: Android's `PdfRenderer` requires a seekable `ParcelFileDescriptor` opened in read-only mode (`MODE_READ_ONLY`). Direct streaming `InputStream` or unseekable pipe descriptors cause immediate failure.
* **Resolution**:
  - Built a dual-path resolver in `FileUtil.renderPdfFirstPage`:
    - For `file://` schemes: `ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)`.
    - For `content://` schemes: `context.contentResolver.openFileDescriptor(uri, "r")`.
  - Protected all resources in a strict `finally` block to prevent descriptor leaks.

---

### 4. Deep Keyword Search Across Multi-Format Files (.docx, .xlsx, .pdf, .txt)
* **Symptom / Error**: Standard SQLite FTS only searches pre-indexed database records. User-initiated keyword searches on device storage failed to find contents inside `.docx`, `.xlsx`, or raw text files on storage.
* **Root Cause**: Binary formats like Office Open XML and PDF cannot be read with plain line readers.
* **Resolution**:
  - Implemented `StorageSearchScanner`:
    - **Comma-Separated Parsing**: Tokenizes input strings into distinct lowercase search terms.
    - **Office Documents (`.docx`, `.xlsx`, `.pptx`)**: Scans the zipped XML entries (`word/document.xml`, `xl/sharedStrings.xml`) via `ZipInputStream` and strips XML markup tags to index textual content in memory.
    - **Plain Text / Code (`.txt`, `.md`, `.csv`, `.json`, `.kt`, `.log`)**: Streamed line-by-line using `useLines { }` with excerpt extraction.
    - **PDF Documents**: Fast ASCII run-length stream parser.
    - **Matched Keywords Highlighting**: Returns structured `SearchMatchResult` containing matched keyword tags, excerpt snippets, and match location.

---

### 5. Auto Organize Safety & System Path Protection
* **Symptom / Error**: Potential risks of moving system files, app-private directories (e.g. `Android/data/com.abc.app`), or creating infinite recursive folder loops inside the destination directory.
* **Root Cause**: Without strict path guards, recursive directory traversal could attempt to organize `.cache`, package internal folders, or the `/AutoOrganized` folder itself.
* **Resolution**:
  - Implemented `AutoOrganizeManager.isProtectedOrSystemPath`:
    - Blocks all destination directory paths and subdirectories to prevent recursive re-organizing.
    - Blocks `/system`, `/proc`, `/sys`, `/dev`, `/apex`, `/vendor`, `/etc`, `/product`.
    - Blocks `/Android/data`, `/Android/obb`, `/Android/sandbox`, and package names (`com.*`, `org.*`, `io.*`, `net.*`).
    - Skips hidden system directories (`.thumbnails`, `.cache`, `.git`).
  - Added safe collision resolution (`file_1.ext`) and synchronized path updates in Room SQLite database.

---

## 📱 Architecture Diagram

```
com.example
├── MainActivity.kt               # Entry point with Theme & Notification routing
├── data
│   ├── local                     # Room Database, DAOs, Entities, FTS4
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── FileDao.kt
│   │   │   └── TagDao.kt
│   │   └── entity/
│   │       ├── FileEntity.kt
│   │       ├── TagEntity.kt
│   │       ├── FileTagCrossRef.kt
│   │       ├── FileWithTags.kt
│   │       └── FileContentFTS.kt
│   ├── model
│   │   └── FileSystemItem.kt     # Unified model for file system + tag metadata
│   └── repository
│       └── FileManagerRepository.kt
├── ui
│   ├── FileManagerScreen.kt      # Main UI with 3-dot overflow drawer & tabs
│   ├── FileManagerViewModel.kt   # UI state machine, keyword search & organizer
│   ├── components/
│   │   ├── AutoOrganizeDialog.kt # Warning modal, folder/category checklist & progress
│   │   ├── KeywordSearchTabContent.kt # Dedicated background keyword search UI
│   │   ├── TagFilterHeader.kt    # Explorer tab search & tag filter chips
│   │   ├── TopPreviewCard.kt     # Instant preview (Text, PDF, Hex, Audio, Image)
│   │   ├── FileListItem.kt       # Adaptive file row with tags and context menu
│   │   ├── SearchMatchItem.kt    # Deep search result item with keyword badges
│   │   ├── TagSelectionBottomSheet.kt
│   │   ├── FontSizeDialog.kt     # Font scale selection dialog
│   │   ├── PermissionDialog.kt   # Storage permission guidance modal
│   │   ├── FileDetailsDialog.kt
│   │   ├── CreateFolderDialog.kt
│   │   ├── CreateFileDialog.kt
│   │   └── RenameFileDialog.kt
│   └── theme/
│       ├── Theme.kt              # Dynamic Dark/Light & LocalDensity font scale
│       ├── Color.kt
│       └── Type.kt
└── util
    ├── AutoOrganizeManager.kt    # Extension categorization engine & safe mover
    ├── NotificationHelper.kt     # System notifications on scan completion
    ├── AppSettings.kt            # Theme mode and font scale persistence
    ├── StorageSearchScanner.kt   # Multi-format deep keyword search engine
    ├── FileUtil.kt               # File category, hex dump, PDF render, audio meta
    └── AudioPreviewManager.kt    # Media player controller
```

---

## 🚀 Verification & Testing
- Target SDK: Android 14+ (API 34)
- Min SDK: Android 8.0 (API 26)
- Tested with Scoped Storage, Root/System partition navigation, Deep Multi-Format search, Auto Organize folder categorization, and Dynamic Theming.
