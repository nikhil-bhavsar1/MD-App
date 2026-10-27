# HyperMark for Linux

HyperMark runs great on Linux (tested on Fedora 44; works on any distribution).
It needs **no installation, no build, and no dependency commands** — the whole
desktop app (editor, KaTeX math, Mermaid diagrams, graph visualizer) is plain
HTML/JS served locally over `127.0.0.1` by a Python 3 standard-library launcher.

## Native window (no browser)

Like VS Code or Discord, the app opens inside a **real desktop application
window** — its own title bar, taskbar icon and window-tray behavior, with no
browser chrome. The launcher picks the first available engine:

1. **Electron runtime (default)** — self-contained, automatically bundled into
   the RPM/AppImage; for repo use, fetch it once with `linux/fetch-electron.sh`
   (cached in `linux/.cache/`, no system installation).
2. **WebKitGTK** — if your distro ships the engine, a lightweight GTK window.
3. **Browser app-window** — final fallback (Chromium/`--app`, GNOME Web, Firefox).

Force a specific engine with `HYPERMARK_WINDOW=browser|native|auto ./run.sh`.

All renderer libraries (KaTeX 0.16.9, Mermaid 10.6.1, Marked 9.1.6 and fonts)
are **bundled in `linux/src/app/vendor/`**, so the app is fully offline-capable.

## Option 1 — One-click run from the source tree (no install at all)

```bash
./run.sh
```

That single command (or double-clicking `run.sh` in your file manager,
"Run as Program") is the entire requirement — `python3` is preinstalled on
Fedora, Ubuntu, Debian, Arch, and openSUSE. Nothing is installed; the app
serves itself from `linux/src/app` and saves your vault in
`~/.local/share/hypermark/vault/`.

## Option 2 — Standalone click-to-run executable (portable, shareable)

Build once, run anywhere:

```bash
cd linux && ./package-appimage.sh
```

Then copy `linux/dist/HyperMark-1.0.0-x86_64.AppImage` to any Linux machine,
make it executable (`chmod +x`), and double-click or run it. It
self-extracts into a private per-user cache, needs no root, and downloads
nothing at runtime.

## Option 3 — RPM package (system integration for Fedora/RHEL/openSUSE)

Build the RPM with one command (no root needed, builds in-tree):

```bash
cd linux && ./build-rpm.sh
```

Result: `linux/rpmbuild/RPMS/<arch>/hypermark-<version>.<dist>.rpm` (also
copied to `linux/dist/`).

Install it by double-clicking in GNOME Software, or from the terminal:

```bash
sudo dnf install ./hypermark-1.0.0-1.fc44.x86_64.rpm
```

After installing, `hypermark` appears in your application grid with its icon
and `.md` file associations; the launcher itself is `/usr/bin/hypermark`.

## How it works

- `linux/src/hypermark-launcher.py` — starts a localhost HTTP server on a
  free port, then opens the UI in a native app window:
  Chromium/Brave/Edge (`--app=`) → GNOME Web/Epiphany (`--application-mode`)
  → Firefox → system default browser.
- `linux/src/app/` — the web UI (`index.html`, `js/app.js`,
  `js/defaultDocs.js`, `css/styles.css`, and the bundled `vendor/` libraries).
- `linux/src/hypermark.desktop` + `linux/src/icons/` — desktop entry and icons.
- `linux/package-appimage.sh` — builds the standalone click-to-run executable
  (`linux/dist/HyperMark-*.AppImage`) from `linux/src/AppDir` staging.
- `linux/build-rpm.sh` + `linux/rpmbuild/SPECS/hypermark.spec` — build the
  RPM from the same sources.
- Set `HYPERMARK_NO_BROWSER=1` to run the server without opening the window
  (useful for scripting/tests).

## Features

- **Dark Translucent Cyberpunk UI**: Obsidian `#070B13` theme with neon cyan, electric violet, and amber glow.
- **Radial Formatting Wheel**: GTA-style segmented wheel with 6 categories (Headings, Format, Math & Code, Lists & Tables, Insert, Vault) — open it via the 🌀 button bottom-right, **right-click the editor**, or `Ctrl+M`.
- **Open Local Files**: `📂 Open .md` in the sidebar (or the Vault wedge in the radial wheel) opens your system file chooser; imported files are copied into the vault so they persist.
- **Real-Time Markdown & Math**: GFM Markdown with live KaTeX LaTeX formulas (`$..$` and `$$..$$`); code spans are never misread as math, and preview scroll position survives re-renders.
- **Wiki Links**: `[[Title]]` renders as a clickable link that jumps to the matching document (and offers to create it when missing).
- **Interactive Diagrams**: Mermaid (flowchart, sequence, gantt, pie, ER, class, state) plus Cytoscape JSON graphs rendered as vector topologies. Every diagram renders at natural size in a pannable card with its own toolbar (⛶ fullscreen, + / − / ⟲, wheel-zoom, drag-pan).
- **Fullscreen Graph Viewer**: Click ⛶ to open a diagram fullscreen — zoom buttons, `+` / `−` / `0` keys, Fit, 1:1, drag-pan and **↗ Window** to pop the graph out into its own right window (a real native window under Electron; Esc returns to the editor).
- **2D Network Topology Graph**: Smooth curved Bézier connections with **real-time radar pulsing animation** on selected nodes. Tags are harvested from your content (`#tag`), wiki links become graph edges.
- **Emoji Quick-Insert Palette**: Insert category in the radial wheel.
- **Collapsible Folder Tree**: Grouped documentation categories with item statistics.
- **Real LaTeX Engine (Tectonic)**: \`\`\`latex blocks compile to PDF right in the preview — packages like `siunitx`/`physics`/basic TikZ fetched from CTAN on demand. Engine install is a user-clicked, 14 MB one-click; compile results are cached.
- **Package Browser**: the 🌀 wheel's **📦 Packages → Browse All** wedge (or \`Ctrl+Shift+B\`) opens a searchable browser of every LaTeX package, diagram renderer and markdown extension this build supports — click \`＋ Insert\` on any card to drop a working starter snippet into the editor.
- **Disk-Backed Vault**: Notes live as plain editable `.md` files in `~/.local/share/hypermark/vault/` (auto-saved as you type; deletes are undoable via toast).
