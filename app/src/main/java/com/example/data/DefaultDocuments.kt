package com.example.data

import com.example.model.MarkdownDocument

object DefaultDocuments {
    val initialDocument = MarkdownDocument(
        id = "showcase-doc-001",
        title = "Quick Start Guide",
        installedCtanPackages = "amsmath,physics,mhchem,xcolor",
        isPinned = true,
        content = """
# Quick Start Guide

> [!NOTE]
> HyperMark combines GFM, KaTeX with CTAN package loading, live Mermaid diagrams, and Cytoscape + Dagre directed graph visualization in a split-pane interface.

---

## 🌌 1. Graph Topology: Cytoscape + Dagre Hierarchical Layout

Interactive neural processing pipeline rendered using Cytoscape.js with the Dagre hierarchical graph layout engine:

```cytoscape
layout: dagre
nodes:
  - id: input, label: "Data Stream", color: "#38BDF8"
  - id: parser, label: "AST Tokenizer", color: "#818CF8"
  - id: katex, label: "KaTeX / CTAN", color: "#C084FC"
  - id: mermaid, label: "Mermaid Engine", color: "#F472B6"
  - id: cyto, label: "Cytoscape Dagre", color: "#34D399"
  - id: composer, label: "DOM Compositor", color: "#FBBF24"
  - id: screen, label: "Live Split Display", color: "#00E5FF"
edges:
  - source: input, target: parser, label: "Raw UTF-8"
  - source: parser, target: katex, label: "LaTeX tokens"
  - source: parser, target: mermaid, label: "Diagram AST"
  - source: parser, target: cyto, label: "Graph specs"
  - source: katex, target: composer, label: "MathML / SVG"
  - source: mermaid, target: composer, label: "Vector SVG"
  - source: cyto, target: composer, label: "Canvas"
  - source: composer, target: screen, label: "60 FPS"
```

---

## 📊 2. Mermaid Diagram Support

Live dynamic flowchart showing execution pipeline:

```mermaid
graph TD
    A[Markdown Buffer] --> B{Syntax Classifier}
    B -->|Code Block| C[Prism Highlight]
    B -->|LaTeX Segment| D[CTAN Macros + KaTeX]
    B -->|Graph Directive| E[Mermaid / Dagre]
    C --> F[Maximalist Dark Preview]
    D --> F
    E --> F
    style A fill:#1E293B,stroke:#38BDF8,stroke-width:2px,color:#F8FAFC
    style B fill:#312E81,stroke:#818CF8,stroke-width:2px,color:#F8FAFC
    style F fill:#0F172A,stroke:#00E5FF,stroke-width:3px,color:#38BDF8
```

---

## 🔬 3. LaTeX & CTAN Package Integration

Loaded CTAN packages: `\usepackage{amsmath}`, `\usepackage{physics}`, `\usepackage{mhchem}`, `\usepackage{xcolor}`.

### Quantum Mechanics (physics package)
Schrödinger equation with Dirac bra-ket operator expectation:

$$ i\hbar \frac{\partial}{\partial t} \ket{\Psi(t)} = \hat{H} \ket{\Psi(t)} $$

$$ \braket{\psi_n}{\hat{H}|\psi_m} = E_n \delta_{nm} $$

### Electromagnetism (Maxwell's Equations)
$$ \nabla \cdot \mathbf{E} = \frac{\rho}{\varepsilon_0}, \quad \nabla \times \mathbf{B} = \mu_0 \mathbf{J} + \mu_0 \varepsilon_0 \frac{\partial \mathbf{E}}{\partial t} $$

### Chemistry (mhchem package)
$$ \ce{6CO2 + 6H2O ->[light][chlorophyll] C6H12O6 + 6O2} $$

---

## 💻 4. Code Blocks with Syntax Highlighting

```kotlin
// Android Jetpack Compose Reactive Flow
val documentFlow: StateFlow<MarkdownDocument> = repository
    .observeActiveDocument()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarkdownDocument.EMPTY
    )
```

```rust
// Rust High-Performance Markdown Tokenizer
pub fn parse_cytoscape(input: &str) -> Result<GraphElements, ParseError> {
    let mut elements = GraphElements::new();
    for line in input.lines() {
        if line.starts_with("nodes:") { /* process nodes */ }
    }
    Ok(elements)
}
```

---

## 📈 5. Chart.js Data Visualizations

Interactive Bar and Line charts rendered on HTML5 Canvas:

```chart bar
labels: ["Q1 Alpha", "Q2 Beta", "Q3 Gamma", "Q4 Delta"]
dataset: "Throughput (K ops/s)", [58, 92, 134, 185], #00E5FF
```

```chart line
labels: ["00:00", "04:00", "08:00", "12:00", "16:00", "20:00"]
dataset: "Active Nodes", [12, 19, 38, 65, 54, 42], #C084FC
```

---

## 📋 6. GFM Tables, WikiLinks & Neon Highlights

Cross reference linked documents: [[🕸️ Cytoscape + Dagre Topology]] and [[🔬 CTAN & LaTeX Science Laboratory]].

Key findings: ==Real-time AST parsing with zero latency== and high efficiency.

| Flavor Component | Status | Engine | Acceleration |
| :--- | :--- | :--- | :--- |
| **CommonMark** | Verified | Marked v12 | JIT Native |
| **KaTeX Math** | Loaded | KaTeX 0.16 + CTAN | SVG Vector |
| **Cytoscape** | Active | Dagre Hierarchical | Canvas GPU |
| **Mermaid** | Active | Mermaid 10.9 | WebGL/SVG |
| **Chart.js** | Active | Chart.js 4.4 | Canvas 2D |

- [x] Fullscreen editor switch
- [x] Fullscreen preview switch
- [x] Adjustable split panes
- [x] CTAN package loader
- [x] Interactive File Tree Graph with manual rearrange
- [x] All MD flavor syntax palette

> [!TIP]
> Use the **Interactive File Tree Graph (🌳 icon)** in the top bar to inspect documents, tags, and connections!
""".trimIndent()
    )

