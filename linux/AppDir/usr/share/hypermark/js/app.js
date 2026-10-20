// HyperMark Linux Desktop Core Application Engine

class HyperMarkApp {
    constructor() {
        this.documents = this.loadDocuments();
        this.activeDocId = this.documents[0]?.id || "doc_math_ctan";
        this.viewMode = "split"; // 'editor', 'split', 'preview'
        this.findMatches = [];
        this.currentMatchIdx = -1;

        // Graph State
        this.graphMode = "network"; // 'network' or 'tree'
        this.graphScale = 1.0;
        this.graphOffset = { x: 0, y: 0 };
        this.selectedNodeId = this.activeDocId;
        this.draggedNode = null;
        this.graphSearch = "";
        this.showFolders = true;
        this.showTags = true;
        this.labelTextScale = 1.0; // 1.0, 1.3, 1.6
        this.pulseTime = 0;

        this.initDOMElements();
        this.initEventListeners();
        this.renderSidebar();
        this.loadActiveDocument();
        this.initMermaid();
        this.startGraphAnimationLoop();
    }

    loadDocuments() {
        try {
            const saved = localStorage.getItem("hypermark_docs");
            if (saved) {
                const parsed = JSON.parse(saved);
                if (Array.isArray(parsed) && parsed.length > 0) return parsed;
            }
        } catch (e) {
            console.warn("Using default documents:", e);
        }
        return JSON.parse(JSON.stringify(defaultDocuments));
    }

    saveDocuments() {
        try {
            localStorage.setItem("hypermark_docs", JSON.stringify(this.documents));
        } catch (e) {
            console.error("Failed to save to local storage:", e);
        }
    }

    getActiveDocument() {
        return this.documents.find(d => d.id === this.activeDocId) || this.documents[0];
    }

    initDOMElements() {
        this.titleInput = document.getElementById("doc-title-input");
        this.editor = document.getElementById("editor-textarea");
        this.lineNumbers = document.getElementById("line-numbers");
        this.preview = document.getElementById("preview-container");
        this.sidebar = document.getElementById("sidebar");
        this.sidebarDocList = document.getElementById("sidebar-doc-list");
        this.filePicker = document.getElementById("file-picker");

        // Telemetry
        this.statLines = document.getElementById("stat-lines");
        this.statWords = document.getElementById("stat-words");
        this.statChars = document.getElementById("stat-chars");
        this.statRead = document.getElementById("stat-read");
        this.statLatex = document.getElementById("stat-latex");
        this.statDiagrams = document.getElementById("stat-diagrams");

        // Panes
        this.editorPane = document.getElementById("editor-pane");
        this.previewPane = document.getElementById("preview-pane");
        this.splitDivider = document.getElementById("split-divider");

        // Find/Replace
        this.findBar = document.getElementById("find-replace-bar");
        this.findQuery = document.getElementById("find-query");
        this.replaceQuery = document.getElementById("replace-query");
        this.findMatchCount = document.getElementById("find-match-count");

        // Graph Modal
        this.graphModal = document.getElementById("graph-modal");
        this.graphCanvas = document.getElementById("graph-canvas");
        this.ctx = this.graphCanvas.getContext("2d");
        this.graphInspector = document.getElementById("graph-inspector");
        this.folderTreeContainer = document.getElementById("folder-tree-container");
        this.graphCanvasContainer = document.getElementById("graph-canvas-container");
    }

