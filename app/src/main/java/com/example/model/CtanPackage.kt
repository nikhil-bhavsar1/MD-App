package com.example.model

data class CtanPackage(
    val id: String,
    val name: String,
    val caption: String,
    val description: String,
    val author: String,
    val version: String,
    val category: CtanCategory,
    val macros: Map<String, String> = emptyMap(),
    val sampleSnippet: String,
    val documentationUrl: String = "https://ctan.org/pkg/$id"
)

enum class CtanCategory(val label: String, val badgeColor: Long) {
    MATH("Mathematics", 0xFF38BDF8),
    PHYSICS("Physics & Quantum", 0xFFC084FC),
    CHEMISTRY("Chemistry", 0xFF34D399),
    GRAPHICS("Diagrams & Graphs", 0xFFF59E0B),
    FORMATTING("Typography & Layout", 0xFFF472B6)
}

enum class ViewMode(val label: String) {
    SPLIT("Split 50/50"),
    EDITOR_ONLY("Editor Fullscreen"),
    PREVIEW_ONLY("Preview Fullscreen")
}

enum class EditorFont(val label: String) {
    MONOSPACE("Monospace (JetBrains)"),
    SANS_SERIF("Modern Sans"),
    SERIF("Editorial Serif")
}

data class EditorSettings(
    val editorFontSizeSp: Int = 14,
    val previewFontSizeSp: Int = 15,
    val fontChoice: EditorFont = EditorFont.MONOSPACE,
    val isWordWrapEnabled: Boolean = true,
    val showLineNumbers: Boolean = true,
    val isHorizontalSplit: Boolean = true // Horizontal = Side-by-Side, Vertical = Top-Bottom
)
