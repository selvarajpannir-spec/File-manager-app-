# Files+ ⚡
### *The Intelligent Android File Explorer, Deep Document Search Engine & Auto-Organizer*

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25_Offline-00C853?style=for-the-badge&logo=shield&logoColor=white)](#privacy-first-and-security)

---

## 💡 Why Files+ Reinvents the Way You Manage Files

Traditional file managers treat your device like a dusty storage locker: a labyrinth of nested folders, cryptic filenames, and rigid directories where finding a single document feels like searching for a needle in a haystack. 

**Files+ is built for how we actually use our devices today.** 

Instead of opening five different apps just to check a receipt, verify an invoice, or play an audio clip, **Files+** gives you an instant, fluid workspace. Whether you're searching inside 300-page PDF backups for a phone number, organizing thousands of messy downloads in one tap, or grouping project assets across different folders with color-coded tags, **Files+ saves you minutes on every single interaction.**

---

## ⚡ How Files+ Saves You Time Every Day

| Everyday Task | Classic File Managers | **With Files+** |
| :--- | :--- | :--- |
| **Check a PDF or Document** | Click file ➔ choose external app ➔ wait for heavy viewer to load ➔ exit back. | 👁️ **Instant Top Preview**: See page 1 of PDFs, photos, videos, and code right in your feed. |
| **Find Text or Phone Number in Documents** | Impossible without manually opening and skimming every file one by one. | 🔍 **Deep Multi-Format Search**: Searches *inside* PDFs, DOCX, XLSX, TXT, VCF, and Code files simultaneously. |
| **Messy Download & DCIM Folders** | Spend 30 minutes manually creating folders and dragging files. | ✨ **1-Tap Auto-Organize**: Scans hundreds of files and sorts them into neat, categorized folders in seconds. |
| **Organize Projects Across Storage** | Forced to duplicate files into rigid single-folder structures. | 🏷️ **Color-Coded Cross-Folder Tags**: Group files with `#Work`, `#Tax2024`, or `#Urgent` anywhere on your device. |
| **Listen to Voice Notes / Audio** | Launches an external media player that takes over your screen. | 🎵 **Inline Mini-Player**: Play, scrub, and pause audio clips right from the preview header. |
| **Inspect Code or Binary Logs** | Requires a dedicated text editor or desktop hex tool. | 💻 **Built-in Syntax & Hex Inspector**: View formatted code and 16-byte hex dumps with zero lag. |

---

## 🚀 Key Features You'll Love

### 1. 👁️ Instant Multi-Format File Preview (Zero App Switching)
No more waiting for third-party viewers to load. Tapping any file immediately activates the interactive **Top Preview Card**:
* 📄 **PDFs**: Ultra-crisp vector page rendering powered by native Android `PdfRenderer`.
* 🖼️ **Photos & Graphics**: High-definition image previews with zoom and metadata inspect.
* 🎵 **Audio & Voice Memos**: Inline playback with live scrubber, duration stamps, and waveform control.
* 🎬 **Video Files**: Seamless video frame extraction and instant player integration.
* 💻 **Code & Text**: Monospaced syntax display with line numbers for Python, Kotlin, JS, JSON, XML, Markdown, and shell scripts.
* 🔬 **Binary & System Inspector**: Formatted 16-byte Hex Dump viewer (`Offset | Hex Bytes | ASCII`) for logs and raw binaries.

---

### 2. 🔍 Deep Keyword & Content Search Engine
Traditional search only looks at filenames. **Files+ reads what's inside.**
* 📚 **Searches Inside Heavy Files**: Scans text inside **PDFs** (up to 400 pages per file), **Word** (`.docx`), **Excel** (`.xlsx`), **PowerPoint** (`.pptx`), and **vCard** contacts (`.vcf`).
* 📞 **Smart Phone Number & Entity Matching**: Searches flexible numeric patterns (e.g. searching `9876543210` matches `(987) 654-3210`, `+91-98765-43210`, and `987 654 3210`).
* ⚡ **Multi-Keyword Logic**: Filter with comma-separated terms (`invoice, 2024, paid`) using **Match ANY** or **Match ALL** modes.
* 🔔 **Background Scanning with Notification**: Run intensive scans in the background while continuing your work. Receive a rich Android notification when ready, with one-tap jump directly to the matched results.

---

### 3. ✨ 1-Tap Smart Auto-Organizer
Turn messy `Downloads`, `DCIM`, or `WhatsApp` directories into clean, structured libraries in seconds:
* 🗂️ **Automated Category Routing**:
  * 🖼️ **Images**: `.jpg`, `.png`, `.gif`, `.webp`, `.heic`, `.svg` ➔ `/AutoOrganized/Images/`
  * 📄 **Documents**: `.pdf`, `.docx`, `.doc`, `.txt`, `.md`, `.odt` ➔ `/AutoOrganized/Documents/`
  * 🎬 **Videos**: `.mp4`, `.mkv`, `.mov`, `.avi`, `.webm` ➔ `/AutoOrganized/Videos/`
  * 📊 **Sheets**: `.xlsx`, `.xls`, `.csv`, `.numbers` ➔ `/AutoOrganized/Sheets/`
  * 💻 **Code**: `.kt`, `.py`, `.js`, `.json`, `.html`, `.cpp`, `.sql` ➔ `/AutoOrganized/Code/`
  * 📦 **Archives & Apps**: `.zip`, `.rar`, `.7z`, `.tar`, `.apk` ➔ `/AutoOrganized/Archives/` & `/Apps/`
* 🛡️ **Fail-Safe Protection**: Automatically preserves system directories (`/system`, `/proc`, `/sys`) and app data (`/Android/data`).
* 🔁 **Collision-Proof**: Safely renames duplicates (`document_1.pdf`) and updates all SQLite database tag associations automatically.

---

### 4. 🏷️ Color-Coded Tagging & Quick Bookmarks
Break free from rigid folder trees:
* **Custom Tags**: Assign vibrant labels like `#Personal`, `#Work`, `#Receipts`, `#Important`, or `#Code`.
* **Universal Filtering**: Filter your whole device by tag with a single tap, regardless of which folder or subfolder the file lives in.
* **SQLite Persistence**: Backed by a local Room database with lightning-fast query indexing.

---

### 5. 🎨 Ergonomic Material 3 Design & Fluid Controls
* 🌗 **Smart Light / Dark Modes**: Seamless palette transition with deep OLED slate background (`#0F172A`) for battery savings.
* 🔤 **Dynamic Display & Font Scaling**: Choose from 5 typography scales (85% to 150%) that reflow text without clipping.
* 📱 **Adaptive Form Factor Support**: Optimized for compact phones, flip covers, large flagships, foldables, tablets, and Chromebooks.
* ⚡ **Instant Cold-Start Launch**: Dedicated lightweight `SplashActivity` with native window background styling ensures **zero black-screen delays** on startup.

---

## 🔒 Privacy First & 100% Offline

Your files are your private business.
* 🚫 **Zero Cloud Tracking**: All search indexing, PDF text extraction, and categorization happens **strictly on your device's CPU**.
* 🌐 **No Unnecessary Telemetry**: Your files, filenames, tags, and search keywords are never uploaded to any remote server.
* 🔐 **Full Scoped Storage Compliance**: Built for modern Android 11+ (API 30–34) with transparent all-files access permissions.

---

## 🏗️ Architecture & Technology Stack

Built following modern Android architecture guidelines:

```
com.example
├── SplashActivity.kt             # Lightweight, zero-lag launch entry point
├── MainActivity.kt               # Jetpack Compose UI container & navigation
├── data
│   ├── local                     # Room Database, DAOs, Entities, FTS4 Full-Text Search
│   │   ├── AppDatabase.kt
│   │   ├── dao/ (FileDao, TagDao)
│   │   └── entity/ (FileEntity, TagEntity, FileTagCrossRef, FileContentFTS)
│   ├── model/ (FileSystemItem)
│   └── repository/ (FileManagerRepository)
├── ui
│   ├── FileManagerScreen.kt      # Main responsive workspace & List-Detail pane
│   ├── FileManagerViewModel.kt   # UI state machine, deep search & organizer engine
│   ├── components/
│   │   ├── TopPreviewCard.kt     # Multi-format live preview (PDF, Media, Hex, Code)
│   │   ├── AutoOrganizeDialog.kt # Interactive categorization checklist & progress
│   │   ├── KeywordSearchTab.kt   # Background full-text search interface
│   │   ├── TagSelectionSheet.kt  # Material 3 tag manager
│   │   └── PromoAdBanner.kt      # Tight-fit non-intrusive promotion banner
│   └── theme/                    # Dynamic Material 3 colors, typography, shapes
└── util
    ├── StorageSearchScanner.kt   # Apache PDFBox & multi-format text extractor
    ├── AutoOrganizeManager.kt    # Extension categorization engine & safe mover
    ├── NotificationHelper.kt     # System notifications on scan completion
    ├── FileUtil.kt               # Category detection, Hex dump, PdfRenderer
    └── AppSettings.kt            # Datastore-backed theme & font scale preferences
```

* **Framework**: [Jetpack Compose (Material 3)](https://developer.android.com/jetpack/compose)
* **Language**: [Kotlin 1.9+](https://kotlinlang.org/)
* **Database**: [Room SQLite with FTS4 Full-Text Search](https://developer.android.com/training/data-storage/room)
* **Document Engine**: [Apache PDFBox for Android](https://github.com/TomRoush/PdfBox-Android)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
* **Concurrency**: Kotlin Coroutines & `StateFlow`

---

## 🚀 Getting Started

### Minimum Requirements
* **OS**: Android 8.0 (API Level 26) or higher
* **Target SDK**: Android 14+ (API Level 34)
* **Storage Access**: "All Files Access" (`MANAGE_EXTERNAL_STORAGE`) for full device management.

### Build & Run
```bash
# Clone the repository
git clone https://github.com/your-username/FilesPlus.git

# Navigate into the project directory
cd FilesPlus

# Build the debug APK
gradle assembleDebug

# Run unit and instrumented tests
gradle testDebugUnitTest
```

---

## 🌟 Why You'll Never Go Back to Classic File Managers

Files+ transforms file browsing from a tedious chore into a fast, intelligent, and enjoyable experience. Whether you're organizing gigabytes of media, quickly inspecting code on the go, or searching for an invoice buried deep in your storage, **Files+ gives you the control, speed, and intelligence you deserve.**

---
*Crafted with ❤️ for modern Android devices.*
