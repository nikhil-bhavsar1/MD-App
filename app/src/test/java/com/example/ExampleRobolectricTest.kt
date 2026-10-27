package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CtanCatalog
import com.example.data.DefaultDocuments
import com.example.util.MarkdownVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HyperMark", appName)
    }

    @Test
    fun `ctan catalog contains essential math and physics packages`() {
        val amsmath = CtanCatalog.findPackage("amsmath")
        assertNotNull("amsmath should exist in CTAN catalog", amsmath)
        assertTrue(amsmath!!.macros.isNotEmpty())

        val physics = CtanCatalog.findPackage("physics")
        assertNotNull("physics should exist in CTAN catalog", physics)
        assertTrue(physics!!.macros.containsKey("\\bra"))

        val mhchem = CtanCatalog.findPackage("mhchem")
        assertNotNull("mhchem should exist in CTAN catalog", mhchem)
    }

    @Test
    fun `initial showcase document contains mermaid, cytoscape, and chart blocks`() {
        val doc = DefaultDocuments.initialDocument
        assertTrue("Contains cytoscape block", doc.content.contains("```cytoscape"))
        assertTrue("Contains mermaid block", doc.content.contains("```mermaid"))
        assertTrue("Contains chart block", doc.content.contains("```chart bar"))
        assertTrue("Contains math display", doc.content.contains("$$"))
    }

    @Test
    fun `visual transformation supports code highlight and wikilinks`() {
        val vt = MarkdownVisualTransformation()
        val text = androidx.compose.ui.text.AnnotatedString("==highlighted== and [[LinkedDoc]]")
        val transformed = vt.filter(text)
        assertNotNull(transformed)
        assertEquals("==highlighted== and [[LinkedDoc]]", transformed.text.text)
    }

    @Test
    fun `unicode repository provides emoji suggestions for shortcode prefix`() {
        val repo = com.example.data.UnicodeRepository()
        val suggestions = repo.getSuggestionsForPrefix(":roc")
        assertTrue("Should contain rocket emoji", suggestions.any { it.char == "🚀" })

        val mathSuggestions = repo.getSuggestionsForPrefix(":inf")
        assertTrue("Should contain infinity symbol", mathSuggestions.any { it.char == "∞" })

        val searchGreek = repo.searchLocal("nabla")
        assertTrue("Should find nabla symbol", searchGreek.any { it.char == "∇" })
    }

    @Test
    fun `launch MainActivity without crash`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())
    }

    @get:org.junit.Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `test RadialWheelMenu composable and clicking options`() {
        var clicked = false
        composeTestRule.setContent {
            com.example.ui.editor.RadialWheelMenu(
                touchPosition = androidx.compose.ui.geometry.Offset(300f, 600f),
                isOpen = true,
                onDismiss = {},
                onInsertText = { _, _ -> clicked = true },
                onOpenVault = {},
                onToggleFind = {},
                onOpenSyntaxCatalog = {},
                onOpenUnicodePicker = {},
                onOpenCtan = {},
                onOpenGraphs = {},
                onOpenVaultSheet = {},
                onCreateNewDocument = {},
                onOpenRename = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Headings").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test FileTreeGraphScreen rendering and interactions`() {
        val sampleDocs = listOf(
            com.example.model.MarkdownDocument(id = "doc1", title = "Welcome Note", content = "Hello [[Project Specs]]"),
            com.example.model.MarkdownDocument(id = "doc2", title = "Project Specs", content = "Detailed topology specification")
        )

        var selectedDocId: String? = null
        var closed = false

        composeTestRule.setContent {
            com.example.ui.graphview.FileTreeGraphScreen(
                documents = sampleDocs,
                activeDocumentId = "doc1",
                onSelectDocument = { selectedDocId = it },
                onClose = { closed = true }
            )
        }

        composeTestRule.waitForIdle()
        // Switch to Folder Tree View
        composeTestRule.onNodeWithContentDescription("Folder Tree").performClick()
        composeTestRule.waitForIdle()

        // Switch back to Network Graph View
        composeTestRule.onNodeWithContentDescription("Network Graph").performClick()
        composeTestRule.waitForIdle()
    }
}
