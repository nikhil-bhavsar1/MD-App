package com.example.ui.preview

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CtanCatalog
import com.example.model.EditorFont
import com.example.util.MarkdownRendererHtml
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreviewWebView(
    markdown: String,
    installedPackageIds: List<String>,
    previewFontSizeSp: Int,
    fontChoice: EditorFont,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    // Full HTML template for initial load or font/layout changes
    val htmlContent = remember(previewFontSizeSp, fontChoice) {
        MarkdownRendererHtml.generateHtml(
            markdown = markdown,
            installedPackageIds = installedPackageIds,
            previewFontSizePx = previewFontSizeSp,
            fontChoice = fontChoice
        )
    }

    // When font or structure changes, reload template
    LaunchedEffect(htmlContent) {
        webViewRef?.let { wv ->
            isLoaded = false
            wv.loadDataWithBaseURL("https://hypermark.local/", htmlContent, "text/html", "UTF-8", null)
        }
    }

    // Hot live update DOM without reloading the page or losing scroll position
    LaunchedEffect(markdown, installedPackageIds, isLoaded) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isLoaded) {
            val quotedMarkdown = JSONObject.quote(markdown)
            val combinedMacros = mutableMapOf<String, String>()
            for (pkgId in installedPackageIds) {
                CtanCatalog.findPackage(pkgId)?.macros?.let { combinedMacros.putAll(it) }
            }
            val macrosJson = JSONObject(combinedMacros as Map<*, *>).toString()
            val js = "if (typeof window.updateContent === 'function') { window.updateContent($quotedMarkdown, $macrosJson); }"
            wv.evaluateJavascript(js, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D14))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.parseColor("#090D14"))
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        textZoom = 100
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoaded = true
                        }
                    }
                    loadDataWithBaseURL("https://hypermark.local/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = {
                // Handled reactively via LaunchedEffects
            }
        )
    }
}
