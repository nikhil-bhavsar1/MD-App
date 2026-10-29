package com.example.util

import com.example.data.CtanCatalog
import com.example.model.EditorFont
import org.json.JSONObject

object MarkdownRendererHtml {

    fun generateHtml(
        markdown: String,
        installedPackageIds: List<String>,
        previewFontSizePx: Int,
        fontChoice: EditorFont
    ): String {
        // Collect CTAN macros from installed packages
        val combinedMacros = mutableMapOf<String, String>()
        for (pkgId in installedPackageIds) {
            val pkg = CtanCatalog.findPackage(pkgId)
            if (pkg != null) {
                combinedMacros.putAll(pkg.macros)
            }
        }
        val macrosJson = JSONObject(combinedMacros as Map<*, *>).toString()

        val fontFamilyCss = when (fontChoice) {
            EditorFont.MONOSPACE -> "'JetBrains Mono', 'Fira Code', 'Courier New', monospace"
            EditorFont.SANS_SERIF -> "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
            EditorFont.SERIF -> "'Georgia', 'Cambria', 'Times New Roman', serif"
        }

        // Escape for safe embedding in text/plain script tag without JS template literal issues
        val escapedMarkdown = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
    <title>HyperMark Preview</title>
    
    <!-- KaTeX CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css">
    
    <!-- Prism Dark CSS -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css">
    
    <style>
        :root {
            --base-font-size: ${previewFontSizePx}px;
            --base-font-family: $fontFamilyCss;
            --bg-color: #090D14;
            --surface-color: #111724;
            --surface-elevated: #161F30;
            --border-color: #1F2A3F;
            --border-glow: rgba(0, 229, 255, 0.25);
            --text-primary: #F1F5F9;
            --text-secondary: #94A3B8;
            --accent-cyan: #00E5FF;
            --accent-purple: #C084FC;
            --accent-pink: #F472B6;
            --accent-emerald: #34D399;
            --accent-amber: #FBBF24;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-primary);
            font-family: var(--base-font-family);
            font-size: var(--base-font-size);
            line-height: 1.65;
            padding: 16px 14px 56px 14px;
            overflow-x: hidden;
            word-wrap: break-word;
        }

        html {
            scrollbar-color: rgba(0, 229, 255, 0.65) transparent;
            scrollbar-width: thin;
        }

        ::-webkit-scrollbar {
            width: 6px;
            height: 6px;
        }

        ::-webkit-scrollbar-track {
            background: transparent;
        }

        ::-webkit-scrollbar-thumb {
            background-color: rgba(0, 229, 255, 0.65);
            border: 1px solid var(--bg-color);
            border-radius: 999px;
        }

        ::-webkit-scrollbar-thumb:hover {
            background-color: var(--accent-cyan);
        }

        h1, h2, h3, h4, h5, h6 {
            color: #FFFFFF;
            margin-top: 1.4em;
            margin-bottom: 0.6em;
            font-weight: 700;
            line-height: 1.25;
            letter-spacing: -0.02em;
        }

        h1 {
            font-size: 1.9em;
            border-bottom: 2px solid var(--border-glow);
            padding-bottom: 0.3em;
            background: linear-gradient(90deg, #FFFFFF, var(--accent-cyan));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        h2 {
            font-size: 1.5em;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 0.2em;
            color: #38BDF8;
        }

        h3 {
            font-size: 1.25em;
            color: var(--accent-purple);
        }

        p {
            margin-bottom: 1em;
        }

        a {
            color: var(--accent-cyan);
            text-decoration: none;
            border-bottom: 1px dashed var(--accent-cyan);
            transition: all 0.2s ease;
        }

        a:hover {
            color: #7DD3FC;
            border-bottom-style: solid;
        }

        hr {
            border: 0;
            height: 1px;
            background: linear-gradient(90deg, transparent, var(--border-color), var(--border-glow), transparent);
            margin: 2em 0;
        }

        blockquote {
            border-left: 3px solid var(--accent-purple);
            background: rgba(192, 132, 252, 0.05);
            padding: 10px 16px;
            margin: 1em 0;
            border-radius: 0 8px 8px 0;
            color: #CBD5E1;
        }

        /* Highlight Mark ==text== */
        mark, .md-mark {
            background: rgba(254, 240, 138, 0.25);
            color: #FEF08A;
            border-radius: 4px;
            padding: 2px 6px;
            border: 1px solid rgba(254, 240, 138, 0.4);
            box-shadow: 0 0 10px rgba(254, 240, 138, 0.2);
            font-weight: 600;
        }

        /* WikiLinks [[File]] */
        .wikilink {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            background: rgba(192, 132, 252, 0.12);
            color: var(--accent-purple);
            border: 1px solid rgba(192, 132, 252, 0.35);
            padding: 2px 8px;
            border-radius: 6px;
            font-size: 0.9em;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        .wikilink:hover {
            background: rgba(192, 132, 252, 0.25);
            border-color: var(--accent-purple);
        }

        /* Obsidian Admonitions / Callouts */
        .callout {
            border-radius: 8px;
            padding: 12px 16px;
            margin: 1.2em 0;
            border-left: 4px solid;
            background: var(--surface-color);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }
        .callout-title {
            font-weight: 700;
            font-size: 0.95em;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 6px;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .callout-note { border-left-color: #38BDF8; background: rgba(56, 189, 248, 0.07); }
        .callout-note .callout-title { color: #38BDF8; }
        .callout-tip { border-left-color: #34D399; background: rgba(52, 211, 153, 0.07); }
        .callout-tip .callout-title { color: #34D399; }
        .callout-warning { border-left-color: #FBBF24; background: rgba(251, 191, 36, 0.07); }
        .callout-warning .callout-title { color: #FBBF24; }
        .callout-danger { border-left-color: #F87171; background: rgba(248, 113, 113, 0.07); }
        .callout-danger .callout-title { color: #F87171; }
        .callout-info { border-left-color: #60A5FA; background: rgba(96, 165, 250, 0.07); }
        .callout-info .callout-title { color: #60A5FA; }
        .callout-question { border-left-color: #A78BFA; background: rgba(167, 139, 250, 0.07); }
        .callout-question .callout-title { color: #A78BFA; }
        .callout-success { border-left-color: #10B981; background: rgba(16, 185, 129, 0.07); }
        .callout-success .callout-title { color: #10B981; }
        .callout-example { border-left-color: #C084FC; background: rgba(192, 132, 252, 0.07); }
        .callout-example .callout-title { color: #C084FC; }

        /* Code Blocks */
        .code-container {
            position: relative;
            background: #0B0F17;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            margin: 1.4em 0;
            overflow: hidden;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
        }

        .code-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #111827;
            padding: 6px 12px;
            border-bottom: 1px solid var(--border-color);
            font-size: 0.8em;
            font-family: monospace;
            color: #94A3B8;
        }

        .code-lang {
            text-transform: uppercase;
            font-weight: 700;
            color: var(--accent-cyan);
            letter-spacing: 0.05em;
        }

        .copy-btn {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.15);
            color: #E2E8F0;
            border-radius: 4px;
            padding: 2px 8px;
            font-size: 0.75em;
            cursor: pointer;
            transition: all 0.2s;
        }

        .copy-btn:hover {
            background: var(--accent-cyan);
            color: #000;
        }

        pre {
            padding: 12px 14px;
            overflow-x: auto;
            margin: 0;
            font-size: 0.9em;
            line-height: 1.5;
        }

        code:not(pre code) {
            background: #1E293B;
            color: var(--accent-pink);
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.88em;
            font-family: monospace;
            border: 1px solid rgba(244, 114, 182, 0.2);
        }

        /* Tables & Responsive Wrapper */
        .table-wrapper {
            width: 100%;
            margin: 1.4em 0;
            overflow-x: auto;
            border-radius: 8px;
            border: 1px solid rgba(56, 189, 248, 0.35);
            background: var(--surface-color);
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
            scrollbar-width: thin;
            scrollbar-color: rgba(0, 229, 255, 0.4) transparent;
        }

        .table-wrapper::-webkit-scrollbar {
            height: 6px;
        }

        .table-wrapper::-webkit-scrollbar-track {
            background: rgba(8, 13, 24, 0.7);
            border-radius: 4px;
        }

        .table-wrapper::-webkit-scrollbar-thumb {
            background: rgba(56, 189, 248, 0.4);
            border-radius: 4px;
        }

        .table-wrapper::-webkit-scrollbar-thumb:hover {
            background: var(--accent-cyan);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            border-spacing: 0;
            margin: 1.4em 0;
            font-size: 0.95em;
            background: var(--surface-color);
            border: 1px solid rgba(56, 189, 248, 0.35);
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        }

        .table-wrapper table {
            margin: 0;
            border: none;
            border-radius: 0;
            box-shadow: none;
        }

        thead {
            background: #162032;
        }

        th {
            padding: 12px 16px;
            font-weight: 700;
            font-size: 0.88em;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            color: var(--accent-cyan);
            border: 1px solid rgba(56, 189, 248, 0.3);
            border-bottom: 2px solid var(--accent-cyan);
            white-space: nowrap;
            text-align: left;
        }

        td {
            padding: 10px 16px;
            color: var(--text-primary);
            border: 1px solid rgba(56, 189, 248, 0.2);
            font-size: 0.92em;
            line-height: 1.5;
            text-align: left;
        }

        /* GFM alignment attributes */
        th[align="left"], td[align="left"] {
            text-align: left;
        }

        th[align="center"], td[align="center"] {
            text-align: center;
        }

        th[align="right"], td[align="right"] {
            text-align: right;
        }

        tbody tr:nth-child(even) td {
            background: rgba(255, 255, 255, 0.02);
        }

        tbody tr:hover td {
            background: rgba(0, 229, 255, 0.06);
            transition: background 0.15s ease;
        }

        /* Charts Container */
        .chart-wrapper {
            background: #0B101A;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            margin: 1.5em 0;
            padding: 14px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
        }

        .chart-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;
            padding-bottom: 6px;
            border-bottom: 1px solid #1E293B;
            font-size: 0.85em;
        }

        .chart-canvas-box {
            position: relative;
            width: 100%;
            height: 260px;
        }

        /* Cytoscape Container */
        .cytoscape-wrapper {
            background: #080C14;
            border: 1px solid #23314A;
            border-radius: 10px;
            margin: 1.5em 0;
            overflow: hidden;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
        }

        .cytoscape-toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 6px 12px;
            background: #0E1524;
            border-bottom: 1px solid #1E293B;
            font-size: 0.8em;
            color: #94A3B8;
        }

        .cytoscape-canvas {
            width: 100%;
            height: 320px;
            display: block;
            background: radial-gradient(circle at center, #0F172A 0%, #080C14 100%);
        }

        /* Mermaid Container */
        .mermaid-wrapper {
            background: #0D131F;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 16px;
            margin: 1.5em 0;
            text-align: center;
            overflow-x: auto;
        }

        .mermaid svg {
            max-width: 100%;
            height: auto;
        }

        /* KaTeX Math overrides */
        .katex-display {
            overflow-x: auto;
            overflow-y: hidden;
            padding: 8px 0;
            margin: 1em 0;
            color: var(--accent-emerald);
        }

        .katex {
            font-size: 1.08em;
        }

        /* Toast notification for copy */
        #toast {
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: var(--accent-cyan);
            color: #000;
            padding: 6px 14px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 0.8em;
            opacity: 0;
            transition: opacity 0.3s;
            pointer-events: none;
            z-index: 9999;
        }
    </style>
</head>
<body>
    <div id="content"></div>
    <div id="toast">Copied to clipboard</div>
    <script type="text/plain" id="initial-markdown">$escapedMarkdown</script>

    <!-- Core Libraries -->
    <script src="https://cdn.jsdelivr.net/npm/marked@12.0.1/marked.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.0/dist/mermaid.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.28.1/cytoscape.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/dagre/0.8.5/dagre.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/cytoscape-dagre@2.5.0/cytoscape-dagre.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.2/dist/chart.umd.min.js"></script>

    <script>
        const ctanMacros = $macrosJson;

        function showToast(text) {
            const toast = document.getElementById('toast');
            toast.innerText = text;
            toast.style.opacity = '1';
            setTimeout(() => { toast.style.opacity = '0'; }, 1800);
        }

        function copyCode(btn, codeId) {
            const codeEl = document.getElementById(codeId);
            if (!codeEl) return;
            const text = codeEl.innerText;
            if (navigator.clipboard) {
                navigator.clipboard.writeText(text).then(() => showToast('Code copied!'));
            } else {
                showToast('Copied!');
            }
        }

        // Initialize Mermaid
        if (window.mermaid) {
            mermaid.initialize({
                startOnLoad: false,
                theme: 'dark',
                themeVariables: {
                    darkMode: true,
                    background: '#0D131F',
                    primaryColor: '#1E293B',
                    primaryTextColor: '#F1F5F9',
                    primaryBorderColor: '#38BDF8',
                    lineColor: '#C084FC',
                    secondaryColor: '#312E81',
                    tertiaryColor: '#0F172A'
                }
            });
        }

        // Register Dagre layout for Cytoscape
        if (window.cytoscape && window.cytoscapeDagre) {
            try {
                cytoscape.use(cytoscapeDagre);
            } catch (e) {
                console.error("Dagre init error", e);
            }
        }

        function renderMarkdown(raw) {
            let cytoBlocks = [];
            let mermaidBlocks = [];
            let chartBlocks = [];

            // Extract Chart.js blocks: ```chart [bar|line|pie|doughnut|radar]
            let processed = raw.replace(/```(?:chart|chartjs)\s*([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, chartType, body) => {
                const id = 'chart_' + Math.random().toString(36).substring(2, 9);
                chartBlocks.push({ id, type: chartType || 'bar', body });
                return '<div class="chart-wrapper"><div class="chart-header"><span style="color:#00E5FF;font-weight:bold;">📊 ' + (chartType || 'Bar').toUpperCase() + ' CHART</span><span style="color:#94A3B8;font-size:0.8em;">Chart.js Canvas</span></div><div class="chart-canvas-box"><canvas id="' + id + '"></canvas></div></div>';
            });

            // Extract Cytoscape blocks
            processed = processed.replace(/```cytoscape\s*\n([\s\S]*?)```/g, (match, body) => {
                const id = 'cyto_' + Math.random().toString(36).substring(2, 9);
                cytoBlocks.push({ id, body });
                return '<div class="cytoscape-wrapper"><div class="cytoscape-toolbar"><span>🕸️ Cytoscape + Dagre Layout</span><span style="color:#38BDF8;">Interactive Graph</span></div><div id="' + id + '" class="cytoscape-canvas"></div></div>';
            });

            // Extract Mermaid blocks
            processed = processed.replace(/```mermaid\s*\n([\s\S]*?)```/g, (match, body) => {
                const id = 'mermaid_' + Math.random().toString(36).substring(2, 9);
                mermaidBlocks.push({ id, body });
                return '<div class="mermaid-wrapper"><div class="mermaid" id="' + id + '">' + body + '</div></div>';
            });

            // Parse Obsidian callouts
            processed = processed.replace(/^>\s*\[!(NOTE|TIP|WARNING|CAUTION|DANGER|INFO|QUESTION|SUCCESS|EXAMPLE|BUG|QUOTE)\]\s*\n((?:>.*\n?)*)/gim, (match, type, content) => {
                const cleanContent = content.replace(/^>\s?/gm, '');
                const t = type.toLowerCase();
                const icon = t === 'tip' ? '💡' : t === 'warning' ? '⚠️' : (t === 'danger' || t === 'caution') ? '🚨' : t === 'question' ? '❓' : t === 'success' ? '✅' : t === 'example' ? '🔮' : 'ℹ️';
                return '<div class="callout callout-' + t + '"><div class="callout-title"><span>' + icon + '</span> ' + type + '</div><div class="callout-body">' + cleanContent + '</div></div>';
            });

            // Parse Highlight mark ==text==
            processed = processed.replace(/==([^=\n]+)==/g, '<mark class="md-mark">$1</mark>');

            // Parse WikiLinks [[Document]]
            processed = processed.replace(/\[\[([^\]]+)\]\]/g, '<span class="wikilink">📄 $1</span>');

            // Marked HTML parsing
            if (window.marked) {
                try {
                    marked.setOptions({
                        gfm: true,
                        breaks: true,
                        headerIds: true
                    });
                    processed = marked.parse(processed);
                    processed = processed.replace(/<table(\b[^>]*)>([\s\S]*?)<\/table>/gi, '<div class="table-wrapper"><table$1>$2</table></div>');
                } catch(e) {
                    console.warn("Marked parse error", e);
                }
            } else {
                // Fallback instant parser when offline or CDN loading
                processed = processed.replace(/^(\|.+?\|[\r\n]+\|[-:\s|]+?\|[\r\n]+(?:\|.+?\|[\r\n]*)+)/gm, function(match) {
                    const lines = match.trim().split('\n').filter(function(l) { return l.trim().length > 0; });
                    if (lines.length < 2) return match;
                    const headers = lines[0].split('|').slice(1, -1).map(function(h) { return h.trim(); });
                    const alignments = lines[1].split('|').slice(1, -1).map(function(a) {
                        const s = a.trim();
                        if (s.startsWith(':') && s.endsWith(':')) return 'center';
                        if (s.endsWith(':')) return 'right';
                        return 'left';
                    });
                    let out = '<div class="table-wrapper"><table><thead><tr>';
                    headers.forEach(function(h, i) {
                        const align = alignments[i] || 'left';
                        out += '<th align="' + align + '">' + h + '</th>';
                    });
                    out += '</tr></thead><tbody>';
                    for (let r = 2; r < lines.length; r++) {
                        const cells = lines[r].split('|').slice(1, -1).map(function(c) { return c.trim(); });
                        out += '<tr>';
                        headers.forEach(function(_, i) {
                            const align = alignments[i] || 'left';
                            out += '<td align="' + align + '">' + (cells[i] || '') + '</td>';
                        });
                        out += '</tr>';
                    }
                    out += '</tbody></table></div>';
                    return out;
                });
                processed = processed
                    .replace(/^### (.*$)/gim, '<h3 style="color:#C084FC;margin:12px 0;">$1</h3>')
                    .replace(/^## (.*$)/gim, '<h2 style="color:#38BDF8;border-bottom:1px solid #1E293B;padding-bottom:4px;margin:16px 0;">$1</h2>')
                    .replace(/^# (.*$)/gim, '<h1 style="color:#00E5FF;border-bottom:2px solid rgba(0,229,255,0.3);padding-bottom:6px;margin:20px 0;">$1</h1>')
                    .replace(/\*\*(.*?)\*\*/gim, '<strong style="color:#FFF;">$1</strong>')
                    .replace(/\*(.*?)\*/gim, '<em style="color:#CBD5E1;">$1</em>')
                    .replace(/`([^`]+)`/gim, '<code style="background:#111827;color:#38BDF8;padding:2px 6px;border-radius:4px;font-family:monospace;">$1</code>')
                    .replace(/\n\n/gim, '<p style="margin-bottom:12px;"></p>')
                    .replace(/\n/gim, '<br>');
            }

            document.getElementById('content').innerHTML = processed;

            // Render KaTeX with CTAN macros
            if (window.renderMathInElement) {
                renderMathInElement(document.getElementById('content'), {
                    delimiters: [
                        { left: '$$', right: '$$', display: true },
                        { left: '$', right: '$', display: false },
                        { left: '\\(', right: '\\)', display: false },
                        { left: '\\[', right: '\\]', display: true }
                    ],
                    throwOnError: false,
                    macros: ctanMacros
                });
            }

            // Render Mermaid diagrams
            if (window.mermaid && mermaidBlocks.length > 0) {
                mermaid.run({
                    nodes: document.querySelectorAll('.mermaid')
                }).catch(e => console.warn('Mermaid render error:', e));
            }

            // Render Chart.js
            chartBlocks.forEach(item => {
                const canvas = document.getElementById(item.id);
                if (!canvas || !window.Chart) return;
                try {
                    const chartConfig = parseChartDefinition(item.type, item.body);
                    new Chart(canvas.getContext('2d'), chartConfig);
                } catch (e) {
                    canvas.parentElement.innerHTML = '<div style="color:#F87171;padding:16px;">Chart error: ' + e.message + '</div>';
                }
            });

            // Render Cytoscape diagrams with Dagre
            cytoBlocks.forEach(item => {
                const el = document.getElementById(item.id);
                if (!el) return;

                const parsed = parseCytoscapeDefinition(item.body);
                try {
                    cytoscape({
                        container: el,
                        boxSelectionEnabled: false,
                        autounselectify: false,
                        style: [
                            {
                                selector: 'node',
                                style: {
                                    'background-color': 'data(color)',
                                    'label': 'data(label)',
                                    'color': '#FFFFFF',
                                    'font-size': '12px',
                                    'font-weight': 'bold',
                                    'text-valign': 'center',
                                    'text-halign': 'center',
                                    'width': '65px',
                                    'height': '65px',
                                    'border-width': 2,
                                    'border-color': '#00E5FF',
                                    'border-opacity': 0.8,
                                    'text-wrap': 'wrap',
                                    'text-max-width': '75px'
                                }
                            },
                            {
                                selector: 'edge',
                                style: {
                                    'width': 2.5,
                                    'line-color': '#818CF8',
                                    'target-arrow-color': '#818CF8',
                                    'target-arrow-shape': 'triangle',
                                    'curve-style': 'bezier',
                                    'label': 'data(label)',
                                    'font-size': '10px',
                                    'color': '#94A3B8',
                                    'text-rotation': 'autorotate',
                                    'text-background-opacity': 0.8,
                                    'text-background-color': '#0B0F17',
                                    'text-background-padding': '3px'
                                }
                            }
                        ],
                        elements: parsed,
                        layout: {
                            name: 'dagre',
                            rankDir: 'TB',
                            nodeSep: 50,
                            edgeSep: 20,
                            rankSep: 60,
                            padding: 24
                        }
                    });
                } catch (err) {
                    el.innerHTML = '<div style="padding:20px; color:#F87171;">Graph render error: ' + err.message + '</div>';
                }
            });

            // Wrap code blocks with header and copy button
            document.querySelectorAll('pre code').forEach((block, idx) => {
                const pre = block.parentElement;
                if (pre.parentElement.classList.contains('code-container') || pre.classList.contains('mermaid')) return;

                const codeId = 'code_block_' + idx;
                block.id = codeId;

                const langMatch = block.className.match(/language-([a-zA-Z0-9_-]+)/);
                const lang = langMatch ? langMatch[1] : 'CODE';

                const container = document.createElement('div');
                container.className = 'code-container';

                const header = document.createElement('div');
                header.className = 'code-header';
                header.innerHTML = '<span class="code-lang">' + lang + '</span><button class="copy-btn" onclick="copyCode(this, \'' + codeId + '\')">Copy</button>';

                pre.parentNode.insertBefore(container, pre);
                container.appendChild(header);
                container.appendChild(pre);

                if (window.Prism) {
                    Prism.highlightElement(block);
                }
            });
        }

        // Chart.js YAML/JSON parser
        function parseChartDefinition(type, raw) {
            raw = raw.trim();
            if (raw.startsWith('{')) {
                return JSON.parse(raw);
            }

            let chartType = (type || 'bar').toLowerCase();
            let labels = [];
            let datasets = [];

            const lines = raw.split('\n');
            lines.forEach(line => {
                const t = line.trim();
                if (t.startsWith('labels:')) {
                    const content = t.replace('labels:', '').trim();
                    try {
                        labels = JSON.parse(content);
                    } catch (e) {
                        labels = content.split(',').map(s => s.trim().replace(/^["']|["']$/g, ''));
                    }
                } else if (t.startsWith('data:') || t.startsWith('dataset:')) {
                    const content = t.replace(/^(?:data|dataset):/, '').trim();
                    // format: "Revenue", [10, 20, 30], #00E5FF
                    const parts = content.split(/,\s*(?=\[)/);
                    let label = "Values";
                    let dataArr = [];
                    let color = "#00E5FF";

                    if (parts.length > 1) {
                        label = parts[0].replace(/^["']|["']$/g, '').trim();
                        const rest = parts[1];
                        const closingBracket = rest.indexOf(']');
                        if (closingBracket > 0) {
                            dataArr = JSON.parse(rest.substring(0, closingBracket + 1));
                            const extra = rest.substring(closingBracket + 1).trim();
                            if (extra.includes('#')) {
                                const hexMatch = extra.match(/#[a-fA-F0-9]{3,8}/);
                                if (hexMatch) color = hexMatch[0];
                            }
                        }
                    } else {
                        try {
                            dataArr = JSON.parse(content);
                        } catch (e) {
                            dataArr = content.split(',').map(Number);
                        }
                    }

                    const palette = ['#00E5FF', '#C084FC', '#34D399', '#FBBF24', '#F472B6', '#38BDF8'];
                    const bgColors = chartType === 'pie' || chartType === 'doughnut' || chartType === 'polararea'
                        ? palette.slice(0, dataArr.length)
                        : color + '55';
                    const borderColors = chartType === 'pie' || chartType === 'doughnut' || chartType === 'polararea'
                        ? palette.slice(0, dataArr.length)
                        : color;

                    datasets.push({
                        label: label,
                        data: dataArr,
                        backgroundColor: bgColors,
                        borderColor: borderColors,
                        borderWidth: 2,
                        tension: 0.35,
                        fill: chartType === 'line'
                    });
                }
            });

            if (datasets.length === 0) {
                labels = ["Alpha", "Beta", "Gamma", "Delta"];
                datasets = [{
                    label: "Metric",
                    data: [35, 62, 48, 88],
                    backgroundColor: "#00E5FF55",
                    borderColor: "#00E5FF",
                    borderWidth: 2
                }];
            }

            return {
                type: chartType,
                data: {
                    labels: labels,
                    datasets: datasets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            labels: { color: '#F1F5F9', font: { family: 'monospace' } }
                        }
                    },
                    scales: (chartType === 'pie' || chartType === 'doughnut' || chartType === 'radar') ? {} : {
                        x: {
                            ticks: { color: '#94A3B8', font: { family: 'monospace' } },
                            grid: { color: 'rgba(255, 255, 255, 0.08)' }
                        },
                        y: {
                            ticks: { color: '#94A3B8', font: { family: 'monospace' } },
                            grid: { color: 'rgba(255, 255, 255, 0.08)' }
                        }
                    }
                }
            };
        }

        // Parse custom cytoscape syntax or JSON
        function parseCytoscapeDefinition(raw) {
            raw = raw.trim();
            if (raw.startsWith('{') || raw.startsWith('[')) {
                try {
                    return JSON.parse(raw);
                } catch (e) {}
            }

            const elements = { nodes: [], edges: [] };
            const lines = raw.split('\n');
            let section = '';

            lines.forEach(line => {
                const trimmed = line.trim();
                if (!trimmed || trimmed.startsWith('#')) return;

                if (trimmed.startsWith('nodes:')) {
                    section = 'nodes';
                    return;
                }
                if (trimmed.startsWith('edges:')) {
                    section = 'edges';
                    return;
                }

                if (trimmed.startsWith('-')) {
                    const item = trimmed.replace(/^-\s*/, '');
                    const pairs = item.split(',').map(s => s.trim());
                    const obj = {};
                    pairs.forEach(p => {
                        const colonIdx = p.indexOf(':');
                        if (colonIdx > 0) {
                            const k = p.substring(0, colonIdx).trim();
                            let v = p.substring(colonIdx + 1).trim();
                            if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length - 1);
                            }
                            obj[k] = v;
                        }
                    });

                    if (section === 'nodes' && obj.id) {
                        elements.nodes.push({
                            data: {
                                id: obj.id,
                                label: obj.label || obj.id,
                                color: obj.color || '#38BDF8'
                            }
                        });
                    } else if (section === 'edges' && obj.source && obj.target) {
                        elements.edges.push({
                            data: {
                                id: obj.id || (obj.source + '_' + obj.target),
                                source: obj.source,
                                target: obj.target,
                                label: obj.label || ''
                            }
                        });
                    }
                }
            });

            return elements;
        }

        // Initial render without JS template literal errors
        function decodeHtml(html) {
            const txt = document.createElement("textarea");
            txt.innerHTML = html;
            return txt.value;
        }

        const rawMarkdownEl = document.getElementById('initial-markdown');
        const rawMarkdown = rawMarkdownEl ? decodeHtml(rawMarkdownEl.innerHTML) : "";
        if (document.readyState === 'loading') {
            window.addEventListener('DOMContentLoaded', () => {
                renderMarkdown(rawMarkdown);
            });
        } else {
            renderMarkdown(rawMarkdown);
        }

        window.updateContent = function(newMarkdown, newMacros) {
            if (newMacros) {
                Object.assign(ctanMacros, newMacros);
            }
            renderMarkdown(newMarkdown);
        };
    </script>
</body>
</html>
""".trimIndent()
    }
}
