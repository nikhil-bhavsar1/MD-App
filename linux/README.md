# HyperMark for Linux Fedora 44

Welcome to **HyperMark** for Linux Fedora 44!

## Standalone Zero-Installation Executable (Recommended)

1. Double-click **`HyperMark-1.0.0-x86_64.AppImage`** in Fedora Files (Nautilus).
2. It launches immediately — **no terminal commands, no installation, and no dependencies needed**.
3. If running on a fresh Fedora system that requires allowing execution:
   - Right click `HyperMark-1.0.0-x86_64.AppImage` → **Properties** → enable **Executable as Program**.

---

## Native Fedora RPM Package

If you prefer standard Fedora system integration:

1. Double-click **`hypermark-1.0.0-1.fc44.x86_64.rpm`** in Fedora Files.
2. Fedora **GNOME Software** will open; click **Install**.
3. HyperMark will appear in your application grid with its official icon and MIME associations for `.md` files.
4. Or install via terminal:
   ```bash
   sudo dnf install ./hypermark-1.0.0-1.fc44.x86_64.rpm
   ```

---

## Features

- **Dark Translucent Cyberpunk UI**: Obsidian `#070B13` theme with neon cyan, electric violet, and amber glow.
- **Real-Time Markdown & Math**: GFM Markdown with live KaTeX LaTeX formulas (`$..$` and `$$..$$`).
- **Interactive Diagrams**: Mermaid flowcharts, sequence diagrams, and Cytoscape network graphs.
- **2D Network Topology Graph**: Smooth curved Bézier connections with **real-time radar pulsing animation** on selected nodes.
- **Collapsible Folder Tree**: Grouped documentation categories with item statistics.
- **Local Linux Vault Storage**: Files are saved in `~/.local/share/hypermark/vault/`.