    val graphDocument = MarkdownDocument(
        id = "graphs-doc-002",
        title = "🕸️ Cytoscape + Dagre Topology",
        installedCtanPackages = "amsmath",
        isPinned = false,
        content = """
# 🕸️ Cytoscape + Dagre Directed Graphs

Demonstration of hierarchical dependency graphs and network architectures using Cytoscape.js with Dagre layout.

```cytoscape
layout: dagre
nodes:
  - id: client, label: "Client App (Android)", color: "#00E5FF"
  - id: gateway, label: "API Gateway", color: "#38BDF8"
  - id: auth, label: "Auth Service", color: "#F472B6"
  - id: ctan_svc, label: "CTAN Engine", color: "#C084FC"
  - id: graph_svc, label: "Graph Engine", color: "#34D399"
  - id: storage, label: "Room SQLite", color: "#FBBF24"
edges:
  - source: client, target: gateway, label: "gRPC"
  - source: gateway, target: auth, label: "OAuth2"
  - source: gateway, target: ctan_svc, label: "Fetch TeX"
  - source: gateway, target: graph_svc, label: "Compute Layout"
  - source: ctan_svc, target: storage, label: "Cache Macros"
```

## Mermaid Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Developer
    participant Editor as 📝 Compose Editor
    participant Engine as ⚙️ HyperMark Engine
    participant Preview as 👁️ WebView Preview

    User->>Editor: Types Markdown + Math + Graphs
    Editor->>Engine: Debounced Stream (200ms)
    Engine->>Engine: Parse Cytoscape & Dagre AST
    Engine->>Preview: Inject Dynamic DOM
    Preview-->>User: 60FPS Rendered Visuals
```
""".trimIndent()
    )

    val ctanLabDocument = MarkdownDocument(
        id = "ctan-doc-003",
        title = "🔬 CTAN & LaTeX Science Laboratory",
        installedCtanPackages = "amsmath,physics,mhchem,siunitx,amssymb,cancel,xcolor",
        isPinned = false,
        content = """
# 🔬 CTAN & LaTeX Science Laboratory

Explore loaded CTAN package capabilities directly in HyperMark.

## 1. Physical Constants (`siunitx`)
- Speed of light: ${'$'}c = \SI{2.99792458e8}{\meter\per\second}${'$'}
- Planck constant: ${'$'}h = \SI{6.62607015e-34}{\joule\second}${'$'}
- Gravitational constant: ${'$'}G = \SI{6.67430e-11}{\meter\cubed\per\kilogram\per\second\squared}${'$'}

## 2. Quantum Matrix Mechanics (`physics`)
Pauli spin matrices $\sigma_x, \sigma_y, \sigma_z$:

$$
\sigma_x = \begin{pmatrix} 0 & 1 \\ 1 & 0 \end{pmatrix}, \quad
\sigma_y = \begin{pmatrix} 0 & -i \\ i & 0 \end{pmatrix}, \quad
\sigma_z = \begin{pmatrix} 1 & 0 \\ 0 & -1 \end{pmatrix}
$$

Commutator relation:
$$ [\sigma_i, \sigma_j] = 2i \varepsilon_{ijk} \sigma_k $$

## 3. Algebraic Cancellation (`cancel`)
$$ \frac{x^2 - 9}{x - 3} = \frac{(x + 3)\cancel{(x - 3)}}{\cancel{x - 3}} = x + 3 $$

## 4. Chemical Synthesis (`mhchem`)
$$ \ce{2H2 + O2 -> 2H2O} $$
$$ \ce{Fe^{2+} + 2OH- -> Fe(OH)2 v} $$
""".trimIndent()
    )
}
