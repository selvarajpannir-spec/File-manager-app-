# Files+ ⚡
### *The Intelligent Android File Explorer, Deep Document Search Engine & Auto-Organizer*

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-v3.0_(Build_3)-3B82F6?style=for-the-badge&logo=googleplay&logoColor=white)](#whats-new-in-v30)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25_Offline-00C853?style=for-the-badge&logo=shield&logoColor=white)](#privacy-first-and-security)

---

## 🌟 Interactive Demonstration Page
> 🌐 **Experience the live interactive demo page right now:**  
> Open [`app_guide.html`](./app_guide.html) (or [`demo.html`](./demo.html)) in any web browser to test the interactive phone mockup, live preview tab switcher, checkbox multi-selection, and batch sharing simulation!  
> You can also launch this guide directly inside the Android app from the **⋮ (3-Dot Menu) ➔ 📖 How It Works / App Guide**.

---

## 📱 App Layout & Interface Architecture

Files+ organizes your screen with an ergonomic, zero-friction layout built for modern handheld Android devices:

```
┌──────────────────────────────────────────────────────────┐
│ [AD] Files+ Pro Suite • 80 dp Promotional Banner         │
├──────────────────────────────────────────────────────────┤
│ 📁  /Documents/Invoices/          [🔍] [🏷️] [🔃] [⋮ Menu] │  <-- Single Unified Navigation Bar
├──────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────┐ │
│ │ 📄 Invoice_Oct2026.pdf               [Instant Preview]│ │  <-- Top Instant Preview Card
│ │ INVOICE #INV-2026-904 • Acme Global                  │ │      (PDF, Photo, Audio player,
│ │ Customer Phone: +1 (555) 987-6543 • Status: PAID     │ │       Code syntax, Tables, Hex dump)
│ └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│ [☑️] Select All (3 files)     [📤 Share] [🗑️ Delete] [✕] │  <-- Multi-Select Batch Action Toolbar
├──────────────────────────────────────────────────────────┤
│ [☑] 📄  Invoice_Oct2026.pdf       142 KB  • #Finance     │
│ [☑] 📝  Project_Brief.docx         48 KB  • #Work        │  <-- File List with Checkboxes
│ [ ] 🖼️  Site_Inspection.jpg       2.4 MB  • #Photos      │      (Instant preview active while checking!)
│ [ ] 🎵  Meeting_Memo.m4a          3.8 MB                 │
│ [ ] 💻  Server_Script.py           12 KB  • #Code        │
├──────────────────────────────────────────────────────────┤
│ [📁 Explorer]    [🔍 Search]    [🏷️ Tags]    [📊 Storage] │  <-- Bottom Navigation Bar
└──────────────────────────────────────────────────────────┘
```

---

## 📖 How to Use Files+ (Step-by-Step Walkthrough)

### 1. 👁️ Instant Top File Preview (Zero App Switching)
* **How it works:** Whenever you tap or check any file in your storage list, the **Top Preview Card** instantly extracts and renders its contents right on your screen without leaving the app.
* **Supported Formats:**
  * **📄 PDF Documents:** Instant page-1 vector rendering using native Android `PdfRenderer`.
  * **🖼️ Images & Graphics:** High-definition thumbnails with dimensions, resolution, and EXIF metadata.
  * **🎵 Audio & Voice Memos:** Built-in mini-player with interactive play/pause, scrubbable progress bar, and elapsed time counter.
  * **🎬 Video Files:** Instant video thumbnail generation with duration and container specs.
  * **💻 Code & Plain Text:** Monospaced syntax display with line numbers for Python, Kotlin, JavaScript, JSON, XML, HTML, Shell, and Markdown.
  * **📊 CSV & Excel Spreadsheets:** Formatted table viewer displaying rows, columns, and numeric cells.
  * **🔬 Binary & Firmware Logs:** 16-byte formatted Hex Dump viewer (`Offset | Hex Bytes | ASCII`).
* **Tip:** Tap the eye icon or preview banner to open the full-screen interactive reader with text search, copy-to-clipboard, and zoom controls.

---

### 2. ☑️ Multi-File Checkbox Selection & Batch Sharing
* **How it works:** Every file row includes an accessible Material 3 **Checkbox**.
* **Select One or All:**
  * Tap an individual checkbox to toggle selection on that file.
  * Tap the **Select All** header checkbox to instantly select all files in the current folder.
* **Continuous Instant Preview:**
  * Checking a file's checkbox automatically updates the top preview card to that file, so you can review content before sharing or deleting.
* **Batch Native Sharing:**
  * When one or more files are selected, tap **📤 Share** on the batch toolbar.
  * Files+ bundles all selected items and invokes Android's native `Intent.ACTION_SEND_MULTIPLE` via secure `FileProvider` URIs.
  * Share multiple documents, photos, or audio clips simultaneously to WhatsApp, Gmail, Google Drive, Telegram, Slack, or Bluetooth.
* **Batch Safe Deletion:**
  * Tap **🗑️ Delete** to delete all selected files at once with a safe confirmation prompt.

---

### 3. 💾 Session State Memory & Zero Root Reset
* **Exit & Reopen Memory:**
  * When you exit or close Files+, the app automatically saves your:
    1. Active folder directory path (e.g. `/storage/emulated/0/Download/Work`)
    2. Active tab (File Explorer, Deep Search, Tags, or Storage)
    3. Last previewed file item
  * When you relaunch Files+, it opens **immediately to the exact screen, folder, and file preview** where you left off.
* **Safe Deletion Navigation (No Root Reset):**
  * In classic file managers, deleting a file often resets the screen back to the Linux root (`/`) or empty home screen.
  * In **Files+**, deleting a file (single or batch) updates the current list in-place and safely refreshes while strictly keeping you on the **exact same page/folder**.
* **Storage Boundary Guard:**
  * Navigating up with the parent folder button (`nav_up_button`) stops safely at primary user storage (`/storage/emulated/0`), eliminating "empty folder" confusion caused by navigating into restricted Linux root partitions.

---

### 4. 🔍 Deep Inside-Document Search Engine
Standard file managers only match file names. **Files+ reads the text inside your files.**

* **Supported Document Types:** Scans text content inside **PDFs** (up to 400 pages), **Word documents** (`.docx`), **Excel spreadsheets** (`.xlsx`), **PowerPoint presentations** (`.pptx`), **CSV tables**, **vCard contacts** (`.vcf`), and code files.
* **📞 Smart Phone Number & Entity Matching:**
  * Matches normalized numbers regardless of spacing, dashes, or country codes.
  * Searching `9876543210` automatically matches `+91-98765-43210`, `(987) 654-3210`, and `987 654 3210`.
* **⚡ Multi-Keyword Logic:**
  * Enter comma-separated search terms (e.g. `invoice, tax, 2026`).
  * Choose **Match ANY** (finds files containing any term) or **Match ALL** (finds files containing all terms).
* **🔔 Non-Blocking Background Scanning & Synced Notifications:**
  * Deep scans run asynchronously in a foreground service so the app never hangs.
  * A system notification displays live progress and seamlessly synchronizes into a "Scan Completed" notification with a 1-tap jump to search results.

---

### 5. ✨ 1-Tap Smart Auto-Organizer
Turn cluttered `Downloads`, `DCIM`, or `WhatsApp` folders into organized libraries with a single tap:

1. Tap **⋮ (3-Dot Menu) ➔ ✨ Auto Organize Files**.
2. Select which categories to sort:
   * 🖼️ **Images**: `.jpg`, `.png`, `.gif`, `.webp`, `.heic`, `.svg` ➔ `/AutoOrganized/Images/`
   * 📄 **Documents**: `.pdf`, `.docx`, `.doc`, `.txt`, `.md`, `.odt` ➔ `/AutoOrganized/Documents/`
   * 🎬 **Videos**: `.mp4`, `.mkv`, `.mov`, `.avi`, `.webm` ➔ `/AutoOrganized/Videos/`
   * 🎵 **Audio**: `.mp3`, `.m4a`, `.wav`, `.flac`, `.ogg` ➔ `/AutoOrganized/Audio/`
   * 📊 **Sheets**: `.xlsx`, `.xls`, `.csv`, `.numbers` ➔ `/AutoOrganized/Sheets/`
   * 💻 **Code**: `.kt`, `.py`, `.js`, `.json`, `.html`, `.cpp`, `.sql` ➔ `/AutoOrganized/Code/`
   * 📦 **Archives & Apps**: `.zip`, `.rar`, `.7z`, `.tar`, `.apk` ➔ `/AutoOrganized/Archives/` & `/Apps/`
3. Tap **Organize Now**. Files are safely moved with automatic collision avoidance (renaming duplicates like `invoice_1.pdf`) while preserving all tag associations.

---

### 6. 🏷️ Color-Coded Cross-Folder Tagging
* Assign custom tags (e.g. `#Taxes2026`, `#Receipts`, `#ClientA`, `#Urgent`) to any file or folder.
* Switch to the **🏷️ Tagged Files** tab to view and filter all tagged documents across your entire device in one unified view, regardless of which nested directory they reside in.
* Tag data is stored locally in a high-speed SQLite database powered by Room.

---

### 7. 📖 3-Dot Overflow Menu Drawer
Tap the **⋮** button on the top navigation bar to open the quick-access drawer:
* **📁 File Explorer:** Jump back to directory browsing.
* **🔍 Deep Keyword Search:** Access full-text in-document search.
* **🏷️ Tagged Files:** Open your tagged collection.
* **📊 Storage Partitions Stats:** Check disk space breakdown (Total, Used, Free).
* **✨ Auto Organize Files:** Launch 1-tap sorting.
* **➕ New Folder / New Text File:** Create new storage items.
* **📥 Import via SAF:** Import files via the Android Storage Access Framework.
* **🌙 Dark / Light Theme:** Toggle between OLED slate dark mode (`#0F172A`) and daylight mode.
* **🔤 Font Size:** Reflow text size dynamically (85% to 150%).
* **📖 How It Works / App Guide:** Open the built-in interactive demonstration guide and launch the bundled HTML showcase page!

---

## 🔒 Privacy First & 100% Offline

* **🚫 Zero Cloud Tracking:** All search indexing, PDF text extraction, and categorization happens **strictly on your device's CPU**.
* **🌐 No Unnecessary Telemetry:** Your files, filenames, tags, and search keywords are never uploaded to any remote server.
* **🔐 Full Scoped Storage Compliance:** Built for modern Android (API 24 to 36) with transparent all-files access permissions.

---

## 🏗️ Architecture & Technology Stack

```
com.example
├── MainActivity.kt               # Jetpack Compose UI container & lifecycle routing
├── data
│   ├── local                     # Room Database, DAOs, Entities, FTS4 Full-Text Search
│   │   ├── AppDatabase.kt
│   │   ├── dao/ (FileDao, TagDao)
│   │   └── entity/ (FileEntity, TagEntity, FileTagCrossRef, FileContentFTS)
│   ├── model/ (FileSystemItem)
│   └── repository/ (FileManagerRepository)
├── ui
│   ├── FileManagerScreen.kt      # Main responsive workspace & unified navigation
│   ├── FileManagerViewModel.kt   # UI state machine, multi-selection & persistence engine
│   ├── components/
│   │   ├── AppGuideDialog.kt     # In-app interactive guide & HTML demo launcher
│   │   ├── TopPreviewCard.kt     # Multi-format live preview (PDF, Media, Hex, Code)
│   │   ├── FileListItem.kt       # List row with Checkbox, tags, and category styling
│   │   ├── AutoOrganizeDialog.kt # 1-tap categorization checklist & progress
│   │   ├── KeywordSearchTab.kt   # Deep in-file content search engine
│   │   ├── TagSelectionSheet.kt  # Material 3 tag manager
│   │   └── PromoAdBanner.kt      # 80.dp fitted promotional ad banner
│   └── theme/                    # Dynamic Material 3 colors, typography, shapes
└── util
    ├── StorageSearchScanner.kt   # Apache PDFBox & multi-format text extractor
    ├── AutoOrganizeManager.kt    # Extension categorization engine & safe mover
    ├── NotificationHelper.kt     # System notifications on scan completion
    ├── FileUtil.kt               # Category detection, Hex dump, PdfRenderer
    └── AppSettings.kt            # Datastore & SharedPreferences session state memory
```

---

## 🚀 Getting Started

### Minimum Requirements
* **OS**: Android 8.0 (API Level 26) or higher (Target SDK: Android 14/15, API 36)
* **Storage Access**: All Files Access (`MANAGE_EXTERNAL_STORAGE`)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/your-username/FilesPlus.git

# Navigate into the project directory
cd FilesPlus

# Build the debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 📄 Documentation Assets
* **[`app_guide.html`](./app_guide.html)**: Interactive standalone HTML showcase with live demo phone layout.
* **[`demo.html`](./demo.html)**: Quick redirect alias to `app_guide.html`.

---
*Crafted with ❤️ for modern Android devices • Version 3.0 (Build 3)*
