# Implementation Plan: HyperMark Standalone Click-to-Run for Linux (Fedora 44)

Provide a direct, standalone click-to-run executable for Fedora 44 with **zero installation**, **zero terminal commands**, and **zero external dependencies**, bundling its own self-contained runtime alongside a native Fedora RPM installer.

---

## 1. User Intent & Refined Specifications

* **Target Operating System**: Linux (Fedora 44 x86_64 / Wayland & X11).
* **Delivery Format**: **Direct standalone click-to-run executable** (self-contained AppImage / bundled portable executable).
  * **User Experience**: Double-click the file in Fedora's file manager (Files / Nautilus) to immediately launch HyperMark.
  * **Zero Setup**: No `sudo`, no DNF commands, no terminal needed, and no Java/JDK installation required (runtime is fully embedded).
  * **Secondary Output**: Standalone `.rpm` package for users who prefer standard Fedora system integration.
* **Preserved Application Features**:
  * Dark translucent cyberpunk aesthetic (`#070B13` obsidian background, neon cyan `#00E5FF`, electric violet `#C084FC`).
  * Real-time Markdown syntax highlighting and live split-screen preview.
  * Full LaTeX / CTAN math formulas and Mermaid / Cytoscape diagram rendering.
  * Interactive 2D Network Topology Graph with smooth curved Bézier connectors, glowing node halos, and real-time radar pulsing selection animation.
  * Collapsible Folder Tree View with file metrics.
  * Local Vault filesystem storage.

---

## 2. Technical Architecture for Zero-Installation Linux Desktop

### A. Self-Contained Desktop Runtime
* **Framework**: Compose Multiplatform for Desktop (Skia hardware-accelerated renderer on Wayland and X11).
* **Runtime Bundling**: Uses `jpackage` to bundle a trimmed, stripped JVM runtime inside the executable bundle. The user does not need to install Java on Fedora.
* **Direct Click-to-Run Packaging**:
  1. **Standalone AppImage / Portable Executable (`HyperMark-1.0.0-x86_64.AppImage`)**:
     * Self-mounting, compressed single executable file.
     * Contains the executable binary, embedded runtime, desktop entry, and icon.
     * Ready to run instantly on double-click in Fedora 44.
  2. **Native Fedora RPM (`hypermark-1.0.0.x86_64.rpm`)**:
     * Generated for single-click installation via Fedora GNOME Software.

### B. Linux Desktop Integration
* **File System Access**: Direct access to user documents (`~/Documents`, `~/.local/share/hypermark/vault`, or any directory selected via native file choosers).
* **Window Management**: Native Linux desktop window with custom dark translucent header bar, minimize, maximize, and close controls.
* **Keyboard Accelerators**: Standard desktop shortcuts (`Ctrl+N` new document, `Ctrl+S` save, `Ctrl+F` find/replace, `Ctrl+G` graph view, `Ctrl+O` open file).

---

## 3. Implementation Steps

### Step 1: Multiplatform Project Configuration
* Keep the existing Android app module (`:app`) completely intact so Android build and preview remain fully functional.
* Add the desktop subproject (`:desktop`) in `settings.gradle.kts` and configure Compose Desktop with `packageDistributionForCurrentOS` and `packageRpm`.

### Step 2: Standalone Desktop UI & Platform Adapters
* Create desktop entry point `Main.kt` and `DesktopApp.kt`.
* Implement desktop native file operations (open/save file dialogs and local directory monitoring).
* Connect shared markdown visual transformation and network graph canvas rendering to desktop mouse/trackpad events (zoom wheel, pan drag, node selection).

### Step 3: Zero-Installation Executable Packaging
* Configure the Gradle task to build the standalone self-contained portable distribution.
* Package into a single click-to-run executable file with embedded runtime and executable permissions.
* Copy the standalone executable directly into the root project directory (`/HyperMark-x86_64.AppImage` / portable binary) for direct download.

---

## 4. Verification & Testing Plan

1. **Android Integrity**: Execute `compile_applet` to confirm Android compilation remains unaffected.
2. **Desktop Build Verification**: Build the desktop module with `gradle :desktop:assemble`.
3. **Standalone Executable Output Verification**: Verify the generated standalone executable is self-contained and located in the project root directory ready for one-click launching.
