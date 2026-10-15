# Coleta Fácil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Build an Android application that captures codes, accepts a manual name and Kronos status, persists collections and exports three columns to CSV/XLSX.

**Architecture:** Kotlin/Compose Android UI, CameraX and bundled ML Kit, Room persistence. A pure Java core owns barcode rearming and spreadsheet serialization so real production logic can be tested with the available JVM, independently of Android tooling.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, CameraX, ML Kit, Room, Java ZIP/XML, Gradle.

**Spec:** ../specs/2026-10-15-coletas-android-design.md

## Global Constraints
- Exactly Código, Nome, Confere no Kronos? in that order.
- Names and Kronos status entered manually; initial status Pendente.
- Local persistence, no account, bundled scanning model.
- CSV UTF-8 BOM and semicolon; XLSX code cells are text.
- Same code remains suppressed until removed from camera; intentional repeat creates a separate record.
- Review may export pending/unnamed entries; export success only after writing.

## Review Focus
- Exported formula-like codes are literal and leading zeros survive XLSX.
- Editing and navigating away does not lose confirmed edits.
- Cancelled/failed export never marks a collection complete.
- Late camera callbacks cannot add records after navigating away.
- Small screens, large text and denied camera permission keep manual entry available.

### Task 1: Pure production core and export formats
**Files:** core/src/main/java/br/com/coletafacil/core/{ScanRecord,ScanGate,SpreadsheetWriter}.java; core/src/test/java/br/com/coletafacil/core/CoreTests.java; tools/test-core.sh; tools/verify-exports.py.
**Interfaces:** ScanRecord(String code,String name,String status); ScanGate.candidate(Set<String>,long) and accepted(String), failed(String); SpreadsheetWriter.writeCsv(OutputStream,List<ScanRecord>) and writeXlsx(OutputStream,List<ScanRecord>).
- [x] Write assertions for suppression, disappearance/rearm, failed saves, semicolon/quote/newline/BOM and literal Excel text.
- [x] Run tests against empty implementations; expected assertion failure.
- [x] Implement core; run Java tests and independent openpyxl/CSV verification; expected all assertions pass.
- [x] Commit core and verification tools.

### Task 2: Android persistence, state and scanner
**Files:** app/src/main/java/br/com/coletafacil/{data/Database.kt,AppViewModel.kt,camera/Scanner.kt}; Gradle modules and manifest; app/src/androidTest/java/br/com/coletafacil/DatabaseTest.kt.
**Interfaces:** ViewModel exposes StateFlow<AppState>, select/create collection, suspend add/update/delete/restore, complete after export; Camera scanner reports one accepted rawValue at a time and returns a save result.
- [x] Add Room instrumentation tests for collection persistence, exact code/name/status and completion state.
- [x] Implement Room and serialized IO mutations; scanner uses ScanGate and lifecycle disposal.
- [x] Attempt Android compilation; tooling/network limitations recorded in docs/VALIDACAO.md. Instrumentation remains unexecuted.
- [x] Commit Android data and camera integration.

### Task 3: Compose app and packaging
**Files:** ui/{Theme,ColetaApp,HomeScreen,CollectionScreen,Dialogs}.kt; MainActivity.kt; README.md; .github/workflows/android.yml.
**Interfaces:** Screens consume the ViewModel; SAF creates an export URI, snapshot bytes are written on IO, then completion persists and success shows.
- [x] Add Compose tests for manual entry/status; denied-camera fallback requires real-device validation and is listed in the report.
- [x] Implement accessible UI semantics, saved drafts, confirmation/discard behavior, filters, undo deletion, export and reopening.
- [x] Run core/export verification; supply Gradle wrapper and a clear Android Studio build path. Android build remains unverified.
- [x] Review implementation and test evidence; source ZIP delivery proceeds with an explicit no-APK limitation.
