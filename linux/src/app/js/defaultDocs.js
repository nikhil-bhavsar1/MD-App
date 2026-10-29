// Default markdown documents for HyperMark Linux Desktop
const defaultDocuments = [
    {
        id: "doc_math_ctan",
        title: "CTAN & LaTeX Math Showcase",
        isPinned: true,
        updatedAt: Date.now(),
        content: `# CTAN & LaTeX Mathematics Laboratory

Welcome to **HyperMark** on Linux Fedora 44! This document demonstrates advanced LaTeX mathematics equations and CTAN package extensions.

## 1. Fundamental Quantum & Field Equations

The time-dependent Schrödinger equation in a central potential:

$$i\\hbar \\frac{\\partial}{\\partial t} \\Psi(\\mathbf{r}, t) = \\left[ -\\frac{\\hbar^2}{2m}\\nabla^2 + V(\\mathbf{r}) \\right] \\Psi(\\mathbf{r}, t)$$

The Einstein field equations with cosmological constant:

$$G_{\\mu\\nu} + \\Lambda g_{\\mu\\nu} = \\frac{8\\pi G}{c^4} T_{\\mu\\nu}$$

The Gaussian probability density integral:

$$\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}$$

## 2. CTAN Package Macro Demonstrations

### amsmath & physics
Using matrix notation and Dirac bra-ket formalism:

$$\\hat{H} |\\psi_n\\rangle = E_n |\\psi_n\\rangle$$

$$\\langle \\psi | \\hat{A} | \\phi \\rangle = \\sum_k \\langle \\psi | k \\rangle \\langle k | \\hat{A} | \\phi \\rangle$$

$$\\begin{pmatrix} a_{11} & a_{12} & a_{13} \\\\ a_{21} & a_{22} & a_{23} \\\\ a_{31} & a_{32} & a_{33} \\end{pmatrix} \\begin{pmatrix} x_1 \\\\ x_2 \\\\ x_3 \\end{pmatrix} = \\begin{pmatrix} b_1 \\\\ b_2 \\\\ b_3 \\end{pmatrix}$$

### Maxwell's Electromagnetism
In differential form:

$$\\nabla \\cdot \\mathbf{E} = \\frac{\\rho}{\\varepsilon_0}, \\quad \\nabla \\cdot \\mathbf{B} = 0$$
$$\\nabla \\times \\mathbf{E} = -\\frac{\\partial \\mathbf{B}}{\\partial t}, \\quad \\nabla \\times \\mathbf{B} = \\mu_0 \\mathbf{J} + \\mu_0 \\varepsilon_0 \\frac{\\partial \\mathbf{E}}{\\partial t}$$

> [!NOTE]
> All formulas render in real time using client-side KaTeX with sub-millisecond latency.
`
    },
    {
        id: "doc_topology_graphs",
        title: "Topology & Graph Architecture",
        isPinned: true,
        updatedAt: Date.now() - 3600000,
        content: `# Topology, Cytoscape & Mermaid Architecture

HyperMark supports embedded diagrams and interactive network topologies.

## 1. System Pipeline Architecture (Mermaid)

\`\`\`mermaid
flowchart TD
    A[Markdown Input] --> B[AST Lexer & Parser]
    B --> C{Syntax Node Triage}
    C -->|Math Block| D[KaTeX Engine]
    C -->|Mermaid Block| E[Mermaid Renderer]
    C -->|Cytoscape Block| F[Cytoscape Engine]
    C -->|GFM Markdown| G[HTML DOM Engine]
    D --> H[Live Preview Window]
    E --> H
    F --> H
    G --> H
\`\`\`

## 2. Network Topology Graph (Cytoscape)

\`\`\`cytoscape
{
  "nodes": [
    {"data": {"id": "core", "label": "Fedora 44 Core", "color": "#00E5FF"}},
    {"data": {"id": "vault", "label": "Local Vault", "color": "#38BDF8"}},
    {"data": {"id": "engine", "label": "AST Renderer", "color": "#C084FC"}},
    {"data": {"id": "graph", "label": "2D Topology Canvas", "color": "#34D399"}},
    {"data": {"id": "cache", "label": "Memory Cache", "color": "#FBBF24"}}
  ],
  "edges": [
    {"data": {"source": "core", "target": "vault", "label": "io"}},
    {"data": {"source": "core", "target": "engine", "label": "eval"}},
    {"data": {"source": "engine", "target": "graph", "label": "render"}},
    {"data": {"source": "core", "target": "cache", "label": "sync"}}
  ]
}
\`\`\`

## 3. Interactive File Tree & Connections
Check the **Topology Graph view** (press the 🕸️ button in the top bar or \`Ctrl+G\`) to visualize cross-document references in an organic 2D graph with radar pulsing animations.
`
    },
    {
        id: "doc_quickstart",
        title: "HyperMark Linux Quickstart",
        isPinned: false,
        updatedAt: Date.now() - 7200000,
        content: `# HyperMark Quickstart for Fedora 44

Welcome to **HyperMark** standalone desktop edition!

## Real LaTeX Engine (\`\`\`latex blocks)

Fenced **\`latex\`** blocks compile with the Tectonic engine straight to a
live PDF card inside the preview — including beyond-KaTeX packages like
\`siunitx\`, \`physics\`, or basic TikZ, which Tectonic fetches from CTAN
on first use:

\`\`\`latex
\\documentclass{article}
\\begin{document}
\\section{Hello}
Mass-energy: $E = mc^{2}$
\\end{document}
\`\`\`

The engine binary (~14 MB) is a manual one-click install from the card —
nothing downloads until you press the button.

## Desktop Keyboard Shortcuts

| Shortcut | Command | Action |
| :--- | :--- | :--- |
| `Ctrl + S` | Save File | Saves active document to local Linux vault |
| `Ctrl + N` | New Document | Creates a new blank markdown buffer |
| `Ctrl + F` | Find & Replace | Opens interactive in-document search bar |
| `Ctrl + G` | Topology Graph | Opens network visualization canvas |
| `Ctrl + M` | Radial Menu | Opens circular quick formatting palette |
| `Ctrl + P` | Preview Mode | Switches between Split and Preview |
| `Tab` | Indent / Nest | Indents 2 spaces (Enter continues lists) |


## Local Vault Storage
Your notes are stored as plain \`.md\` files in the Linux vault:

\`~/.local/share/hypermark/vault/\`

New documents, imports, renames and deletions go straight to that folder, and
edits are written back automatically. \`Ctrl + S\` saves immediately.

You can also use the **Open File** button in the sidebar or vault sheet to open any existing Markdown file from anywhere in your Fedora filesystem.

## Feature Highlights
- [x] High-performance 2D network topology graph with Bézier curved links
- [x] Multi-tiered radar pulse animation on selected nodes
- [x] Cytoscape JSON diagrams rendered as vector graphs in the preview
- [x] Collapsible Folder Tree view with item count badges
- [x] Dark translucent cyberpunk aesthetic with glowing borders
- [x] Standalone zero-installation executable for Fedora 44
`
    }
];
