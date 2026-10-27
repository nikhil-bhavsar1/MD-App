package com.example.data

import com.example.model.CtanCategory
import com.example.model.CtanPackage

object CtanCatalog {
    private val dynamicPackages: MutableList<CtanPackage> = mutableListOf(
        CtanPackage(
            id = "amsmath",
            name = "amsmath",
            caption = "AMS mathematical facilities for LaTeX",
            description = "Fundamental mathematical typesetting package from the American Mathematical Society. Provides aligned equations, matrices, multiline formulas, and extensive operators.",
            author = "American Mathematical Society",
            version = "2.17m",
            category = CtanCategory.MATH,
            macros = mapOf(
                "\\iint" to "\\int\\!\\!\\int",
                "\\iiint" to "\\int\\!\\!\\int\\!\\!\\int",
                "\\dddot" to "\\overset{\\dots}",
                "\\varinjlim" to "\\lim",
                "\\varprojlim" to "\\lim"
            ),
            sampleSnippet = """
```latex
\begin{align*}
  \nabla \times \mathbf{E} &= -\frac{\partial \mathbf{B}}{\partial t} \\
  \nabla \cdot \mathbf{B} &= 0 \\
  \nabla \times \mathbf{B} &= \mu_0 \mathbf{J} + \mu_0 \varepsilon_0 \frac{\partial \mathbf{E}}{\partial t}
\end{align*}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "physics",
            name = "physics",
            caption = "Macros supporting the typesetting of physics notation",
            description = "Provides intuitive commands for quantum Dirac bra-ket notation, differentials, vectors, matrices, Pauli matrices, derivatives, and commutators.",
            author = "Bryan Arsenault",
            version = "1.3",
            category = CtanCategory.PHYSICS,
            macros = mapOf(
                "\\bra" to "\\langle #1 \\vert",
                "\\ket" to "\\vert #1 \\rangle",
                "\\braket" to "\\langle #1 \\vert #2 \\rangle",
                "\\ketbra" to "\\vert #1 \\rangle\\langle #2 \\vert",
                "\\matrixel" to "\\langle #1 \\vert #2 \\vert #3 \\rangle",
                "\\eval" to "\\left. #1 \\right|_{#2}^{#3}",
                "\\abs" to "\\left| #1 \\right|",
                "\\norm" to "\\left\\| #1 \\right\\|",
                "\\pdv" to "\\frac{\\partial #1}{\\partial #2}",
                "\\comm" to "\\left[ #1, #2 \\right]"
            ),
            sampleSnippet = """
```latex
% Quantum Hamilton-Jacobi with Dirac Braket Notation
\hat{H} \ket{\psi(t)} = i\hbar \frac{\partial}{\partial t} \ket{\psi(t)}

\braket{\phi}{\psi} = \int \phi^*(x) \psi(x) \, dx

\matrixel{\psi_n}{\hat{O}}{\psi_m} = \delta_{nm} \lambda_n
```
""".trimIndent()
        ),
        CtanPackage(
            id = "mhchem",
            name = "mhchem",
            caption = "Typeset chemical formulas, reactions and equations",
            description = "Comprehensive chemistry typesetting supporting isotope notation, chemical reaction equations with equilibrium arrows, stoichiometry, and state symbols.",
            author = "Martin Hensel",
            version = "4.09",
            category = CtanCategory.CHEMISTRY,
            macros = mapOf(
                "\\ce" to "\\mathrm{#1}"
            ),
            sampleSnippet = """
```latex
% Photosynthesis and Chemical Synthesis
\ce{6CO2 + 6H2O ->[light][chlorophyll] C6H12O6 + 6O2}

\ce{238_92U -> 234_90Th + 4_2\alpha}

\ce{H+ + OH- <=> H2O}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "siunitx",
            name = "siunitx",
            caption = "Comprehensive (SI) units package",
            description = "Typesetting of values, physical units, scientific notation, uncertainties, angles, and unit conversions compliant with the International System of Units (SI).",
            author = "Joseph Wright",
            version = "3.3.4",
            category = CtanCategory.PHYSICS,
            macros = mapOf(
                "\\SI" to "#1\\,\\text{#2}",
                "\\si" to "\\text{#1}",
                "\\num" to "#1"
            ),
            sampleSnippet = """
```latex
c = \SI{2.99792458e8}{\meter\per\second}
G = \SI{6.67430(15)e-11}{\meter\cubed\per\kilogram\per\second\squared}
h = \SI{6.62607015e-34}{\joule\second}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "amssymb",
            name = "amssymb",
            caption = "Advanced TeX math font symbols",
            description = "Defines blackboard bold characters (R, C, N, Z, Q), gothic fraktur fonts, Hebrew symbols, extra relations, and geometric operators.",
            author = "American Mathematical Society",
            version = "3.01",
            category = CtanCategory.MATH,
            macros = mapOf(
                "\\mathbb" to "\\mathbf",
                "\\therefore" to "\\dot{..}",
                "\\because" to "\\ddot{.}",
                "\\subsetneq" to "\\subsetneqq"
            ),
            sampleSnippet = """
```latex
\forall x \in \mathbb{R}, \quad e^{ix} = \cos(x) + i\sin(x) \in \mathbb{C}
\mathbb{N} \subset \mathbb{Z} \subset \mathbb{Q} \subset \mathbb{R} \subset \mathbb{C}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "mathtools",
            name = "mathtools",
            caption = "Mathematical tools to use with amsmath",
            description = "Extends amsmath with fixes and additions: paired delimiters, multiline sub/superscripts, colon equals := (\\coloneqq), and matrix annotations.",
            author = "Morten Høgholm, Lars Madsen",
            version = "1.29",
            category = CtanCategory.MATH,
            macros = mapOf(
                "\\coloneqq" to ":=",
                "\\eqqcolon" to "=:",
                "\\prescript" to "{}^{#1}_{#2}#3"
            ),
            sampleSnippet = """
```latex
f(x) \coloneqq \sum_{n=1}^{\infty} \frac{(-1)^{n+1}}{n^s}
\begin{pmatrix*}[r]
  -10 & 2 \\
  3 & -405
\end{pmatrix*}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "cancel",
            name = "cancel",
            caption = "Place lines through math for algebraic cancellation",
            description = "Allows diagonal strike-through strokes in mathematical expressions to illustrate simplification of fractions and algebraic terms.",
            author = "Donald Arseneau",
            version = "2.2",
            category = CtanCategory.MATH,
            macros = mapOf(
                "\\cancel" to "\\rlap{/}{\\vphantom{#1}}#1",
                "\\bcancel" to "\\rlap{\\backslash}{\\vphantom{#1}}#1"
            ),
            sampleSnippet = """
```latex
\frac{x^2 - 1}{x - 1} = \frac{(x + 1)\cancel{(x - 1)}}{\cancel{x - 1}} = x + 1
```
""".trimIndent()
        ),
        CtanPackage(
            id = "tikz",
            name = "tikz",
            caption = "TikZ & PGF vector graphics system",
            description = "Powerful TeX-based system for programmatic creation of 2D/3D technical drawings, graph automata, commutative diagrams, and circuits.",
            author = "Till Tantau, Christian Feuersänger",
            version = "3.1.10",
            category = CtanCategory.GRAPHICS,
            macros = emptyMap(),
            sampleSnippet = """
```latex
% TikZ State Transition Diagram
\begin{tikzpicture}[node distance=2cm, auto]
  \node[state, initial] (q_0) {${'$'}q_0${'$'}};
  \node[state, accepting] (q_1) [right of=q_0] {${'$'}q_1${'$'}};
  \path[->] (q_0) edge [loop above] node {0} ()
                  edge node {1} (q_1)
            (q_1) edge [loop above] node {1} ()
                  edge [bend left] node {0} (q_0);
\end{tikzpicture}
```
""".trimIndent()
        ),
        CtanPackage(
            id = "braket",
            name = "braket",
            caption = "Dirac bra-ket notation and sets",
            description = "Provides clean Dirac bra-ket notations for quantum states, expectation values, and mathematical set builders with auto-sizing vertical bars.",
            author = "Donald Arseneau",
            version = "1.1",
            category = CtanCategory.PHYSICS,
            macros = mapOf(
                "\\Braket" to "\\left\\langle #1 \\right\\rangle",
                "\\Set" to "\\left\\{ #1 \\right\\}"
            ),
            sampleSnippet = """
```latex
\Set{ x \in \mathbb{R} \mid x^2 < 2 }
\Braket{ \phi | \frac{\partial^2}{\partial x^2} | \psi }
```
""".trimIndent()
        ),
        CtanPackage(
            id = "xcolor",
            name = "xcolor",
            caption = "Driver-independent color extensions for LaTeX",
            description = "Supports colored text, colored math glyphs, background tints, shade mixtures, and custom color definitions.",
            author = "Uwe Kern",
            version = "2.14",
            category = CtanCategory.FORMATTING,
            macros = mapOf(
                "\\textcolor" to "\\color{#1}{#2}",
                "\\colorbox" to "\\text{\\color{#1}{#2}}"
            ),
            sampleSnippet = """
```latex
\textcolor{#00E5FF}{\text{Quantum Core: }} \int_{-\infty}^\infty \textcolor{#C084FC}{\psi^*(x)} \hat{p} \textcolor{#38BDF8}{\psi(x)} \, dx
```
""".trimIndent()
        )
    )

    val packages: List<CtanPackage> get() = dynamicPackages

    fun addOrUpdatePackage(pkg: CtanPackage) {
        val index = dynamicPackages.indexOfFirst { it.id.equals(pkg.id, ignoreCase = true) }
        if (index >= 0) {
            dynamicPackages[index] = pkg
        } else {
            dynamicPackages.add(pkg)
        }
    }

    fun updatePackageVersion(packageId: String, newVersion: String) {
        val index = dynamicPackages.indexOfFirst { it.id.equals(packageId, ignoreCase = true) }
        if (index >= 0) {
            val old = dynamicPackages[index]
            dynamicPackages[index] = old.copy(version = newVersion)
        }
    }

    fun findPackage(id: String): CtanPackage? {
        return dynamicPackages.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    fun search(query: String): List<CtanPackage> {
        if (query.isBlank()) return dynamicPackages
        val q = query.trim().lowercase()
        return dynamicPackages.filter {
            it.name.lowercase().contains(q) ||
            it.caption.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.name.lowercase().contains(q) ||
            it.id.lowercase().contains(q)
        }
    }
}