    initEventListeners() {
        // Text changes
        this.editor.addEventListener("input", () => {
            const doc = this.getActiveDocument();
            if (doc) {
                doc.content = this.editor.value;
                doc.updatedAt = Date.now();
                this.saveDocuments();
                this.updateLineNumbers();
                this.renderPreview();
                this.updateTelemetry();
            }
        });

        // Sync scroll between textarea and line numbers
        this.editor.addEventListener("scroll", () => {
            this.lineNumbers.scrollTop = this.editor.scrollTop;
        });

        // Title rename
        this.titleInput.addEventListener("change", () => {
            const doc = this.getActiveDocument();
            if (doc) {
                doc.title = this.titleInput.value.trim() || "Untitled";
                this.saveDocuments();
                this.renderSidebar();
            }
        });

        // View Mode buttons
        document.getElementById("btn-mode-editor").addEventListener("click", () => this.setViewMode("editor"));
        document.getElementById("btn-mode-split").addEventListener("click", () => this.setViewMode("split"));
        document.getElementById("btn-mode-preview").addEventListener("click", () => this.setViewMode("preview"));

        // Sidebar toggle
        document.getElementById("btn-toggle-sidebar").addEventListener("click", () => {
            this.sidebar.classList.toggle("collapsed");
        });
        document.getElementById("btn-close-sidebar").addEventListener("click", () => {
            this.sidebar.classList.add("collapsed");
        });

        // New Document
        document.getElementById("btn-new-doc").addEventListener("click", () => {
            const newDoc = {
                id: "doc_" + Date.now(),
                title: "Untitled Note " + (this.documents.length + 1),
                isPinned: false,
                updatedAt: Date.now(),
                content: "# Untitled Note\\n\\nStart writing markdown here...\\n"
            };
            this.documents.unshift(newDoc);
            this.activeDocId = newDoc.id;
            this.saveDocuments();
            this.renderSidebar();
            this.loadActiveDocument();
        });

        // Open / Import .md file
        document.getElementById("btn-import-file").addEventListener("click", () => {
            this.filePicker.click();
        });
        this.filePicker.addEventListener("change", (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = (event) => {
                const title = file.name.replace(/\\.(md|markdown|txt)$/i, "");
                const newDoc = {
                    id: "doc_" + Date.now(),
                    title: title || "Imported Note",
                    isPinned: false,
                    updatedAt: Date.now(),
                    content: event.target.result
                };
                this.documents.unshift(newDoc);
                this.activeDocId = newDoc.id;
                this.saveDocuments();
                this.renderSidebar();
                this.loadActiveDocument();
            };
            reader.readAsText(file);
        });

        // Export active doc as .md file
        document.getElementById("btn-export-md").addEventListener("click", () => {
            const doc = this.getActiveDocument();
            if (!doc) return;
            const blob = new Blob([doc.content], { type: "text/markdown;charset=utf-8" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = (doc.title.trim().replace(/[/\\\\?%*:|"<>]/g, "_") || "Untitled") + ".md";
            a.click();
            URL.revokeObjectURL(url);
        });

        // Formatting toolbar
        document.querySelectorAll(".tool-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                const prefix = btn.dataset.prefix || "";
                const suffix = btn.dataset.suffix || "";
                this.insertFormatting(prefix, suffix);
            });
        });

        // Find & Replace
        document.getElementById("btn-toggle-find").addEventListener("click", () => {
            const isHidden = this.findBar.style.display === "none";
            this.findBar.style.display = isHidden ? "flex" : "none";
            if (isHidden) this.findQuery.focus();
        });
        document.getElementById("btn-close-find").addEventListener("click", () => {
            this.findBar.style.display = "none";
        });
        this.findQuery.addEventListener("input", () => this.executeFind());
        document.getElementById("btn-find-next").addEventListener("click", () => this.findNext());
        document.getElementById("btn-find-prev").addEventListener("click", () => this.findPrev());
        document.getElementById("btn-replace-current").addEventListener("click", () => this.replaceCurrent());
        document.getElementById("btn-replace-all").addEventListener("click", () => this.replaceAll());

        // Fullscreen
        document.getElementById("btn-fullscreen").addEventListener("click", () => {
            if (!document.fullscreenElement) {
                document.documentElement.requestFullscreen().catch(() => {});
            } else {
                document.exitFullscreen().catch(() => {});
            }
        });

        // Topology Graph Modal
        document.getElementById("btn-open-graph").addEventListener("click", () => {
            this.openGraphModal();
        });
        document.getElementById("btn-close-graph").addEventListener("click", () => {
            this.graphModal.classList.add("hidden");
        });

        // Graph View Mode (Network vs Tree)
        document.getElementById("btn-graph-mode-network").addEventListener("click", () => {
            this.setGraphMode("network");
        });
        document.getElementById("btn-graph-mode-tree").addEventListener("click", () => {
            this.setGraphMode("tree");
        });

        // Graph filters
        const btnFolderFilter = document.getElementById("btn-graph-filter-folders");
        btnFolderFilter.addEventListener("click", () => {
            this.showFolders = !this.showFolders;
            btnFolderFilter.classList.toggle("active", this.showFolders);
            this.buildGraphNodes();
        });
        const btnTagFilter = document.getElementById("btn-graph-filter-tags");
        btnTagFilter.addEventListener("click", () => {
            this.showTags = !this.showTags;
            btnTagFilter.classList.toggle("active", this.showTags);
            this.buildGraphNodes();
        });

        // Graph font size toggle
        const btnTextScale = document.getElementById("btn-graph-text-scale");
        btnTextScale.addEventListener("click", () => {
            this.labelTextScale = this.labelTextScale === 1.0 ? 1.3 : this.labelTextScale === 1.3 ? 1.6 : 1.0;
            btnTextScale.textContent = `Text: ${Math.round(this.labelTextScale * 100)}%`;
        });

        // Graph search
        document.getElementById("graph-search-input").addEventListener("input", (e) => {
            this.graphSearch = e.target.value.toLowerCase().trim();
        });

        // Graph floating zoom controls
        document.getElementById("btn-zoom-in").addEventListener("click", () => {
            this.graphScale = Math.min(3.8, this.graphScale * 1.25);
        });
        document.getElementById("btn-zoom-out").addEventListener("click", () => {
            this.graphScale = Math.max(0.35, this.graphScale / 1.25);
        });
        document.getElementById("btn-zoom-fit").addEventListener("click", () => {
            this.graphScale = 1.0;
            this.graphOffset = { x: 0, y: 0 };
        });
        document.getElementById("btn-reset-layout").addEventListener("click", () => {
            this.graphScale = 1.0;
            this.graphOffset = { x: 0, y: 0 };
            this.buildGraphNodes();
        });

        // Inspector Open in Editor button
        document.getElementById("btn-inspector-open").addEventListener("click", () => {
            if (this.selectedNodeId) {
                const doc = this.documents.find(d => d.id === this.selectedNodeId);
                if (doc) {
                    this.activeDocId = doc.id;
                    this.loadActiveDocument();
                    this.renderSidebar();
                    this.graphModal.classList.add("hidden");
                }
            }
        });

        // Keyboard Shortcuts
        window.addEventListener("keydown", (e) => {
            if (e.ctrlKey && e.key.toLowerCase() === "s") {
                e.preventDefault();
                document.getElementById("btn-export-md").click();
            } else if (e.ctrlKey && e.key.toLowerCase() === "g") {
                e.preventDefault();
                if (this.graphModal.classList.contains("hidden")) {
                    this.openGraphModal();
                } else {
                    this.graphModal.classList.add("hidden");
                }
            } else if (e.ctrlKey && e.key.toLowerCase() === "f") {
                e.preventDefault();
                document.getElementById("btn-toggle-find").click();
            } else if (e.ctrlKey && e.key.toLowerCase() === "n") {
                e.preventDefault();
                document.getElementById("btn-new-doc").click();
            }
        });

        // Canvas Mouse Events
        this.initCanvasInteractions();
    }

    setViewMode(mode) {
        this.viewMode = mode;
        document.getElementById("btn-mode-editor").classList.toggle("active", mode === "editor");
        document.getElementById("btn-mode-split").classList.toggle("active", mode === "split");
        document.getElementById("btn-mode-preview").classList.toggle("active", mode === "preview");

        if (mode === "editor") {
            this.editorPane.style.display = "flex";
            this.previewPane.style.display = "none";
            this.splitDivider.style.display = "none";
        } else if (mode === "preview") {
            this.editorPane.style.display = "none";
            this.previewPane.style.display = "flex";
            this.splitDivider.style.display = "none";
            this.renderPreview();
        } else {
            this.editorPane.style.display = "flex";
            this.previewPane.style.display = "flex";
            this.splitDivider.style.display = "block";
            this.renderPreview();
        }
    }

    loadActiveDocument() {
        const doc = this.getActiveDocument();
        if (doc) {
            this.titleInput.value = doc.title;
            this.editor.value = doc.content;
            this.updateLineNumbers();
            this.renderPreview();
            this.updateTelemetry();
        }
    }

    updateLineNumbers() {
        const lines = this.editor.value.split("\\n").length;
        let numbersHtml = "";
        for (let i = 1; i <= lines; i++) {
            numbersHtml += i + "\\n";
        }
        this.lineNumbers.textContent = numbersHtml;
    }

    renderPreview() {
        const raw = this.editor.value;
        if (!window.marked) {
            this.preview.innerText = raw;
            return;
        }

        // 1. Pre-process LaTeX equations so marked doesn't mangle backslashes or underscores
        const mathBlocks = [];
        let processed = raw.replace(/\\$\\$([\\s\\S]*?)\\$\\$/g, (match, eq) => {
            const placeholder = `%%MATH_DISPLAY_${mathBlocks.length}%%`;
            mathBlocks.push({ type: 'display', eq });
            return placeholder;
        });

        processed = processed.replace(/(?<!\\$)\\$([^$\\n]+)\\$(?!\\$)/g, (match, eq) => {
            const placeholder = `%%MATH_INLINE_${mathBlocks.length}%%`;
            mathBlocks.push({ type: 'inline', eq });
            return placeholder;
        });

        // 2. Parse Markdown via Marked
        let html = marked.parse(processed, { gfm: true, breaks: true });

        // 3. Re-inject KaTeX math equations
        mathBlocks.forEach((item, index) => {
            const placeholder = item.type === 'display' ? `%%MATH_DISPLAY_${index}%%` : `%%MATH_INLINE_${index}%%`;
            let rendered = item.eq;
            if (window.katex) {
                try {
                    rendered = katex.renderToString(item.eq, {
                        displayMode: item.type === 'display',
                        throwOnError: false
                    });
                } catch (err) {
                    rendered = `<span style="color:#F87171">[LaTeX Error: ${err.message}]</span>`;
                }
            }
            html = html.replace(placeholder, rendered);
        });

        // 4. Transform Callouts (> [!NOTE])
        html = html.replace(/<blockquote>\\s*<p>\\s*\\[!(NOTE|TIP|WARNING|DANGER|INFO|QUESTION)\\]/gi, (match, type) => {
            const colors = {
                NOTE: '#38BDF8',
                TIP: '#34D399',
                WARNING: '#FBBF24',
                DANGER: '#F87171',
                INFO: '#00E5FF',
                QUESTION: '#A78BFA'
            };
            const col = colors[type.toUpperCase()] || '#38BDF8';
            return `<blockquote style="border-left: 4px solid ${col}; background: rgba(30, 41, 59, 0.4); padding: 8px 14px; border-radius: 4px; margin: 12px 0;"><p><strong style="color: ${col}">[${type.toUpperCase()}]</strong>`;
        });

        // 5. Transform WikiLinks [[Title]]
        html = html.replace(/\\[\\[([^\\]]+)\\]\\]/g, (match, title) => {
            return `<span style="color: var(--accent-violet); font-weight: bold; text-decoration: underline; cursor: pointer;">🔗 ${title}</span>`;
        });

        this.preview.innerHTML = html;

        // Render Mermaid diagrams asynchronously if any are present
        if (window.mermaid && html.includes('class="language-mermaid"')) {
            try {
                const mermaidNodes = this.preview.querySelectorAll('pre code.language-mermaid');
                mermaidNodes.forEach((node, idx) => {
                    const code = node.textContent;
                    const container = document.createElement('div');
                    container.className = 'mermaid';
                    container.textContent = code;
                    node.parentElement.replaceWith(container);
                });
                mermaid.run();
            } catch (e) {
                console.warn("Mermaid render notice:", e);
            }
        }
    }

    initMermaid() {
        if (window.mermaid) {
            mermaid.initialize({
                startOnLoad: false,
                theme: 'dark',
                themeVariables: {
                    primaryColor: '#16233B',
                    primaryTextColor: '#00E5FF',
                    primaryBorderColor: '#00E5FF',
                    lineColor: '#C084FC',
                    secondaryColor: '#0D1526',
                    tertiaryColor: '#1E293B'
                }
            });
        }
    }

    updateTelemetry() {
        const text = this.editor.value;
        const chars = text.length;
        const words = text.trim() ? text.trim().split(/\\s+/).length : 0;
        const lines = text ? text.split("\\n").length : 1;
        const readMin = Math.max(1, Math.round(words / 200));

        const mathMatches = text.match(/\\$\\$[\\s\\S]*?\\$\\$|(?<!\\$)\\$[^$\\n]+\\$(?!\\$)/g);
        const mathCount = mathMatches ? mathMatches.length : 0;

        const diagMatches = text.match(/\\`\\`\\`(mermaid|cytoscape)/g);
        const diagCount = diagMatches ? diagMatches.length : 0;

        this.statLines.textContent = lines;
        this.statWords.textContent = words;
        this.statChars.textContent = chars;
        this.statRead.textContent = `~${readMin}m`;
        this.statLatex.textContent = `${mathCount} eq`;
        this.statDiagrams.textContent = `${diagCount} diag`;
    }

    renderSidebar() {
        this.sidebarDocList.innerHTML = "";
        const query = document.getElementById("sidebar-search-input").value.toLowerCase().trim();

        const filtered = this.documents.filter(d => {
            if (!query) return true;
            return d.title.toLowerCase().includes(query) || d.content.toLowerCase().includes(query);
        });

        filtered.forEach(doc => {
            const item = document.createElement("div");
            item.className = "doc-item" + (doc.id === this.activeDocId ? " active" : "");

            const title = document.createElement("span");
            title.className = "doc-item-title";
            title.textContent = (doc.isPinned ? "📌 " : "📄 ") + doc.title;

            const delBtn = document.createElement("button");
            delBtn.className = "doc-item-delete";
            delBtn.textContent = "✕";
            delBtn.title = "Delete document";
            delBtn.onclick = (e) => {
                e.stopPropagation();
                if (this.documents.length <= 1) {
                    alert("Cannot delete the only document.");
                    return;
                }
                if (confirm(`Delete "${doc.title}"?`)) {
                    this.documents = this.documents.filter(d => d.id !== doc.id);
                    if (this.activeDocId === doc.id) {
                        this.activeDocId = this.documents[0].id;
                    }
                    this.saveDocuments();
                    this.renderSidebar();
                    this.loadActiveDocument();
                }
            };

            item.appendChild(title);
            item.appendChild(delBtn);

            item.onclick = () => {
                this.activeDocId = doc.id;
                this.saveDocuments();
                this.renderSidebar();
                this.loadActiveDocument();
            };

            this.sidebarDocList.appendChild(item);
        });
    }

    insertFormatting(prefix, suffix) {
        const start = this.editor.selectionStart;
        const end = this.editor.selectionEnd;
        const text = this.editor.value;
        const selected = text.substring(start, end);

        const replacement = prefix + (selected || "text") + suffix;
        this.editor.value = text.substring(0, start) + replacement + text.substring(end);
        this.editor.focus();
        this.editor.selectionStart = start + prefix.length;
        this.editor.selectionEnd = start + prefix.length + (selected ? selected.length : 4);

        this.editor.dispatchEvent(new Event("input"));
    }

    executeFind() {
        const q = this.findQuery.value;
        this.findMatches = [];
        this.currentMatchIdx = -1;

        if (!q) {
            this.findMatchCount.textContent = "0/0";
            return;
        }

        const text = this.editor.value;
        let pos = 0;
        while ((pos = text.indexOf(q, pos)) !== -1) {
            this.findMatches.push(pos);
            pos += q.length;
        }

        if (this.findMatches.length > 0) {
            this.currentMatchIdx = 0;
            this.highlightMatch();
        } else {
            this.findMatchCount.textContent = "0/0";
        }
    }

    highlightMatch() {
        if (this.currentMatchIdx >= 0 && this.currentMatchIdx < this.findMatches.length) {
            const pos = this.findMatches[this.currentMatchIdx];
            const q = this.findQuery.value;
            this.editor.focus();
            this.editor.setSelectionRange(pos, pos + q.length);
            this.findMatchCount.textContent = `${this.currentMatchIdx + 1}/${this.findMatches.length}`;
        }
    }

    findNext() {
        if (this.findMatches.length === 0) return;
        this.currentMatchIdx = (this.currentMatchIdx + 1) % this.findMatches.length;
        this.highlightMatch();
    }

    findPrev() {
        if (this.findMatches.length === 0) return;
        this.currentMatchIdx = (this.currentMatchIdx - 1 + this.findMatches.length) % this.findMatches.length;
        this.highlightMatch();
    }

    replaceCurrent() {
        if (this.currentMatchIdx < 0 || this.findMatches.length === 0) return;
        const pos = this.findMatches[this.currentMatchIdx];
        const q = this.findQuery.value;
        const rep = this.replaceQuery.value;
        const text = this.editor.value;

        this.editor.value = text.substring(0, pos) + rep + text.substring(pos + q.length);
        this.editor.dispatchEvent(new Event("input"));
        this.executeFind();
    }

    replaceAll() {
        const q = this.findQuery.value;
        const rep = this.replaceQuery.value;
        if (!q) return;

        this.editor.value = this.editor.value.split(q).join(rep);
        this.editor.dispatchEvent(new Event("input"));
        this.executeFind();
    }

    // --- Interactive 2D Network Topology Graph & Folder Tree ---

    openGraphModal() {
        this.graphModal.classList.remove("hidden");
        this.resizeCanvas();
        this.buildGraphNodes();
        this.updateInspector();
    }

    setGraphMode(mode) {
        this.graphMode = mode;
        document.getElementById("btn-graph-mode-network").classList.toggle("active", mode === "network");
        document.getElementById("btn-graph-mode-tree").classList.toggle("active", mode === "tree");

        if (mode === "network") {
            this.graphCanvasContainer.style.display = "block";
            this.folderTreeContainer.style.display = "none";
            this.resizeCanvas();
        } else {
            this.graphCanvasContainer.style.display = "none";
            this.folderTreeContainer.style.display = "flex";
            this.renderFolderTree();
        }
    }

    resizeCanvas() {
        const rect = this.graphCanvasContainer.getBoundingClientRect();
        this.graphCanvas.width = rect.width;
        this.graphCanvas.height = rect.height;
    }

    buildGraphNodes() {
        this.nodes = [];
        this.edges = [];

        const centerX = this.graphCanvas.width / 2 || 450;
        const centerY = this.graphCanvas.height / 2 || 350;

        // 1. Root Workspace Node
        const rootNode = {
            id: "workspace_root",
            title: "HyperMark Vault",
            type: "root",
            x: centerX,
            y: centerY - 160,
            color: "#00E5FF",
            size: 34
        };
        this.nodes.push(rootNode);

        // 2. Folder Nodes
        const folderNotes = { id: "f_notes", title: "📁 Notes & Docs", type: "folder", x: centerX - 200, y: centerY - 30, color: "#38BDF8", size: 28 };
        const folderGraphs = { id: "f_graphs", title: "📁 Graphs & Topology", type: "folder", x: centerX, y: centerY + 10, color: "#C084FC", size: 28 };
        const folderScience = { id: "f_science", title: "📁 CTAN & Math Lab", type: "folder", x: centerX + 200, y: centerY - 30, color: "#34D399", size: 28 };

        if (this.showFolders) {
            this.nodes.push(folderNotes, folderGraphs, folderScience);
            this.edges.push({ source: rootNode.id, target: folderNotes.id, color: "#38BDF8" });
            this.edges.push({ source: rootNode.id, target: folderGraphs.id, color: "#C084FC" });
            this.edges.push({ source: rootNode.id, target: folderScience.id, color: "#34D399" });
        }

        // 3. Document Nodes positioned in orbital layout
        this.documents.forEach((doc, idx) => {
            const angle = (idx / Math.max(1, this.documents.length)) * 2 * Math.PI;
            const dist = 240 + (idx % 2) * 60;
            const px = centerX + dist * Math.cos(angle);
            const py = centerY + 80 + dist * Math.sin(angle);

            const isMath = doc.content.includes("katex") || doc.content.includes("amsmath") || doc.content.includes("$$");
            const isGraph = doc.content.includes("cytoscape") || doc.content.includes("mermaid");

            const color = isGraph ? "#C084FC" : isMath ? "#34D399" : "#38BDF8";

            const docNode = {
                id: doc.id,
                title: doc.title,
                type: "document",
                x: px,
                y: py,
                color: color,
                docId: doc.id,
                size: doc.id === this.activeDocId ? 30 : 25
            };
            this.nodes.push(docNode);

            if (this.showFolders) {
                const targetFolder = isGraph ? folderGraphs.id : isMath ? folderScience.id : folderNotes.id;
                this.edges.push({ source: targetFolder, target: doc.id, color: color });
            } else {
                this.edges.push({ source: rootNode.id, target: doc.id, color: color });
            }

            // Cross-document links detection
            this.documents.forEach(other => {
                if (other.id !== doc.id && doc.content.includes(other.title)) {
                    this.edges.push({ source: doc.id, target: other.id, color: "#F472B6", isLink: true });
                }
            });
        });

        // 4. Tag Nodes
        if (this.showTags) {
            const tags = ["latex", "cytoscape", "mermaid", "vault", "gfm"];
            tags.forEach((tag, i) => {
                const tx = centerX - 240 + i * 120;
                const ty = centerY + 260 + (i % 2) * 40;
                const tagNode = { id: "tag_" + tag, title: "#" + tag, type: "tag", x: tx, y: ty, color: "#FBBF24", size: 20 };
                this.nodes.push(tagNode);

                this.documents.forEach(doc => {
                    if (doc.content.toLowerCase().includes(tag)) {
                        this.edges.push({ source: doc.id, target: tagNode.id, color: "#FBBF24", isDashed: true });
                    }
                });
            });
        }
    }

    startGraphAnimationLoop() {
        const render = () => {
            this.pulseTime += 0.025;
            if (!this.graphModal.classList.contains("hidden") && this.graphMode === "network") {
                this.drawGraph();
            }
            requestAnimationFrame(render);
        };
        requestAnimationFrame(render);
    }

    drawGraph() {
        const ctx = this.ctx;
        const width = this.graphCanvas.width;
        const height = this.graphCanvas.height;
        ctx.clearRect(0, 0, width, height);

        ctx.save();
        ctx.translate(this.graphOffset.x, this.graphOffset.y);
        ctx.scale(this.graphScale, this.graphScale);

        // 1. Grid Background Pattern
        const gridSpacing = 60;
        ctx.strokeStyle = "#0E1626";
        ctx.lineWidth = 1;
        ctx.beginPath();
        for (let x = -this.graphOffset.x / this.graphScale; x < (width - this.graphOffset.x) / this.graphScale; x += gridSpacing) {
            const snappedX = Math.floor(x / gridSpacing) * gridSpacing;
            ctx.moveTo(snappedX, -this.graphOffset.y / this.graphScale);
            ctx.lineTo(snappedX, (height - this.graphOffset.y) / this.graphScale);
        }
        for (let y = -this.graphOffset.y / this.graphScale; y < (height - this.graphOffset.y) / this.graphScale; y += gridSpacing) {
            const snappedY = Math.floor(y / gridSpacing) * gridSpacing;
            ctx.moveTo(-this.graphOffset.x / this.graphScale, snappedY);
            ctx.lineTo((width - this.graphOffset.x) / this.graphScale, snappedY);
        }
        ctx.stroke();

        const nodeMap = {};
        this.nodes.forEach(n => nodeMap[n.id] = n);

        // 2. Draw Smooth Curved Bézier Edges
        this.edges.forEach(edge => {
            const src = nodeMap[edge.source];
            const dst = nodeMap[edge.target];
            if (!src || !dst) return;

            const isHighlighted = this.selectedNodeId && (edge.source === this.selectedNodeId || edge.target === this.selectedNodeId);
            const isDimmed = this.selectedNodeId && !isHighlighted;

            ctx.save();
            ctx.strokeStyle = isHighlighted ? "#00E5FF" : isDimmed ? "rgba(255,255,255,0.04)" : edge.color;
            ctx.globalAlpha = isHighlighted ? 0.9 : isDimmed ? 0.05 : 0.35;
            ctx.lineWidth = isHighlighted ? 3 : 1.8;

            if (edge.isDashed) {
                ctx.setLineDash([8, 6]);
            }

            // Curved Quadratic Bézier offset
            const midX = (src.x + dst.x) / 2;
            const midY = (src.y + dst.y) / 2;
            const dx = dst.x - src.x;
            const dy = dst.y - src.y;
            const ctrlX = midX - dy * 0.12;
            const ctrlY = midY + dx * 0.12;

            ctx.beginPath();
            ctx.moveTo(src.x, src.y);
            ctx.quadraticCurveTo(ctrlX, ctrlY, dst.x, dst.y);
            ctx.stroke();
            ctx.restore();
        });

        // 3. Draw Nodes with Halos & Pulsing Animation
        this.nodes.forEach(node => {
            const isSelected = node.id === this.selectedNodeId;
            const isActiveDoc = node.docId === this.activeDocId;
            const matchesSearch = this.graphSearch && node.title.toLowerCase().includes(this.graphSearch);

            ctx.save();

            // --- Multi-tiered Radar Pulse Animation on Selected Node ---
            if (isSelected) {
                // Wave 1: Radiating Expanding Radar Ring
                const pulseCycle = (this.pulseTime * 0.8) % 1; // 0 to 1
                const rippleRadius = node.size * (1.0 + pulseCycle * 1.8);
                const rippleAlpha = Math.max(0, 0.85 * (1 - pulseCycle));

                ctx.strokeStyle = `rgba(0, 229, 255, ${rippleAlpha})`;
                ctx.lineWidth = 2.5;
                ctx.beginPath();
                ctx.arc(node.x, node.y, rippleRadius, 0, Math.PI * 2);
                ctx.stroke();

                // Wave 2: Breathing Radial Glow Halo
                const breathGlow = 0.45 + Math.sin(this.pulseTime * 2.5) * 0.25;
                const glowGrad = ctx.createRadialGradient(node.x, node.y, node.size * 0.5, node.x, node.y, node.size * 2.8);
                glowGrad.addColorStop(0, `rgba(0, 229, 255, ${breathGlow})`);
                glowGrad.addColorStop(1, 'rgba(0, 229, 255, 0)');
                ctx.fillStyle = glowGrad;
                ctx.beginPath();
                ctx.arc(node.x, node.y, node.size * 2.8, 0, Math.PI * 2);
                ctx.fill();
            } else if (isActiveDoc) {
                // Ambient cyan glow for active document
                const glowGrad = ctx.createRadialGradient(node.x, node.y, node.size * 0.4, node.x, node.y, node.size * 2.0);
                glowGrad.addColorStop(0, 'rgba(0, 229, 255, 0.35)');
                glowGrad.addColorStop(1, 'rgba(0, 229, 255, 0)');
                ctx.fillStyle = glowGrad;
                ctx.beginPath();
                ctx.arc(node.x, node.y, node.size * 2.0, 0, Math.PI * 2);
                ctx.fill();
            }

            // Node Base Solid Fill
            ctx.fillStyle = isSelected ? "#16233B" : "#0D1424";
            ctx.beginPath();
            ctx.arc(node.x, node.y, node.size, 0, Math.PI * 2);
            ctx.fill();

            // Glowing Outer Stroke
            ctx.strokeStyle = isSelected ? "#00E5FF" : node.color;
            ctx.lineWidth = isSelected ? 3.5 : 2;
            ctx.beginPath();
            ctx.arc(node.x, node.y, node.size, 0, Math.PI * 2);
            ctx.stroke();

            // Core Beacon Dot
            const coreRadius = isSelected ? node.size * 0.45 : node.size * 0.35;
            ctx.fillStyle = isSelected ? "#00E5FF" : node.color;
            ctx.beginPath();
            ctx.arc(node.x, node.y, coreRadius, 0, Math.PI * 2);
            ctx.fill();

            // --- Readability Contrast Capsule Pill & Density-Scaled Label ---
            const fontSize = Math.round(13 * this.labelTextScale);
            ctx.font = `${isSelected || matchesSearch ? 'bold ' : ''}${fontSize}px system-ui, sans-serif`;

            const textWidth = ctx.measureText(node.title).width;
            const pillPadX = 8 * this.labelTextScale;
            const pillPadY = 4 * this.labelTextScale;
            const textY = node.y + node.size + 18;

            const pillX = node.x - textWidth / 2 - pillPadX;
            const pillY = textY - fontSize;
            const pillW = textWidth + pillPadX * 2;
            const pillH = fontSize + pillPadY * 1.5;

            // Draw dark contrast capsule pill background
            ctx.fillStyle = isSelected ? "rgba(9, 20, 40, 0.95)" : matchesSearch ? "rgba(15, 30, 56, 0.95)" : "rgba(8, 14, 26, 0.88)";
            ctx.beginPath();
            ctx.roundRect(pillX, pillY, pillW, pillH, 6);
            ctx.fill();

            // Capsule Border
            ctx.strokeStyle = isSelected ? "#00E5FF" : matchesSearch ? "#38BDF8" : "rgba(30, 42, 63, 0.8)";
            ctx.lineWidth = isSelected ? 1.5 : 1;
            ctx.stroke();

            // Label Text
            ctx.fillStyle = isSelected ? "#FFFFFF" : matchesSearch ? "#00E5FF" : node.type === "tag" ? "#FBBF24" : "#F1F5F9";
            ctx.textAlign = "center";
            ctx.textBaseline = "alphabetic";
            ctx.fillText(node.title, node.x, textY);

            ctx.restore();
        });

        ctx.restore();
    }

    initCanvasInteractions() {
        let isDragging = false;
        let startX = 0, startY = 0;

        this.graphCanvas.addEventListener("mousedown", (e) => {
            const rect = this.graphCanvas.getBoundingClientRect();
            const mouseX = (e.clientX - rect.left - this.graphOffset.x) / this.graphScale;
            const mouseY = (e.clientY - rect.top - this.graphOffset.y) / this.graphScale;

            const hit = this.nodes.slice().reverse().find(n => {
                const dx = n.x - mouseX;
                const dy = n.y - mouseY;
                return Math.sqrt(dx * dx + dy * dy) <= n.size + 16;
            });

            if (hit) {
                this.draggedNode = hit;
                this.selectedNodeId = hit.id;
                this.updateInspector();
            } else {
                this.selectedNodeId = null;
                this.updateInspector();
            }

            isDragging = true;
            startX = e.clientX;
            startY = e.clientY;
        });

        window.addEventListener("mousemove", (e) => {
            if (!isDragging) return;
            const dx = (e.clientX - startX);
            const dy = (e.clientY - startY);
            startX = e.clientX;
            startY = e.clientY;

            if (this.draggedNode) {
                this.draggedNode.x += dx / this.graphScale;
                this.draggedNode.y += dy / this.graphScale;
            } else {
                this.graphOffset.x += dx;
                this.graphOffset.y += dy;
            }
        });

        window.addEventListener("mouseup", () => {
            isDragging = false;
            this.draggedNode = null;
        });

        // Wheel Zoom
        this.graphCanvas.addEventListener("wheel", (e) => {
            e.preventDefault();
            const zoomFactor = e.deltaY < 0 ? 1.15 : 0.85;
            this.graphScale = Math.min(3.8, Math.max(0.35, this.graphScale * zoomFactor));
        });
    }

    updateInspector() {
        const inspector = this.graphInspector;
        if (!this.selectedNodeId) {
            inspector.classList.add("hidden");
            return;
        }

        const doc = this.documents.find(d => d.id === this.selectedNodeId);
        if (!doc) {
            inspector.classList.add("hidden");
            return;
        }

        inspector.classList.remove("hidden");
        document.getElementById("inspector-doc-title").textContent = doc.title;
        document.getElementById("inspector-badge-chars").textContent = `${doc.content.length} chars`;

        const connected = this.edges
            .filter(e => e.source === doc.id || e.target === doc.id)
            .map(e => e.source === doc.id ? e.target : e.source)
            .filter(id => id !== doc.id && this.documents.some(d => d.id === id));

        document.getElementById("inspector-badge-links").textContent = `Linked: ${connected.length} docs`;

        const snippetLine = doc.content.split("\\n").find(l => l.trim() && !l.startsWith("#")) || doc.content.substring(0, 80);
        document.getElementById("inspector-snippet").textContent = snippetLine;

        const connRow = document.getElementById("inspector-connected-row");
        connRow.innerHTML = "";
        connected.forEach(cid => {
            const cdoc = this.documents.find(d => d.id === cid);
            if (cdoc) {
                const chip = document.createElement("span");
                chip.style.cssText = "background: #1E293B; border: 1px solid #334155; color: #CBD5E1; padding: 2px 8px; border-radius: 6px; font-size: 11px; cursor: pointer;";
                chip.textContent = "🔗 " + cdoc.title;
                chip.onclick = () => {
                    this.selectedNodeId = cdoc.id;
                    this.updateInspector();
                };
                connRow.appendChild(chip);
            }
        });
    }

    renderFolderTree() {
        const container = this.folderTreeContainer;
        container.innerHTML = "";

        const folders = [
            { name: "📁 Notes & Docs", color: "#38BDF8", filter: d => !d.content.includes("mermaid") && !d.content.includes("katex") && !d.content.includes("$$") },
            { name: "📁 Topology & Graphs", color: "#C084FC", filter: d => d.content.includes("cytoscape") || d.content.includes("mermaid") },
            { name: "📁 CTAN & Math Lab", color: "#34D399", filter: d => d.content.includes("katex") || d.content.includes("amsmath") || d.content.includes("$$") }
        ];

        folders.forEach(f => {
            const card = document.createElement("div");
            card.className = "folder-card";

            const docsInFolder = this.documents.filter(f.filter);

            const header = document.createElement("div");
            header.className = "folder-card-header";
            header.innerHTML = `
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="color: ${f.color}">▼</span>
                    <span style="color: #FFFFFF">${f.name}</span>
                </div>
                <span style="background: rgba(255,255,255,0.1); color: ${f.color}; padding: 2px 8px; border-radius: 10px; font-size: 11px;">${docsInFolder.length} files</span>
            `;

            const list = document.createElement("div");
            list.style.cssText = "display: flex; flex-direction: column; gap: 8px; margin-top: 12px;";

            docsInFolder.forEach(d => {
                const item = document.createElement("div");
                item.style.cssText = "display: flex; align-items: center; justify-content: space-between; background: rgba(15, 23, 42, 0.7); border: 1px solid var(--border-dark); border-radius: 8px; padding: 10px 14px;";

                item.innerHTML = `
                    <div>
                        <div style="font-weight: 600; font-size: 13px; color: #FFFFFF;">${d.title}</div>
                        <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">${d.content.length} chars • ${d.content.split(/\\s+/).length} words</div>
                    </div>
                `;

                const openBtn = document.createElement("button");
                openBtn.className = "btn-open-editor";
                openBtn.style.cssText = "padding: 6px 12px; font-size: 11px;";
                openBtn.textContent = "Open in Editor";
                openBtn.onclick = () => {
                    this.activeDocId = d.id;
                    this.loadActiveDocument();
                    this.renderSidebar();
                    this.graphModal.classList.add("hidden");
                };

                item.appendChild(openBtn);
                list.appendChild(item);
            });

            card.appendChild(header);
            card.appendChild(list);
            container.appendChild(card);
        });
    }
}

// Bootstrap on DOM ready
document.addEventListener("DOMContentLoaded", () => {
    window.app = new HyperMarkApp();
});
