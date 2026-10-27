// HyperMark Linux Desktop Core Application Engine

class HyperMarkApp {
    constructor() {
        this.documents = this.loadDocuments();
        this.activeDocId = this.documents[0]?.id || "doc_math_ctan";
        this.viewMode = "split"; // 'editor', 'split', 'preview'
        this.findMatches = [];
        this.currentMatchIdx = -1;
        this.findCaseSensitive = false;

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

        // Vault state (real .md files on disk, served by the launcher)
        this.vaultApi = false;
        this._vaultSaveTimers = new Map(); // fileName -> timer id

        // LaTeX engine (tectonic) — status learned async from the launcher
        this.latexEngine = undefined;    // undefined = unknown yet
        this._latexCache = new Map();    // sourceHash -> { id, error }
        this._pendingLatexCards = [];

        this.initDOMElements();
        this.initEventListeners();
        this.initToast();
        this.initLatexEngineStatus();
        this.renderSidebar();
        this.loadActiveDocument();
        this.initMermaid();
        this.startGraphAnimationLoop();
        this.bootFromVault(); // async upgrade to disk-backed vault when available
    }

    // ---------------------------------------------------------------------
    // Persistence: disk-backed vault (via launcher API) with localStorage
    // fallback / migration so pre-vault notes are never lost.
    // ---------------------------------------------------------------------

    async discoverVault() {
        if (this.vaultApi !== null && this._vaultDiscovered) return this.vaultApi;
        this._vaultDiscovered = true;
        try {
            const res = await fetch("api/vault/list", { headers: { "Accept": "application/json" } });
            this.vaultApi = res.ok;
        } catch (e) {
            this.vaultApi = false;
        }
        return this.vaultApi;
    }

    async bootFromVault() {
        const hasVault = await this.discoverVault();
        if (!hasVault) { this.setVaultStatus("LOCAL ONLY"); return; }

        try {
            const list = await (await fetch("api/vault/list")).json();
            const docs = [];
            for (const f of (list.files || [])) {
                const res = await fetch("api/vault/read?name=" + encodeURIComponent(f.name));
                if (!res.ok) continue;
                const content = await res.text();
                docs.push({
                    id: "vault_" + f.name.replace(/\W+/g, "_"),
                    fileName: f.name,
                    title: f.name.replace(/\.(md|markdown|txt)$/i, ""),
                    isPinned: f.name.startsWith("00-"),
                    updatedAt: f.mtime || 0,
                    content: content
                });
            }
            docs.sort((a, b) => b.updatedAt - a.updatedAt);

            if (docs.length === 0) {
                // First run on a fresh vault: seed it with whatever we started
                // with (migrated localStorage notes, else the default docs).
                const hasContent = this.documents.some(d => (d.content || "").trim());
                const source = hasContent ? this.documents : JSON.parse(JSON.stringify(defaultDocuments));
                for (const d of source) {
                    d.fileName = this.uniqueVaultFileName(d.title + ".md");
                    await this.vaultWrite(d.fileName, d.content);
                }
                this.documents = source;
            } else {
                this.documents = docs;
            }

            this.activeDocId = this.documents[0].id;
            this.renderSidebar();
            this.loadActiveDocument();
            this.setVaultStatus("SYNCED");
        } catch (e) {
            console.warn("Vault boot failed, staying on localStorage:", e);
            this.setVaultStatus("LOCAL ONLY");
        }
    }

    uniqueVaultFileName(base) {
        const ext = (base.match(/\.(md|markdown|txt)$/i) || [".md"])[0];
        const stem = base.slice(0, base.length - ext.length).replace(/\s+/g, "_").replace(/[^\w-]+/g, "_").replace(/_+/g, "_").replace(/^_|_$/g, "") || "untitled";
        const taken = new Set(this.documents.map(d => d.fileName).filter(Boolean));
        let name = stem + ext, i = 2;
        while (taken.has(name)) name = `${stem}_${i++}${ext}`;
        return name;
    }

    async vaultWrite(name, content) {
        const res = await fetch("api/vault/write", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, content })
        });
        if (!res.ok) throw new Error(`vault write failed (${res.status})`);
        return res.json();
    }

    async vaultDelete(name) {
        const res = await fetch("api/vault/delete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name })
        });
        return res.ok;
    }

    // Debounced per-file save — a burst of keystrokes writes the file once.
    queueVaultSave(doc) {
        if (!this.vaultApi || !doc) return;
        clearTimeout(this._vaultSaveTimers.get(doc.fileName));
        this._vaultSaveTimers.set(doc.fileName, setTimeout(() => {
            this._vaultSaveTimers.delete(doc.fileName);
            if (!doc.fileName) return;
            this.vaultWrite(doc.fileName, doc.content)
                .then(() => this.setVaultStatus("SYNCED"))
                .catch(() => this.setVaultStatus("ERROR"));
        }, 500));
        this.setVaultStatus("SYNCING…");
    }

    setVaultStatus(state) {
        this.vaultStatusEl.textContent = state;
        this.vaultStatusEl.title = this.vaultApi
            ? "Notes are stored as plain .md files in ~/.local/share/hypermark/vault/"
            : "Vault API unavailable — notes are stored in the browser profile only";
        this.vaultStatusEl.style.color = state.includes("SYNC") && state !== "SYNCING…"
            ? "var(--accent-emerald)" : state === "ERROR" ? "var(--accent-amber)" : "var(--accent-cyan)";
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

    saveDocuments(immediate = false) {
        if (immediate) { this.doSaveDocuments(); return; }
        clearTimeout(this._docsSaveTimer);
        this._docsSaveTimer = setTimeout(() => this.doSaveDocuments(), 400);
    }

    doSaveDocuments() {
        // Legacy mirror (survives when the vault API is unavailable)
        try {
            localStorage.setItem("hypermark_docs", JSON.stringify(this.documents));
        } catch (e) {
            console.error("Failed to save to local storage:", e);
        }
        this.queueVaultSave(this.getActiveDocument());
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
        this.statDocs = document.getElementById("stat-docs");

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

        // Vault status + toast (bottom bar right side)
        this.vaultStatusEl = document.getElementById("vault-status");
    }

    // ---------------------------------------------------------------------
    // Editor input: instant line numbers / telemetry, debounced heavy render
    // ---------------------------------------------------------------------

    handleEditorInput() {
        const doc = this.getActiveDocument();
        if (!doc) return;
        doc.content = this.editor.value;
        doc.updatedAt = Date.now();
        this.saveDocuments();          // debounced inside saveDocuments()
        this.updateLineNumbers();
        this.updateTelemetry();
        if (this.viewMode !== "editor") {
            clearTimeout(this._previewTimer);
            this._previewTimer = setTimeout(() => this.renderPreview(), 180);
        }
    }

    initEventListeners() {
        // Text changes (debounced preview render, instant stats)
        this.editor.addEventListener("input", () => this.handleEditorInput());

        // Sync scroll between textarea and line numbers
        this.editor.addEventListener("scroll", () => {
            this.lineNumbers.scrollTop = this.editor.scrollTop;
        });

        // Title rename (also renames the vault file)
        this.titleInput.addEventListener("change", async () => {
            const doc = this.getActiveDocument();
            if (!doc) return;
            const oldFile = doc.fileName;
            doc.title = this.titleInput.value.trim() || "Untitled";
            doc.updatedAt = Date.now();
            if (this.vaultApi && oldFile) {
                const newFile = this.uniqueVaultFileName(doc.title + ".md");
                try {
                    // Cancel a pending write to the old name (else it comes back)
                    const pending = this._vaultSaveTimers.get(oldFile);
                    if (pending) { clearTimeout(pending); this._vaultSaveTimers.delete(oldFile); }
                    await this.vaultWrite(newFile, doc.content);
                    if (newFile !== oldFile) {
                        doc.fileName = newFile;
                        const newId = "vault_" + newFile.replace(/\W+/g, "_");
                        if (this.activeDocId === doc.id) this.activeDocId = newId;
                        doc.id = newId;
                        await this.vaultDelete(oldFile);
                    }
                    this.setVaultStatus("SYNCED");
                } catch (e) {
                    console.warn("Rename failed:", e);
                    this.setVaultStatus("ERROR");
                }
            }
            this.saveDocuments(true);
            this.renderSidebar();
            this.loadActiveDocument();
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
        document.getElementById("btn-new-doc").addEventListener("click", async () => {
            const title = "Untitled Note " + (this.documents.length + 1);
            const doc = await this.createDoc(title, "# Untitled Note\n\nStart writing markdown here...\n");
            this.activeDocId = doc.id;
            this.renderSidebar();
            this.loadActiveDocument();
            this.editor.focus();
        });

        // Open / Import .md file (also copied into the vault so it persists)
        document.getElementById("btn-import-file").addEventListener("click", () => {
            this.filePicker.click();
        });
        this.filePicker.addEventListener("change", async (e) => {
            const file = e.target.files[0];
            e.target.value = "";
            if (!file) return;
            const content = await file.text();
            const title = file.name.replace(/\.(md|markdown|txt)$/i, "") || "Imported Note";
            const doc = await this.createDoc(title, content, file.name.toLowerCase());
            this.activeDocId = doc.id;
            this.renderSidebar();
            this.loadActiveDocument();
        });

        // Export active doc as .md file ("Save to disk" — Ctrl+S saves to vault)
        document.getElementById("btn-export-md").addEventListener("click", async () => {
            const doc = this.getActiveDocument();
            if (!doc) return;
            // Prefer a native save via the vault; fall back to a browser download
            // (also keeps a local .md copy under Downloads from the browser tier).
            const fileName = (doc.fileName || this.uniqueVaultFileName(doc.title + ".md"));
            if (this.vaultApi) {
                try {
                    await this.vaultWrite(fileName, this.editor.value);
                    this.showToast(`💾 Saved to vault: ${fileName}`);
                    this.setVaultStatus("SYNCED");
                    return;
                } catch (e) {
                    console.warn("Vault save failed, downloading instead:", e);
                }
            }
            const blob = new Blob([this.editor.value], { type: "text/markdown;charset=utf-8" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = (doc.title.trim().replace(/[/\\?%*:|"<>]/g, "_") || "Untitled") + ".md";
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
        document.getElementById("btn-find-case").addEventListener("click", (e) => {
            this.findCaseSensitive = !this.findCaseSensitive;
            e.currentTarget.classList.toggle("active", this.findCaseSensitive);
            this.executeFind();
        });
        this.findQuery.addEventListener("input", () => this.executeFind());
        this.findQuery.addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); (e.shiftKey ? this.findPrev() : this.findNext()); }
        });
        this.replaceQuery.addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); this.replaceCurrent(); }
        });
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

        // Draggable split divider (22% – 78%), double-click resets to 50/50
        this.splitDivider.addEventListener("mousedown", (e) => {
            if (e.button !== 0) return;
            e.preventDefault();
            const workspace = document.getElementById("content-split");
            const rect = () => workspace.getBoundingClientRect();
            const onMove = (ev) => {
                const r = rect();
                if (!r.width) return;
                const pct = Math.min(78, Math.max(22, ((ev.clientX - r.left) / r.width) * 100));
                this.editorPane.style.flex = `0 0 ${pct}%`;
                this.previewPane.style.flex = "1 1 auto";
            };
            const onUp = () => {
                window.removeEventListener("mousemove", onMove);
                window.removeEventListener("mouseup", onUp);
            };
            window.addEventListener("mousemove", onMove);
            window.addEventListener("mouseup", onUp);
        });
        this.splitDivider.addEventListener("dblclick", () => {
            this.editorPane.style.flex = "";
            this.previewPane.style.flex = "";
        });

        // Keep the graph canvas crisp and correctly sized on window resizes
        window.addEventListener("resize", () => {
            if (!this.graphModal.classList.contains("hidden")) this.resizeCanvas();
        });

        // Topology Graph Modal
        document.getElementById("btn-open-graph").addEventListener("click", () => {
            this.openGraphModal();
        });
        document.getElementById("btn-close-graph").addEventListener("click", () => {
            this.graphModal.classList.add("hidden");
        });

        // Wiki link navigation (delegated — links live inside re-rendered preview)
        this.preview.addEventListener("click", (e) => {
            const link = e.target.closest("a.wikilink");
            if (!link) return;
            e.preventDefault();
            const title = link.dataset.wiki || "";
            const doc = this.documents.find(d => d.title.toLowerCase() === title.toLowerCase());
            if (doc) {
                this.activeDocId = doc.id;
                this.renderSidebar();
                this.loadActiveDocument();
            } else {
                this.showToast(`"${title}" not found — creating it…`, "Open", () => {
                    this.createDoc(title, `# ${title}\n\n(auto-created from a [[${title}]] link)\n`)
                        .then((nd) => {
                            this.activeDocId = nd.id;
                            this.renderSidebar();
                            this.loadActiveDocument();
                        });
                }, false);
            }
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
            } else if (e.ctrlKey && e.key.toLowerCase() === "m") {
                // Radial formatting wheel
                e.preventDefault();
                this.openRadialWheel();
            } else if (e.ctrlKey && e.key.toLowerCase() === "p") {
                // Toggle fullscreen preview (documented in the Quickstart doc)
                e.preventDefault();
                this.setViewMode(this.viewMode === "preview" ? "split" : "preview");
            } else if (e.key === "Escape") {
                if (!this.graphModal.classList.contains("hidden")) {
                    this.graphModal.classList.add("hidden");
                }
                if (!this.graphViewer || !this.graphViewer.classList.contains("hidden")) {
                    this.closeGraphViewer();
                }
                if (!this.radialModal.classList.contains("hidden")) {
                    this.closeRadialWheel();
                }
                if (!this.emojiPopover.classList.contains("hidden")) {
                    this.closeEmojiPopover();
                }
                if (this.packageModal && !this.packageModal.classList.contains("hidden")) {
                    this.closePackageBrowser();
                }
                if (this.toastEl && !this.toastEl.classList.contains("hidden")) {
                    this.hideToast();
                }
            }
        });

        // Tab inserts spaces; Enter continues lists / checklists
        this.editor.addEventListener("keydown", (e) => {
            if (e.key === "Tab" && !e.ctrlKey && !e.altKey && !e.metaKey) {
                e.preventDefault();
                this.insertAtCursor("  ");
            } else if (e.key === "Enter" && !e.ctrlKey && !e.altKey && !e.metaKey) {
                this.handleListContinuation(e);
            }
        });

        // Canvas Mouse Events
        this.initCanvasInteractions();

        // Radial Formatting Wheel & Emoji palette
        this.initRadialWheel();
        this.initPackageBrowser();
        this.initEmojiPopover();
        this.initGraphViewer();
        this.initSharedPanPump();    // shared mousemove/mouseup panner for diagrams
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
        const lines = this.editor.value.split("\n").length;
        let numbersHtml = "";
        for (let i = 1; i <= lines; i++) {
            numbersHtml += i + "\n";
        }
        this.lineNumbers.textContent = numbersHtml;
    }

    renderPreview() {
        const raw = this.editor.value;
        if (!window.marked) {
            this.preview.innerText = raw;
            return;
        }

        // Any open fullscreen viewer points at nodes we're about to destroy
        if (this.graphViewer && !this.graphViewer.classList.contains("hidden")) {
            this.closeGraphViewer();
        }

        const pv = this.preview;
        const preserveScroll = pv.scrollTop;

        // 1. Protect code spans/fences AND extract LaTeX in one pass, so
        //    prose like "costs $5 and $6" or `$$` inside code stays intact.
        //    Placeholders carry a random run to avoid collisions with real text.
        const mathBlocks = [];
        const phToken = () => `%%MATH_${Date.now().toString(36)}${mathBlocks.length}_${Math.random().toString(36).slice(2, 7)}%%`;
        const processed = raw.replace(
            /(`+[\s\S]*?`+)|(\$\$[\s\S]+?\$\$)|(?<!\$)\$([^$\n]+?)\$(?!\$)/g,
            (match, code, display, inline) => {
                if (code) return code;
                const ph = phToken();
                if (display) mathBlocks.push({ ph, type: "display", eq: display.slice(2, -2) });
                else mathBlocks.push({ ph, type: "inline", eq: inline });
                return ph;
            }
        );

        // 2. Parse Markdown via Marked
        let html = marked.parse(processed, { gfm: true, breaks: true });

        // 3. Re-inject KaTeX math (split/join — immune to $& patterns)
        mathBlocks.forEach((item) => {
            let rendered = item.eq;
            if (window.katex) {
                try {
                    rendered = katex.renderToString(item.eq, {
                        displayMode: item.type === "display",
                        throwOnError: false
                    });
                } catch (err) {
                    rendered = `<span class="math-error">[LaTeX Error: ${err.message}]</span>`;
                }
            }
            html = html.split(item.ph).join(rendered);
        });

        // 4. Transform Callouts (> [!NOTE])
        html = html.replace(/<blockquote>\s*<p>\s*\[!(NOTE|TIP|WARNING|DANGER|INFO|QUESTION)\]/gi, (match, type) => {
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

        // 5. Transform WikiLinks [[Title]] into real links (click = navigate)
        html = html.replace(/\[\[([^\]\[]+)\]\]/g, (match, title) => {
            const safe = title.replace(/"/g, "&quot;").trim();
            return `<a href="#" class="wikilink" data-wiki="${safe}">🔗 ${safe}</a>`;
        });

        pv.innerHTML = html;

        // Keep the reader's place across live re-renders
        pv.scrollTop = Math.min(preserveScroll, Math.max(0, pv.scrollHeight - pv.clientHeight));

        // 6. Render Cytoscape JSON graphs (bundled vector renderer, no engine lib)
        this.renderCytoscapeBlocks();

        // 6b. `latex blocks -> compile cards (tectonic engine via launcher)
        this.replaceLatexBlocks();

        // 7. Render Mermaid diagrams asynchronously if any are present
        const mermaidPending = this.replaceMermaidBlocks();
        if (mermaidPending) {
            // mermaid.run() is async — enhance only after diagrams exist
            Promise.resolve(mermaidPending)
                .catch((e) => console.warn("Mermaid render notice:", e))
                .then(() => this.enhanceMermaidGraphs());
        } else {
            this.enhanceMermaidGraphs();
        }
    }

    replaceMermaidBlocks() {
        const codeNodes = this.preview.querySelectorAll('pre code.language-mermaid');
        if (!codeNodes.length || !window.mermaid) return null;
        try {
            codeNodes.forEach((node) => {
                const container = document.createElement('div');
                container.className = 'mermaid';
                container.textContent = node.textContent;
                node.parentElement.replaceWith(container);
            });
            return mermaid.run();
        } catch (e) {
            console.warn("Mermaid render notice:", e);
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Cytoscape-style JSON graphs rendered as SVG (same look as the vault
    // topology canvas) — styled by enhanceMermaidGraphs like Mermaid output.
    // ---------------------------------------------------------------------

    renderCytoscapeBlocks() {
        const codeNodes = this.preview.querySelectorAll('pre code.language-cytoscape');
        if (!codeNodes.length) return;

        codeNodes.forEach((node) => {
            const pre = node.parentElement;
            const container = document.createElement('div');
            container.className = 'cytoscape-svg';

            let json = null;
            try {
                json = JSON.parse(node.textContent);
            } catch (err) {
                container.innerHTML = `<div class="cytoscape-error">⚠️ Invalid Cytoscape JSON: ${err.message}</div>`;
                pre.replaceWith(container);
                return;
            }

            const nodes = Array.isArray(json?.nodes) ? json.nodes.map(n => n.data || {}) : [];
            const edges = Array.isArray(json?.edges) ? json.edges.map(e => e.data || {}) : [];
            if (!nodes.length) {
                container.innerHTML = `<div class="cytoscape-error">⚠️ Cytoscape graph has no nodes</div>`;
                pre.replaceWith(container);
                return;
            }

            // Deterministic layout: nodes on a ring sized to fit labels,
            // first node (if it looks like a hub) centered.
            const esc = (s) => String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
            const pad = 130;
            const labelW = 150;
            const R = Math.max(230, (labelW * nodes.length) / (2 * Math.PI) + 55);
            const W = Math.ceil(2 * (R + pad)), H = Math.ceil(2 * (R + pad));
            const cx = W / 2, cy = H / 2;

            const positions = nodes.map((n, i) => {
                if (i === 0 && nodes.length > 2) {
                    return { x: cx, y: cy };
                }
                const a = ((i - 1) / Math.max(1, nodes.length - 1)) * Math.PI * 2 - Math.PI / 2;
                return { x: cx + R * Math.cos(a) * 0.95, y: cy + R * Math.sin(a) * 0.7 };
            });

            const nodeMap = {};
            nodes.forEach((n, i) => { nodeMap[n.id] = positions[i]; });

            let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}" class="cytoscape-render">`;
            edges.forEach((e) => {
                const s = nodeMap[e.source], t = nodeMap[e.target];
                if (!s || !t) return;
                const mx = (s.x + t.x) / 2, my = (s.y + t.y) / 2;
                const dx = t.x - s.x, dy = t.y - s.y;
                svg += `<path d="M ${s.x} ${s.y} Q ${(mx - dy * 0.18).toFixed(1)} ${(my + dx * 0.18).toFixed(1)} ${t.x} ${t.y}" fill="none" stroke="#C084FC" stroke-width="1.8" stroke-opacity="0.45"/>`;
                if (e.label) {
                    svg += `<text x="${mx}" y="${my - 8}" text-anchor="middle" font-size="11" fill="#94A3B8">${esc(e.label)}</text>`;
                }
            });
            nodes.forEach((n, i) => {
                const p = positions[i];
                const color = n.color || "#00E5FF";
                const label = esc(n.label || n.id || "node");
                svg += `<circle cx="${p.x}" cy="${p.y}" r="34" fill="#0D1424" stroke="${color}" stroke-width="2.5"/>`;
                svg += `<circle cx="${p.x}" cy="${p.y}" r="11" fill="${color}"/>`;
                const tw = Math.ceil(label.length * 7.2);
                svg += `<rect x="${p.x - (tw / 2 + 12)}" y="${p.y + 44}" width="${tw + 24}" height="24" rx="6" fill="rgba(8,14,26,0.9)" stroke="rgba(30,42,63,0.9)"/>`;
                svg += `<text x="${p.x}" y="${p.y + 61}" text-anchor="middle" font-size="13" font-family="system-ui, sans-serif" fill="#F1F5F9">${label}</text>`;
            });
            svg += "</svg>";

            container.innerHTML = svg;
            pre.replaceWith(container);
        });
    }

    // =====================================================================
    // Diagram enhancement: scrollable panes, toolbars, fullscreen viewer
    // =====================================================================

    enhanceMermaidGraphs() {
        this._mermaidGraphSeq = (this._mermaidGraphSeq || 0);
        this.preview.querySelectorAll('.mermaid, .cytoscape-svg').forEach((node) => {
            const svg = node.querySelector("svg");
            if (!svg || node._enhanced) return;
            node._enhanced = true;
            svg.style.maxWidth = "none";

            this._mermaidGraphSeq += 1;

            // Toolbar: fullscreen, zoom in/out, reset
            const toolbar = document.createElement("div");
            toolbar.className = "mermaid-graph-toolbar";
            const mkBtn = (label, title) => {
                const b = document.createElement("button");
                b.className = "mg-btn";
                b.textContent = label;
                b.title = title;
                return b;
            };
            const btnFull = mkBtn("⛶", "Open diagram in fullscreen viewer (zoom, pan, separate window)");
            const btnIn = mkBtn("+", "Zoom in");
            const btnOut = mkBtn("−", "Zoom out");
            const btnReset = mkBtn("⟲", "Reset to 100%");
            const zoomLabel = document.createElement("span");
            zoomLabel.className = "mg-zoom-label";
            zoomLabel.textContent = "100%";
            btnFull.addEventListener("click", (e) => { e.stopPropagation(); this.openGraphViewer(card); });
            btnIn.addEventListener("click", (e) => { e.stopPropagation(); this.zoomGraphWrap(card, 1.25); });
            btnOut.addEventListener("click", (e) => { e.stopPropagation(); this.zoomGraphWrap(card, 1 / 1.25); });
            btnReset.addEventListener("click", (e) => { e.stopPropagation(); this.resetGraphZoom(card); });
            [btnFull, btnOut, btnIn, btnReset].forEach(b => toolbar.appendChild(b));
            toolbar.appendChild(zoomLabel);

            // Scrollable pane that pans & zooms
            const wrap = document.createElement("div");
            wrap.className = "mermaid-graph-wrap";
            const inner = document.createElement("div");
            inner.className = "mermaid-graph-inner";

            const card = document.createElement("div");
            card.className = "mermaid-graph-card";
            card.dataset.graphId = `g${this._mermaidGraphSeq}`;
            card._inner = inner;
            card._wrap = wrap;
            card._zoom = 1;

            // Swap the diagram node out with a placeholder, assemble the card,
            // then put the card in the diagram's old spot
            const placeholder = document.createElement("div");
            node.replaceWith(placeholder);
            inner.appendChild(node);
            card.appendChild(toolbar);
            card.appendChild(wrap);
            wrap.appendChild(inner);

            this.initGraphPanZoom(card);
            placeholder.replaceWith(card);
        });
    }

    getGraphBaseSize(card) {
        // Measure natural (1x) content size (transition disabled for accuracy)
        const inner = card._inner;
        const prevZoom = inner.style.zoom;
        const prevTrans = inner.style.transition;
        inner.style.transition = "none";
        inner.style.zoom = "1";
        const rect = inner.getBoundingClientRect();
        const dims = { w: rect.width, h: rect.height };
        inner.style.zoom = prevZoom;
        inner.style.transition = prevTrans;
        return dims;
    }

    zoomGraphWrap(card, factor, cx, cy, scroller) {
        const wrap = scroller || card._wrap;
        const k0 = card._zoom || 1;
        let k1 = Math.min(8, Math.max(0.15, k0 * factor));
        // Snap toward 1 to avoid sticky decimal zooms
        if (Math.abs(k1 - 1) < 0.02) k1 = 1;

        if (cx === undefined) { cx = wrap.clientWidth / 2; cy = wrap.clientHeight / 2; }
        const anchorX = (wrap.scrollLeft + cx) / k0;
        const anchorY = (wrap.scrollTop + cy) / k0;

        card._zoom = k1;
        card._inner.style.zoom = k1;
        wrap.scrollLeft = anchorX * k1 - cx;
        wrap.scrollTop = anchorY * k1 - cy;
        this.updateGraphZoomLabel(card);
    }

    updateGraphZoomLabel(card) {
        const text = `${Math.round((card._zoom || 1) * 100)}%`;
        const label = card && card.querySelector(".mg-zoom-label");
        if (label) label.textContent = text;
        const gv = document.getElementById("gv-zoom-label");
        if (gv && this.gvCard === card) gv.textContent = text;
    }

    resetGraphZoom(card, scroller) {
        card._zoom = 1;
        card._inner.style.zoom = 1;
        const wrap = scroller || card._wrap;
        wrap.scrollLeft = 0;
        wrap.scrollTop = 0;
        this.updateGraphZoomLabel(card);
    }

    fitGraphToWrap(card, scroller) {
        const wrap = scroller || card._wrap;
        const base = this.getGraphBaseSize(card);
        if (!base.w) return;
        const k = Math.min(1.8, Math.min(
            wrap.clientWidth / base.w,
            (wrap.clientHeight || window.innerHeight * 0.7) / base.h
        ));
        this.zoomGraphWrap(card, k / (card._zoom || 1), undefined, undefined, wrap);
        wrap.scrollLeft = 0;
        wrap.scrollTop = 0;
    }

    initGraphPanZoom(card) {
        const wrap = card._wrap;

        // Wheel zoom at pointer position
        wrap.addEventListener("wheel", (e) => {
            e.preventDefault();
            const rect = wrap.getBoundingClientRect();
            const cx = e.clientX - rect.left;
            const cy = e.clientY - rect.top;
            const factor = e.deltaY < 0 ? 1.15 : 1 / 1.15;
            this.zoomGraphWrap(card, factor, cx, cy);
        }, { passive: false });

        // Drag to pan — move handled by ONE shared pair of window listeners
        // (this._panTarget), not N pairs (one per card).
        wrap.addEventListener("mousedown", (e) => {
            if (e.button !== 0 || e.target.closest(".mg-btn")) return;
            this._panTarget = {
                wrap,
                active: true,
                startX: e.clientX, startY: e.clientY,
                sl0: wrap.scrollLeft, st0: wrap.scrollTop
            };
            wrap.classList.add("panning");
            e.preventDefault();
        });
    }

    initSharedPanPump() {
        window.addEventListener("mousemove", (e) => {
            const pan = this._panTarget;
            if (!pan) return;
            pan.wrap.scrollLeft = pan.sl0 - (e.clientX - pan.startX);
            pan.wrap.scrollTop = pan.st0 - (e.clientY - pan.startY);
        });
        window.addEventListener("mouseup", () => {
            const pan = this._panTarget;
            if (pan) {
                pan.wrap.classList.remove("panning");
                this._panTarget = null;
            }
        });
    }

    // ---------------------------------------------------------------------
    // Real LaTeX compilation (`latex blocks -> PDF) via the launcher's
    // engine endpoint (tectonic, opt-in install). Cards reuse the diagram
    // viewer plumbing, so fullscreen/zoom/pan come for free.
    // ---------------------------------------------------------------------

    initLatexEngineStatus() {
        fetch("api/latex/status")
            .then(r => r.json())
            .then(d => {
                this.latexEngine = d;
                this.retryLatexCards();
            })
            .catch(() => { this.latexEngine = { engine: null, available: false }; });
    }

    hashString(str) {
        let h = 5381;
        for (let i = 0; i < str.length; i++) h = ((h << 5) + h + str.charCodeAt(i)) | 0;
        return (h >>> 0).toString(36);
    }

    replaceLatexBlocks() {
        const codeNodes = this.preview.querySelectorAll("pre code.language-latex, pre code.language-tex");
        if (!codeNodes.length) return;
        codeNodes.forEach((node) => {
            const pre = node.parentElement;
            if (pre._latexDone) return;
            pre._latexDone = true;
            const source = node.textContent;
            const card = this.buildLatexCard(source);
            pre.replaceWith(card);

            const hash = this.hashString(source);
            const cached = this._latexCache.get(hash);
            if (cached && cached.id) {
                this.showLatexPdf(card, cached.id);
            } else if (cached && cached.error === "NO_ENGINE") {
                this.showLatexInstallCard(card);
            } else if (this.latexEngine === undefined) {
                card._statusEl.textContent = "Checking LaTeX engine…";
                this._pendingLatexCards.push(card);
            } else if (this.latexEngine.available) {
                this.latexCompileCard(card, source);
            } else {
                this.showLatexInstallCard(card);
            }
        });
    }

    retryLatexCards() {
        const cards = this._pendingLatexCards;
        this._pendingLatexCards = [];
        cards.forEach(card => {
            if (this.latexEngine.available) this.latexCompileCard(card, card._source);
            else this.showLatexInstallCard(card);
        });
    }

    buildLatexCard(source) {
        // Same generic card anatomy as diagram cards => viewer/zoom/pan work
        const card = document.createElement("div");
        card.className = "mermaid-graph-card latex-card";
        card._source = source;

        const toolbar = document.createElement("div");
        toolbar.className = "mermaid-graph-toolbar";
        const mkBtn = (label, title) => {
            const b = document.createElement("button");
            b.className = "mg-btn";
            b.textContent = label;
            b.title = title;
            return b;
        };
        const btnFull = mkBtn("⛶", "Open compiled LaTeX in fullscreen");
        const btnIn = mkBtn("+", "Zoom in");
        const btnOut = mkBtn("−", "Zoom out");
        const btnCompile = mkBtn("⟳", "Recompile LaTeX");
        btnCompile.classList.add("lx-compile");
        btnCompile.addEventListener("click", (e) => {
            e.stopPropagation();
            this.latexCompileCard(card, card._source, true);
        });
        btnFull.addEventListener("click", (e) => { e.stopPropagation(); this.openGraphViewer(card); });
        btnIn.addEventListener("click", (e) => { e.stopPropagation(); this.zoomGraphWrap(card, 1.25); });
        btnOut.addEventListener("click", (e) => { e.stopPropagation(); this.zoomGraphWrap(card, 1 / 1.25); });
        const label = document.createElement("span");
        label.className = "mg-zoom-label";
        label.textContent = "100%";
        const status = document.createElement("span");
        status.className = "lx-status";
        status.textContent = "idle";
        card._statusEl = status;

        toolbar.appendChild(btnCompile);
        toolbar.appendChild(status);
        const spacer = document.createElement("span");
        spacer.style.cssText = "flex:1";
        toolbar.appendChild(spacer);
        [btnFull, btnOut, btnIn].forEach(b => toolbar.appendChild(b));
        toolbar.appendChild(label);

        const wrap = document.createElement("div");
        wrap.className = "mermaid-graph-wrap";
        const inner = document.createElement("div");
        inner.className = "mermaid-graph-inner latex-inner";
        const stage = document.createElement("div");
        stage.className = "latex-stage";
        stage.textContent = source.trim().split("\n")[0].slice(0, 40) + " …";
        inner.appendChild(stage);

        card._inner = inner;
        card._wrap = wrap;
        card._zoom = 1;
        card.appendChild(toolbar);
        wrap.appendChild(inner);
        card.appendChild(wrap);
        this.initGraphPanZoom(card);
        return card;
    }

    latexCompileCard(card, source, force = false) {
        const hash = this.hashString(source);
        const cached = this._latexCache.get(hash);
        if (cached && cached.id && !force) {
            this.showLatexPdf(card, cached.id);
            return;
        }
        if (!this.latexEngineAvailable()) {
            this.showLatexInstallCard(card);
            return;
        }
        if (card._compiling) {
            card._recompileAfter = source; // newer content arrived; compile again after
            return;
        }
        card._compiling = true;
        card._statusEl.textContent = "⟳ compiling LaTeX…";

        const run = async () => {
            try {
                const res = await fetch("api/latex/compile", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ source })
                });
                const data = await res.json();
                if (data.ok && data.id) {
                    this._latexCache.set(hash, { id: data.id });
                    this.showLatexPdf(card, data.id);
                } else if ((data.error || "").includes("NO_ENGINE")) {
                    card._statusEl.textContent = "no engine";
                    this.showLatexInstallCard(card);
                } else {
                    card._statusEl.textContent = "compile error";
                    const stage = card.querySelector(".latex-stage");
                    const logPre = document.createElement("pre");
                    logPre.className = "latex-log";
                    logPre.textContent = (data.error || "unknown error").slice(-3000);
                    stage.innerHTML = "";
                    stage.appendChild(logPre);
                }
            } catch (err) {
                card._statusEl.textContent = "server unreachable";
            } finally {
                card._compiling = false;
                if (card._recompileAfter) {
                    const next = card._recompileAfter;
                    card._recompileAfter = null;
                    this.latexCompileCard(card, next);
                }
            }
        };
        run();
    }

    latexEngineAvailable() {
        return !!this.latexEngine && this.latexEngine.available === true;
    }

    showLatexPdf(card, id) {
        card._statusEl.textContent = "compiled";
        const stage = card.querySelector(".latex-stage");
        if (!stage) return;
        const frame = document.createElement("iframe");
        frame.className = "latex-pdf-frame";
        frame.title = "LaTeX output PDF";
        frame.loading = "lazy";
        frame.src = `api/latex/pdf?id=${id}`;
        stage.innerHTML = "";
        stage.appendChild(frame);
    }

    showLatexInstallCard(card) {
        card._statusEl.textContent = "no engine";
        const stage = card.querySelector(".latex-stage");
        if (!stage) return;
        stage.innerHTML = "";
        const info = document.createElement("div");
        info.className = "latex-install";
        const p = document.createElement("div");
        p.className = "latex-install-text";
        p.innerHTML = "📄 <strong>Real LaTeX render</strong> needs an engine.<br>Tectonic is a single 14 MB binary; it fetches CTAN packages <em>(siunitx, TikZ, …)</em> on demand during the first compile.<br><span style='color: var(--text-muted); font-size: 11px;'>Install is manual — nothing runs until you click.</span>";
        const btn = document.createElement("button");
        btn.className = "mg-btn lx-install-btn";
        btn.textContent = "⬇ Install Tectonic engine";
        btn.addEventListener("click", async (e) => {
            e.stopPropagation();
            btn.disabled = true;
            btn.textContent = "⬇ Downloading… (14 MB)";
            card._statusEl.textContent = "installing engine…";
            try {
                const res = await fetch("api/latex/install");
                const data = await res.json();
                if (data.ok) {
                    this.latexEngine = { engine: "tectonic", available: true, installable: true };
                    this._latexCache.clear();
                    card._statusEl.textContent = "engine ready";
                    btn.textContent = "✓ Engine installed — compiling…";
                    this.latexCompileCard(card, card._source);
                } else {
                    btn.disabled = false;
                    btn.textContent = "↻ Retry install";
                    card._statusEl.textContent = "install failed";
                    const err = document.createElement("div");
                    err.className = "latex-install-text";
                    err.style.color = "#F87171";
                    err.textContent = data.error || "unknown install error";
                    info.appendChild(err);
                }
            } catch (err) {
                btn.disabled = false;
                btn.textContent = "↻ Retry install";
                card._statusEl.textContent = "server unreachable";
            }
        });
        info.appendChild(p);
        info.appendChild(btn);
        stage.appendChild(info);
    }


    initGraphViewer() {
        this.graphViewer = document.getElementById("graph-viewer");
        this.gvContent = document.getElementById("graph-viewer-content");
        this.gvTitle = document.getElementById("gv-title");
        this.gvZoomLabel = document.getElementById("gv-zoom-label");

        document.getElementById("gv-close").addEventListener("click", () => this.closeGraphViewer());
        document.getElementById("gv-zoom-in").addEventListener("click", () => this.zoomGraphWrap(this.gvCard, 1.25, undefined, undefined, this.gvContent));
        document.getElementById("gv-zoom-out").addEventListener("click", () => this.zoomGraphWrap(this.gvCard, 1 / 1.25, undefined, undefined, this.gvContent));
        document.getElementById("gv-fit").addEventListener("click", () => this.fitGraphToWrap(this.gvCard, this.gvContent));
        document.getElementById("gv-100").addEventListener("click", () => this.resetGraphZoom(this.gvCard, this.gvContent));
        document.getElementById("gv-popout").addEventListener("click", () => this.popOutGraph());

        this.graphViewer.addEventListener("mousedown", (e) => {
            if (e.target === this.graphViewer) this.closeGraphViewer();
        });

        // Wheel zoom at pointer + drag to pan on the shared pump
        this.gvContent.addEventListener("wheel", (e) => {
            if (this.gvCard) {
                e.preventDefault();
                const rect = this.gvContent.getBoundingClientRect();
                const factor = e.deltaY < 0 ? 1.15 : 1 / 1.15;
                this.zoomGraphWrap(this.gvCard, factor, e.clientX - rect.left, e.clientY - rect.top, this.gvContent);
            }
        }, { passive: false });

        this.gvContent.addEventListener("mousedown", (e) => {
            if (e.button !== 0) return;
            e.preventDefault();
            this._panTarget = {
                wrap: this.gvContent,
                active: true,
                startX: e.clientX, startY: e.clientY,
                sl0: this.gvContent.scrollLeft, st0: this.gvContent.scrollTop
            };
        });

        // Wheel zoom shortcuts while the viewer is open
        window.addEventListener("keydown", (e) => {
            if (this.graphViewer.classList.contains("hidden") || !this.gvCard) return;
            if (e.key === "+" || e.key === "=") this.zoomGraphWrap(this.gvCard, 1.25, undefined, undefined, this.gvContent);
            if (e.key === "-") this.zoomGraphWrap(this.gvCard, 1 / 1.25, undefined, undefined, this.gvContent);
            if (e.key === "0") this.fitGraphToWrap(this.gvCard, this.gvContent);
        });
    }

    openGraphViewer(card, titleText) {
        this.gvCard = card;
        this._graphDetached = card._inner;

        // Title from nearest preceding heading in preview, if any
        let title = titleText;
        if (!title) {
            const heading = card.previousElementSibling;
            title = "Diagram";
            let probe = card;
            for (let hops = 0; hops < 3 && probe.previousElementSibling; hops++) {
                probe = probe.previousElementSibling;
                const h = probe.querySelector && (probe.matches?.("h1,h2,h3,h4") ? probe : probe.querySelector("h1,h2,h3,h4"));
                if (h) { title = h.textContent.trim(); break; }
            }
        }
        this.gvTitle.textContent = title;

        // Move (not clone) the SVG so ids/markers stay unique while open
        this.gvContent.appendChild(this._graphDetached);
        this.graphViewer.classList.remove("hidden");

        // Fit nicely: show the whole diagram, then allow zoom
        requestAnimationFrame(() => {
            this.fitGraphToWrap(this.gvCard, this.gvContent);
            this.zoomGraphWrap(this.gvCard, 1.25, undefined, undefined, this.gvContent);
            this.fitGraphToWrap(this.gvCard, this.gvContent);
        });
    }

    closeGraphViewer() {
        if (!this.graphViewer.classList.contains("hidden")) {
            this.graphViewer.classList.add("hidden");
            const inner = this.gvContent.firstElementChild;
            if (inner) {
                // If the card was destroyed by a re-render, discard quietly
                if (this.gvCard && this.gvCard.isConnected && this.gvCard._wrap) {
                    this.gvCard._wrap.appendChild(inner);
                    inner.style.zoom = this.gvCard._zoom || 1;
                } else {
                    inner.remove();
                }
            }
            this.gvCard = null;
        }
    }

    popOutGraph(titleText) {
        const svg = this.gvContent.querySelector("svg");
        if (!svg) return;
        const title = this.gvTitle.textContent || "Diagram";
        const svgHtml = svg.outerHTML;
        const headText = titleText || title;

        const page = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>HyperMark — ${headText}</title>
<style>
    :root { --bg: #070B13; --bord: #1E2A3F; --cyan: #00E5FF; --sec: #94A3B8; }
    html, body { margin:0; height:100%; background:var(--bg); color:#F1F5F9;
                 font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
                 display:flex; flex-direction:column; overflow:hidden; }
    #bar { display:flex; align-items:center; gap:10px; padding:10px 14px;
           background:rgba(13,21,38,.94); border-bottom:1px solid var(--bord); }
    #bar .t { font-weight:800; font-size:14px; color:var(--cyan); margin-right:6px; }
    button { background:rgba(30,42,63,.5); border:1px solid var(--bord); color:#F1F5F9;
             border-radius:8px; padding:4px 12px; cursor:pointer; font-size:13px; }
    button:hover { border-color:var(--cyan); }
    #pct { font-size:12px; color:var(--sec); min-width:44px; text-align:center; font-family: monospace; }
    #wrap { flex:1; overflow:auto; cursor:grab; }
    #wrap.panning { cursor:grabbing; }
    #inner { padding:28px; transform-origin: 0 0; transition: zoom .16s ease; }
    #inner svg { display:block; }
</style>
</head>
<body>
<div id="bar">
    <span class="t">🕸 ${headText}</span>
    <button onclick="z(1.25)">+</button>
    <span id="pct">100%</span>
    <button onclick="z(1/1.25)">−</button>
    <button onclick="fit()">Fit</button>
    <button onclick="one()">1:1</button>
    <span style="color:var(--sec);font-size:11px;margin-left:auto;">drag + wheel to pan/zoom</span>
</div>
<div id="wrap"><div id="inner">${svgHtml}</div></div>
<script>
    let k = 1;
    function z(f, cx, cy) {
        const wrap = document.getElementById("wrap"), inner = document.getElementById("inner");
        if (cx === undefined) { cx = wrap.clientWidth/2; cy = wrap.clientHeight/2; }
        const ax = (wrap.scrollLeft + cx)/k, ay = (wrap.scrollTop + cy)/k;
        k = Math.min(8, Math.max(0.15, k*f));
        inner.style.zoom = k;
        wrap.scrollLeft = ax*k - cx; wrap.scrollTop = ay*k - cy;
        document.getElementById("pct").textContent = Math.round(k*100) + "%";
    }
    function fit() {
        const wrap = document.getElementById("wrap"), inner = document.getElementById("inner");
        inner.style.zoom = 1;
        const r = inner.getBoundingClientRect();
        k = Math.min(wrap.clientWidth/r.width, wrap.clientHeight/r.height);
        inner.style.zoom = k;
        wrap.scrollLeft = 0; wrap.scrollTop = 0;
        document.getElementById("pct").textContent = Math.round(k*100) + "%";
    }
    function one() { k = 1; document.getElementById("inner").style.zoom = 1; wrapToTop(); document.getElementById("pct").textContent = "100%"; }
    function wrapToTop(){ const w=document.getElementById("wrap"); w.scrollLeft=0; w.scrollTop=0; }
    document.getElementById("wrap").addEventListener("wheel", (e) => {
        e.preventDefault();
        const r = e.currentTarget.getBoundingClientRect();
        z(e.deltaY < 0 ? 1.15 : 1/1.15, e.clientX - r.left, e.clientY - r.top);
    }, { passive:false });
    let pan=false, sx=0, sy=0, sl=0, st=0;
    const wrap = document.getElementById("wrap");
    wrap.addEventListener("mousedown", (e)=>{ pan=true; sx=e.clientX; sy=e.clientY; sl=wrap.scrollLeft; st=wrap.scrollTop; wrap.classList.add("panning"); });
    window.addEventListener("mousemove", (e)=>{ if(pan){ wrap.scrollLeft = sl-(e.clientX-sx); wrap.scrollTop = st-(e.clientY-sy); }});
    window.addEventListener("mouseup", ()=>{ pan=false; wrap.classList.remove("panning"); });
    window.addEventListener("keydown", (e)=>{ if(e.key==="Escape") window.close(); if(e.key==="+"||e.key==="=") z(1.25); if(e.key==="-") z(1/1.25); });
    document.getElementById("inner").style.zoom = 1;
</script>
</body>
</html>`;

        const blob = new Blob([page], { type: "text/html" });
        const blobUrl = URL.createObjectURL(blob);
        const win = window.open(blobUrl, "_blank", "width=1250,height=830");
        if (win) setTimeout(() => URL.revokeObjectURL(blobUrl), 15000);
    }

    initMermaid() {
        if (window.mermaid) {
            mermaid.initialize({
                startOnLoad: false,
                theme: 'dark',
                // Natural SVG sizing: no max-width squeeze, so big hierarchy
                // graphs stay readable and can be scrolled / zoomed instead.
                flowchart: { useMaxWidth: false, padding: 14, nodeSpacing: 55, rankSpacing: 65 },
                sequence: { useMaxWidth: false, width: 150 },
                gantt: { useMaxWidth: false },
                pie: { useMaxWidth: false },
                er: { useMaxWidth: false },
                class: { useMaxWidth: false },
                state: { useMaxWidth: false },
                themeVariables: {
                    primaryColor: '#16233B',
                    primaryTextColor: '#00E5FF',
                    primaryBorderColor: '#00E5FF',
                    lineColor: '#C084FC',
                    fontSize: '16px',
                    fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, sans-serif',
                    secondaryColor: '#0D1526',
                    tertiaryColor: '#1E293B'
                }
            });
        }
    }

    updateTelemetry() {
        const text = this.editor.value;
        const chars = text.length;
        const words = text.trim() ? text.trim().split(/\s+/).length : 0;
        const lines = text ? text.split("\n").length : 1;
        const readMin = Math.max(1, Math.round(words / 200));

        // Math count uses the same code-span-aware extraction as the renderer
        const mathReg = /(`+[\s\S]*?`+)|(\$\$[\s\S]+?\$\$)|(?<!\$)\$([^$\n]+?)\$(?!\$)/g;
        let mathCount = 0;
        let mm;
        while ((mm = mathReg.exec(text))) {
            if (mm[1]) continue; // code span/fence
            mathCount++;
        }

        const diagMatches = text.match(/\`\`\`(mermaid|cytoscape)/g);
        const diagCount = diagMatches ? diagMatches.length : 0;

        this.statLines.textContent = lines;
        this.statWords.textContent = words;
        this.statChars.textContent = chars;
        this.statRead.textContent = `~${readMin}m`;
        this.statLatex.textContent = `${mathCount} eq`;
        this.statDiagrams.textContent = `${diagCount} diag`;
        this.statDocs.textContent = this.documents.length;
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
                this.deleteDocWithUndo(doc);
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
        if (this.statDocs) this.statDocs.textContent = this.documents.length;
    }

    // Create a document, assign a vault filename, persist, and return it.
    async createDoc(title, content, preferredName = "") {
        const doc = {
            id: null,
            fileName: null,
            title: title,
            isPinned: false,
            updatedAt: Date.now(),
            content: content
        };
        if (this.vaultApi) {
            doc.fileName = this.uniqueVaultFileName((preferredName && /\.(md|markdown|txt)$/i.test(preferredName))
                ? preferredName : title + ".md");
            try {
                await this.vaultWrite(doc.fileName, content);
                this.setVaultStatus("SYNCED");
            } catch (e) { this.setVaultStatus("ERROR"); }
            doc.id = "vault_" + doc.fileName.replace(/\W+/g, "_");
        } else {
            doc.id = "doc_" + Date.now();
        }
        this.documents.unshift(doc);
        this.saveDocuments();
        return doc;
    }

    // Toast notifications (non-blocking; optional Undo action)
    initToast() {
        this.toastEl = document.getElementById("toast");
        if (!this.toastEl) return;
        this.toastMsg = this.toastEl.querySelector(".toast-msg");
        this.toastAction = this.toastEl.querySelector(".toast-action");
        this.toastAction.addEventListener("click", () => {
            const fn = this._toastFn;
            this.hideToast();
            if (fn) fn();
        });
    }

    showToast(msg, actionLabel = null, actionFn = null, ms = 5000) {
        if (!this.toastEl) { return; }
        this.toastMsg.textContent = msg;
        if (actionLabel && actionFn) {
            this._toastFn = actionFn;
            this.toastAction.textContent = actionLabel;
            this.toastAction.classList.remove("hidden");
        } else {
            this._toastFn = null;
            this.toastAction.classList.add("hidden");
        }
        this.toastEl.classList.remove("hidden");
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => this.hideToast(), ms);
    }

    hideToast() {
        clearTimeout(this._toastTimer);
        this._toastFn = null;
        if (this.toastEl) this.toastEl.classList.add("hidden");
    }

    deleteDocWithUndo(doc) {
        // Keep at least one note so the app always has an active document
        if (this.documents.length <= 1) {
            this.showToast("Keep at least one note — create a new one first.");
            return;
        }
        const idx = this.documents.findIndex(d => d.id === doc.id);
        if (idx === -1) return;
        const hadFile = doc.fileName;
        // Cancel any pending vault write for that file (else it resurrects)
        const pending = this._vaultSaveTimers.get(doc.fileName);
        if (pending) { clearTimeout(pending); this._vaultSaveTimers.delete(doc.fileName); }
        this.documents.splice(idx, 1);
        if (this.activeDocId === doc.id) {
            this.activeDocId = (this.documents[idx] || this.documents[this.documents.length - 1]).id;
        }
        this.saveDocuments(true);
        this.renderSidebar();
        this.loadActiveDocument();
        if (hadFile) this.vaultDelete(hadFile).catch(() => {});
        this.showToast(`Deleted "${doc.title}"`, "Undo", async () => {
            this.documents.splice(Math.min(idx, this.documents.length), 0, doc);
            this.activeDocId = doc.id;
            this.saveDocuments(true);
            this.renderSidebar();
            this.loadActiveDocument();
            if (hadFile) {
                try { await this.vaultWrite(hadFile, doc.content); this.setVaultStatus("SYNCED"); }
                catch (e) { this.setVaultStatus("ERROR"); }
            }
        }, 8000);
    }

    // Enter at the end of "- item" / "- [x] task" / "1. step" continues the list
    handleListContinuation(e) {
        const pos = this.editor.selectionStart;
        if (pos !== this.editor.selectionEnd) return;
        const value = this.editor.value;
        const lineStart = value.lastIndexOf("\n", pos - 1) + 1;
        const lineEndNl = value.indexOf("\n", pos);
        const lineEnd = lineEndNl === -1 ? value.length : lineEndNl;
        // Match the WHOLE line, so a mid-line caret after the marker with
        // content behind it never triggers the "empty item exits list" path
        const line = value.slice(lineStart, lineEnd);
        const m = line.match(/^(\s*)(?:([-*+])\s(\[[ xX]\]\s)?|(\d+)(\.)\s)(.*)$/);
        if (!m) return;
        const [, indent, bullet, box, num, dot, rest] = m;
        // Empty item (caret at its end): pressing Enter removes the marker
        if (!rest.trim() && pos === lineEnd) {
            e.preventDefault();
            const markerLen = line.length - indent.length; // the "- [ ] " part
            this.editor.value = value.slice(0, pos - markerLen) + value.slice(pos);
            this.editor.selectionStart = this.editor.selectionEnd = pos - markerLen;
            this.handleEditorInput();
            return;
        }
        e.preventDefault();
        let marker;
        if (bullet) marker = `${indent}${bullet} ${box ? "[ ] " : ""}`;
        else marker = `${indent}${parseInt(num, 10) + 1}${dot} `;
        this.insertAtCursor("\n" + marker);
    }


    initRadialWheel() {
        this.radialModal = document.getElementById("radial-modal");
        this.radialSvg = document.getElementById("radial-svg");
        this.radialHub = document.getElementById("radial-hub");
        this.radialHubTitle = document.getElementById("radial-hub-title");
        this.radialHubSub = document.getElementById("radial-hub-sub");
        this.radialActiveCategory = null;

        document.getElementById("btn-radial-dial").addEventListener("click", () => this.openRadialWheel());

        // Right-click (desktop analog of the mobile long-press trigger)
        const editorWrap = document.querySelector(".editor-wrapper");
        if (editorWrap) {
            editorWrap.addEventListener("contextmenu", (e) => {
                e.preventDefault();
                this.openRadialWheel(e.clientX, e.clientY);
            });
        }

        // Click on the dark backdrop dismisses
        this.radialModal.addEventListener("mousedown", (e) => {
            if (e.target === this.radialModal) this.closeRadialWheel();
        });

        this.buildRadialWheel();
    }

    getRadialCategories() {
        return [
            {
                title: "Headings", icon: "🅷", accent: "#38BDF8",
                subOptions: [
                    { label: "H1", desc: "Level 1 Heading (# )", badge: "H1", accent: "#00E5FF", insert: ["# ", ""] },
                    { label: "H2", desc: "Level 2 Heading (## )", badge: "H2", accent: "#38BDF8", insert: ["## ", ""] },
                    { label: "H3", desc: "Level 3 Heading (### )", badge: "H3", accent: "#818CF8", insert: ["### ", ""] },
                    { label: "H4", desc: "Level 4 Heading (#### )", badge: "H4", accent: "#A78BFA", insert: ["#### ", ""] },
                    { label: "H5", desc: "Level 5 Heading (##### )", badge: "H5", accent: "#C084FC", insert: ["##### ", ""] },
                    { label: "H6", desc: "Level 6 Heading (###### )", badge: "H6", accent: "#E879F9", insert: ["###### ", ""] }
                ]
            },
            {
                title: "Format", icon: "✏️", accent: "#00E5FF",
                subOptions: [
                    { label: "Bold", desc: "Bold weight (**text**)", badge: "B", accent: "#00E5FF", insert: ["**", "**"] },
                    { label: "Italic", desc: "Italic slant (*text*)", badge: "I", accent: "#38BDF8", insert: ["*", "*"] },
                    { label: "Strike", desc: "Strikethrough (~~text~~)", badge: "S", accent: "#F472B6", insert: ["~~", "~~"] },
                    { label: "Highlight", desc: "Neon highlight (==text==)", badge: "H", accent: "#FEF08A", insert: ["==", "=="] },
                    { label: "Quote", desc: "Blockquote (> )", icon: "❝", accent: "#FB7185", insert: ["> ", ""] },
                    { label: "Underline", desc: "HTML underline (<u>)", badge: "U", accent: "#FCD34D", insert: ["<u>", "</u>"] }
                ]
            },
            {
                title: "Math & Code", icon: "∑", accent: "#C084FC",
                subOptions: [
                    { label: "Inline Code", desc: "Inline monospace (code)", icon: "`", accent: "#34D399", insert: ["`", "`"] },
                    { label: "Code Block", desc: "Fenced code block", icon: "{}", accent: "#10B981", insert: ["```\n", "\n```"] },
                    { label: "$ Math", desc: "Inline LaTeX formula ($x$)", badge: "$x$", accent: "#C084FC", insert: ["$", "$"] },
                    { label: "$$ Display", desc: "Display LaTeX block", badge: "$$", accent: "#A855F7", insert: ["$$\n", "\n$$"] },
                    { label: "CTAN", desc: "Insert LaTeX package header", icon: "📦", accent: "#EC4899", insertText: "\\usepackage{amsmath, amssymb, graphicx}\n\\usepackage{hyperref}\n\n" },
                    { label: "Diagram", desc: "Insert Mermaid diagram", icon: "🕸️", accent: "#8B5CF6", insertText: "\n```mermaid\nflowchart TD\n    A[Start] --> B{Decision}\n    B -->|Yes| C[OK]\n    B -->|No| D[Retry]\n    D --> B\n```\n\n" }
                ]
            },
            {
                title: "Lists & Tables", icon: "☑️", accent: "#4ADE80",
                subOptions: [
                    { label: "Task", desc: "Checklist item (- [ ] )", icon: "✔", accent: "#FBBF24", insert: ["- [ ] ", ""] },
                    { label: "Bullets", desc: "Unordered list (- )", icon: "•", accent: "#34D399", insert: ["- ", ""] },
                    { label: "Numbered", desc: "Ordered list (1. )", badge: "1.", accent: "#2DD4BF", insert: ["1. ", ""] },
                    { label: "Table", desc: "Markdown grid table", icon: "▦", accent: "#4ADE80", insertText: "| Col 1 | Col 2 |\n| --- | --- |\n| Val 1 | Val 2 |\n" },
                    { label: "Callout", desc: "Obsidian / GitHub alert note", icon: "💡", accent: "#38BDF8", insert: ["> [!NOTE]\n> ", ""] },
                    { label: "Divider", desc: "Horizontal rule (---)", badge: "—", accent: "#94A3B8", insertText: "\n---\n" }
                ]
            },
            {
                title: "Insert", icon: "🔗", accent: "#FBBF24",
                subOptions: [
                    { label: "Wiki Link", desc: "Internal wiki link ([[ ]])", icon: "🔗", accent: "#818CF8", insert: ["[[", "]]"] },
                    { label: "Web Link", desc: "Markdown hyperlink ([ ]( ))", icon: "🌐", accent: "#60A5FA", insert: ["[", "](url)"] },
                    { label: "Image", desc: "Embedded image (![alt]( ))", icon: "🖼️", accent: "#F59E0B", insert: ["![Alt text](", ")"] },
                    { label: "Emoji", desc: "Unicode symbols palette", icon: "🙂", accent: "#FBBF24", action: () => this.openEmojiPopover() },
                    { label: "Syntax Guide", desc: "Open Markdown cheat sheet", icon: "📖", accent: "#E879F9", action: () => this.openSyntaxGuide() }
                ]
            },
            {
                title: "Packages", icon: "📦", accent: "#EC4899",
                subOptions: [
                    { label: "Browse All", desc: "Open the package & extension browser", icon: "🗂️", accent: "#EC4899", action: () => this.openPackageBrowser() },
                    { label: "siunitx", desc: "SI units & scientific notation", icon: "SI", accent: "#38BDF8", insertText: "\n```latex\n\\usepackage{siunitx}\n\\begin{document}\nAvogadro: \\SI{6.022e23}{\\per\\mole}\nDistance: \\qty{3.14}{\\km}\n\\end{document}\n```\n\n" },
                    { label: "mhchem", desc: "Chemical equations & formulas", icon: "⚗", accent: "#34D399", insertText: "\n```latex\n\\usepackage[version=4]{mhchem}\n\\begin{document}\n\\ce{2H2 + O2 -> 2H2O}\n\\end{document}\n```\n\n" },
                    { label: "physics", desc: "Dirac notation, operators, vectors", icon: "φ", accent: "#C084FC", insertText: "\n```latex\n\\usepackage{physics}\n\\begin{document}\n\\braket{\\psi | \\hat{H} | \\phi}\n\\end{document}\n```\n\n" },
                    { label: "TikZ", desc: "Programmatic vector drawings", icon: "📐", accent: "#F59E0B", insertText: "\n```latex\n\\usepackage{tikz}\n\\begin{document}\n\\begin{tikzpicture}\n\\draw (0,0) circle (1.5cm);\n\\end{tikzpicture}\n\\end{document}\n```\n\n" },
                    { label: "LaTeX Block", desc: "Compile any ```latex block to PDF", icon: "📄", accent: "#F472B6", insertText: "\n```latex\n\\begin{document}\nEnergy: $E = mc^{2}$\n\\end{document}\n```\n\n" }
                ]
            },
            {
                title: "Vault", icon: "💾", accent: "#06B6D4",
                subOptions: [
                    { label: "Documents", desc: "Toggle documents sidebar", icon: "📁", accent: "#06B6D4", action: () => this.sidebar.classList.toggle("collapsed") },
                    { label: "New Doc", desc: "Create fresh markdown document", icon: "➕", accent: "#00E5FF", action: () => { document.getElementById("btn-new-doc").click(); } },
                    { label: "Find", desc: "Find & Replace bar", icon: "🔍", accent: "#FB923C", action: () => { document.getElementById("btn-toggle-find").click(); } },
                    { label: "Open .md", desc: "Open file from local disk", icon: "📂", accent: "#10B981", action: () => { this.filePicker.click(); } },
                    { label: "Rename", desc: "Rename active document", icon: "✏️", accent: "#38BDF8", action: () => { this.titleInput.focus(); this.titleInput.select(); } }
                ]
            }
        ];
    }

    buildRadialWheel() {
        // Wheel geometry (600x600 viewBox, same proportions as the mobile wheel)
        this.RC = {
            cx: 300, cy: 300,
            primaryInner: 74, primaryOuter: 168,
            subInner: 178, subOuter: 266,
            gap: 3, subGap: 2.5, fanSpan: 110, pop: 7
        };
        this.renderRadialPrimary();
    }

    polar(r, deg) {
        const rad = deg * Math.PI / 180;
        return [this.RC.cx + r * Math.cos(rad), this.RC.cy + r * Math.sin(rad)];
    }

    wedgePath(degStart, degEnd, innerR, outerR, pop) {
        const { cx, cy } = this.RC;
        const mid = (degStart + degEnd) / 2;
        const d2r = (d) => d * Math.PI / 180;
        const shiftX = (pop || 0) * Math.cos(d2r(mid));
        const shiftY = (pop || 0) * Math.sin(d2r(mid));
        const ox = cx + shiftX, oy = cy + shiftY;

        const x1 = ox + innerR * Math.cos(d2r(degStart)), y1 = oy + innerR * Math.sin(d2r(degStart));
        const x2 = ox + outerR * Math.cos(d2r(degStart)), y2 = oy + outerR * Math.sin(d2r(degStart));
        const x3 = ox + outerR * Math.cos(d2r(degEnd)), y3 = oy + outerR * Math.sin(d2r(degEnd));
        const x4 = ox + innerR * Math.cos(d2r(degEnd)), y4 = oy + innerR * Math.sin(d2r(degEnd));
        const rf = (v) => Math.round(v * 100) / 100;
        return `M ${rf(x1)} ${rf(y1)} L ${rf(x2)} ${rf(y2)} A ${outerR} ${outerR} 0 0 1 ${rf(x3)} ${rf(y3)} L ${rf(x4)} ${rf(y4)} A ${innerR} ${innerR} 0 0 0 ${rf(x1)} ${rf(y1)} Z`;
    }

    makeSvgEl(tag) {
        return document.createElementNS("http://www.w3.org/2000/svg", tag);
    }

    setRadialHub(cat, sub) {
        if (sub) {
            this.radialHubTitle.textContent = sub.label.toUpperCase();
            this.radialHubTitle.style.color = sub.accent;
            this.radialHubSub.textContent = sub.desc;
            this.radialHub.style.borderColor = sub.accent;
        } else if (cat) {
            this.radialHubTitle.textContent = cat.title.toUpperCase();
            this.radialHubTitle.style.color = cat.accent;
            this.radialHubSub.textContent = `${cat.subOptions.length} OPTIONS`;
            this.radialHub.style.borderColor = cat.accent;
        } else {
            this.radialHubTitle.textContent = "HYPERMARK";
            this.radialHubTitle.style.color = "#00E5FF";
            this.radialHubSub.textContent = "HOVER & PICK";
            this.radialHub.style.borderColor = "#00E5FF";
        }
    }

    renderRadialPrimary() {
        const svg = this.radialSvg;
        svg.innerHTML = "";
        const cats = this.getRadialCategories();
        this.radialCategories = cats;
        const arc = 360 / cats.length;
        const iconR = (this.RC.primaryInner + this.RC.primaryOuter) / 2;

        cats.forEach((cat, i) => {
            const start = i * arc - 90 + this.RC.gap / 2;
            const end = start + arc - this.RC.gap;
            const g = this.makeSvgEl("g");
            g.setAttribute("class", "wedge-group");
            g.style.cursor = "pointer";

            const path = this.makeSvgEl("path");
            path.setAttribute("d", this.wedgePath(start, end, this.RC.primaryInner, this.RC.primaryOuter, 0));
            path.setAttribute("fill", "#0A1220");
            path.setAttribute("stroke", "rgba(56, 189, 248, 0.2)");
            path.setAttribute("stroke-width", "1.3");
            g.appendChild(path);
            g._path = path;

            const mid = (start + end) / 2;
            const [ix, iy] = this.polar(iconR, mid);
            const iconEl = this.makeSvgEl("text");
            iconEl.setAttribute("x", ix);
            iconEl.setAttribute("y", iy - 2);
            iconEl.setAttribute("text-anchor", "middle");
            iconEl.setAttribute("dominant-baseline", "middle");
            iconEl.setAttribute("font-size", "18");
            iconEl.setAttribute("fill", "#E2E8F0");
            iconEl.textContent = cat.icon;
            g.appendChild(iconEl);

            const labelEl = this.makeSvgEl("text");
            labelEl.setAttribute("x", ix);
            labelEl.setAttribute("y", iy + 16);
            labelEl.setAttribute("text-anchor", "middle");
            labelEl.setAttribute("font-size", "11");
            labelEl.setAttribute("font-weight", "700");
            labelEl.setAttribute("fill", "#E2E8F0");
            labelEl.textContent = cat.title;
            g.appendChild(labelEl);
            g._icon = iconEl;
            g._label = labelEl;

            const activate = () => {
                this.radialActiveCategory = i;
                cats.forEach((c, j) => this.highlightPrimaryWedge(j, j === i));
                this.renderRadialSubs(i);
                this.setRadialHub(cat, null);
            };
            g.addEventListener("mouseenter", activate);
            g.addEventListener("click", activate);

            svg.appendChild(g);
        });

        this.setRadialHub(null, null);
    }

    highlightPrimaryWedge(idx, active) {
        const g = this.radialSvg.children[idx];
        if (!g || !g._path) return;
        const cat = this.radialCategories[idx];
        const arc = 360 / this.radialCategories.length;
        const start = idx * arc - 90 + this.RC.gap / 2;
        const end = start + arc - this.RC.gap;
        if (active) {
            g._path.setAttribute("d", this.wedgePath(start, end, this.RC.primaryInner, this.RC.primaryOuter, this.RC.pop));
            g._path.setAttribute("fill", cat.accent);
            g._path.setAttribute("fill-opacity", "0.22");
            g._path.setAttribute("stroke", cat.accent);
            g._path.setAttribute("stroke-width", "2.2");
            g._icon.setAttribute("fill", cat.accent);
            g._label.setAttribute("fill", "#FFFFFF");
        } else {
            g._path.setAttribute("d", this.wedgePath(start, end, this.RC.primaryInner, this.RC.primaryOuter, 0));
            g._path.setAttribute("fill", "#0A1220");
            g._path.setAttribute("fill-opacity", "1");
            g._path.setAttribute("stroke", "rgba(56, 189, 248, 0.2)");
            g._path.setAttribute("stroke-width", "1.3");
            g._icon.setAttribute("fill", "#E2E8F0");
            g._label.setAttribute("fill", "#E2E8F0");
        }
    }

    renderRadialSubs(catIdx) {
        const svg = this.radialSvg;
        svg.querySelectorAll(".sub-wedge-group").forEach(el => el.remove());

        const cat = this.radialCategories[catIdx];
        const subCount = cat.subOptions.length;
        const subArc = this.RC.fanSpan / subCount;
        const arc = 360 / this.radialCategories.length;
        const catCenter = catIdx * arc - 90 + (arc / 2);
        const subStartBase = catCenter - this.RC.fanSpan / 2;
        const iconR = (this.RC.subInner + this.RC.subOuter) / 2;

        cat.subOptions.forEach((sub, j) => {
            const start = subStartBase + j * subArc + this.RC.subGap / 2;
            const end = start + subArc - this.RC.subGap;

            const g = this.makeSvgEl("g");
            g.setAttribute("class", "sub-wedge-group");
            g.style.cursor = "pointer";

            const path = this.makeSvgEl("path");
            path.setAttribute("d", this.wedgePath(start, end, this.RC.subInner, this.RC.subOuter, 0));
            path.setAttribute("fill", "#0A1424");
            path.setAttribute("stroke", sub.accent);
            path.setAttribute("stroke-opacity", "0.45");
            path.setAttribute("stroke-width", "1.2");
            g.appendChild(path);
            g._path = path;

            const mid = (start + end) / 2;
            const [ix, iy] = this.polar(iconR, mid);

            const iconEl = this.makeSvgEl("text");
            iconEl.setAttribute("x", ix);
            iconEl.setAttribute("y", iy - 3);
            iconEl.setAttribute("text-anchor", "middle");
            iconEl.setAttribute("dominant-baseline", "middle");
            iconEl.setAttribute("font-size", "12");
            iconEl.setAttribute("font-weight", "800");
            iconEl.setAttribute("fill", sub.accent);
            iconEl.textContent = sub.icon || sub.badge || sub.label;
            g.appendChild(iconEl);

            const labelEl = this.makeSvgEl("text");
            labelEl.setAttribute("x", ix);
            labelEl.setAttribute("y", iy + 13);
            labelEl.setAttribute("text-anchor", "middle");
            labelEl.setAttribute("font-size", "9.5");
            labelEl.setAttribute("font-weight", "600");
            labelEl.setAttribute("fill", "#CBD5E1");
            labelEl.textContent = sub.label;
            g.appendChild(labelEl);
            g._icon = iconEl;
            g._label = labelEl;

            g.addEventListener("mouseenter", () => {
                path.setAttribute("d", this.wedgePath(start, end, this.RC.subInner, this.RC.subOuter, this.RC.pop));
                path.setAttribute("fill", sub.accent);
                path.setAttribute("fill-opacity", "0.35");
                path.setAttribute("stroke", sub.accent);
                path.setAttribute("stroke-opacity", "1");
                path.setAttribute("stroke-width", "2.4");
                iconEl.setAttribute("fill", "#FFFFFF");
                labelEl.setAttribute("fill", "#FFFFFF");
                this.setRadialHub(cat, sub);
            });
            g.addEventListener("mouseleave", () => {
                path.setAttribute("d", this.wedgePath(start, end, this.RC.subInner, this.RC.subOuter, 0));
                path.setAttribute("fill", "#0A1424");
                path.setAttribute("fill-opacity", "1");
                path.setAttribute("stroke-opacity", "0.45");
                path.setAttribute("stroke-width", "1.2");
                iconEl.setAttribute("fill", sub.accent);
                labelEl.setAttribute("fill", "#CBD5E1");
                this.setRadialHub(cat, null);
            });
            g.addEventListener("click", () => {
                if (sub.insert) this.insertFormatting(sub.insert[0], sub.insert[1]);
                else if (sub.insertText) this.insertAtCursor(sub.insertText);
                else if (sub.action) sub.action();
                this.closeRadialWheel();
            });

            svg.appendChild(g);
        });
    }

    openRadialWheel(x, y) {
        const stage = document.getElementById("radial-stage");
        if (x !== undefined) {
            // Center the wheel near the click point, clamped to stay on screen
            const margin = 280;
            const cx = Math.min(Math.max(x, margin), window.innerWidth - margin);
            const cy = Math.min(Math.max(y, margin), window.innerHeight - margin);
            stage.style.position = "fixed";
            stage.style.left = `${cx}px`;
            stage.style.top = `${cy}px`;
            stage.style.transform = "translate(-50%, -50%)";
        } else {
            stage.style.position = "";
            stage.style.left = "";
            stage.style.top = "";
            stage.style.transform = "";
        }
        this.radialActiveCategory = null;
        this.renderRadialPrimary();
        this.radialModal.classList.remove("hidden");
    }

    closeRadialWheel() {
        this.radialModal.classList.add("hidden");
        this.editor.focus();
    }

    // ---------------------------------------------------------------------
    // Package & Extension Browser — every renderer the build supports,
    // searchable, with one-click starter snippets. Opened from the wheel's
    // 📦 Packages wedge (or Ctrl+Shift+B).
    // ---------------------------------------------------------------------

    getPackageCatalog() {
        const E = "ENGINE";   // compiles to PDF via the tectonic engine (opt-in)
        const B = "BUILT-IN"; // renders in preview, fully offline

        return [
            // ---- LaTeX / CTAN packages (real TeX engine compiles these) ----
            { cat: "LaTeX · CTAN", name: "amsmath", status: B + "·engine", desc: "Core math environments & alignment. Preloaded in every ```latex block.", snippet: "$\\int_0^\\infty e^{-x^2}\\,dx = \\frac{\\sqrt{\\pi}}{2}$" },
            { cat: "LaTeX · CTAN", name: "amssymb", status: B + "·engine", desc: "Extra math symbols (blackboard bold, etc.). Preloaded.", snippet: "$\\mathbb{R}^{n} \\oplus \\mathbb{C}^{m}$" },
            { cat: "LaTeX · CTAN", name: "siunitx", status: E, desc: "SI units & scientific notation with auto-formatting. Fetched from CTAN on first use.", snippet: "\\usepackage{siunitx}\n\\SI{6.022e23}{\\per\\mole}\\quad \\qty{3.14}{\\km}\\quad \\unit{km/h}" },
            { cat: "LaTeX · CTAN", name: "mhchem", status: E, desc: "Chemical equations, isotopes, bonds, states.", snippet: "\\usepackage[version=4]{mhchem}\n\\ce{2H2 + O2 -> 2H2O}\\qquad \\ce{SO4^2-}" },
            { cat: "LaTeX · CTAN", name: "physics", status: E, desc: "Dirac bra-ket, differential operators, vector algebra.", snippet: "\\usepackage{physics}\n\\braket{\\psi|\\hat{H}|\\phi}\\qquad \\dv{x}[f]{y}" },
            { cat: "LaTeX · CTAN", name: "TikZ", status: E, desc: "Programmatic vector drawings — diagrams, plots, geometry.", snippet: "\\usepackage{tikz}\n\\begin{tikzpicture}\n\\draw[thick,cyan] (0,0) circle (1.5cm);\n\\end{tikzpicture}" },
            { cat: "LaTeX · CTAN", name: "pgfplots", status: E, desc: "Axis plots & data visualizations on top of TikZ.", snippet: "\\usepackage{pgfplots}\n\\begin{axis}[xlabel=$x$,ylabel=$\\sin x$]\n\\addplot {sin(deg(x))};\n\\end{axis}" },
            { cat: "LaTeX · CTAN", name: "geometry", status: B + "·engine", desc: "Page layout control. Preloaded (A4, 1.8 cm margins).", snippet: "\\usepackage[a4paper, margin=1.8cm]{geometry}" },
            { cat: "LaTeX · CTAN", name: "hyperref", status: B + "·engine", desc: "Clickable links & PDF metadata. Preloaded.", snippet: "\\href{https://example.com}{visit}" },
            { cat: "LaTeX · CTAN", name: "graphicx", status: B + "·engine", desc: "Embed images in compiled PDFs. Preloaded.", snippet: "\\includegraphics[width=8cm]{figure.png}" },
            { cat: "LaTeX · CTAN", name: "xcolor", status: E, desc: "Named/mixed colors for text and TikZ.", snippet: "\\usepackage{xcolor}\n\\textcolor{cyan}{ neon \\textcolor{magenta}{text} }" },
            { cat: "LaTeX · CTAN", name: "booktabs", status: E, desc: "Publication-quality table rules (\\toprule…).", snippet: "\\usepackage{booktabs}\n\\begin{tabular}{ll}\\toprule x & y \\\\ \\bottomrule\\end{tabular}" },
            { cat: "LaTeX · CTAN", name: "listings", status: E, desc: "Syntax-highlighted code listings in PDF.", snippet: "\\usepackage{listings}\n\\lstset{language=Python}\n\\begin{lstlisting}\nprint(\"hi\")\n\\end{lstlisting}" },
            { cat: "LaTeX · CTAN", name: "mathtools", status: E, desc: "Fixes & extensions for amsmath (cases*, matrices).", snippet: "\\usepackage{mathtools}\n\\begin{dcases} x & x>0 \\\\ -x & \\text{else} \\end{dcases}" },
            { cat: "LaTeX · CTAN", name: "cleveref", status: E, desc: "Smart \\cref references that name the target automatically.", snippet: "\\usepackage{cleveref}\n\\label{eq:main} … \\cref{eq:main}" },
            { cat: "LaTeX · CTAN", name: "esvect", status: E, desc: "Elegant vector arrow notation.", snippet: "\\usepackage{esvect}\n\\vv{F} = m\\,\\vv{a}" },

            // ---- Diagram renderers (native, offline) ----
            { cat: "Diagrams", name: "mermaid flowchart", status: B, desc: "Flowcharts with nodes, decisions & edge labels.", snippet: "```mermaid\nflowchart TD\n    A[Start] --> B{Decision}\n    B -->|Yes| C[OK]\n    B -->|No| D[Retry]\n```" },
            { cat: "Diagrams", name: "mermaid sequence", status: B, desc: "Actors exchanging ordered messages.", snippet: "```mermaid\nsequenceDiagram\n    Alice->>Bob: ping\n    Bob-->>Alice: pong\n```" },
            { cat: "Diagrams", name: "mermaid gantt", status: B, desc: "Timeline / project schedule bars.", snippet: "```mermaid\ngantt\n    section Phase\n    Design :a1, 2026-01-01, 7d\n    Build   :after a1, 10d\n```" },
            { cat: "Diagrams", name: "mermaid class", status: B, desc: "UML class boxes with members & relations.", snippet: "```mermaid\nclassDiagram\n    class Note { +title +body save() }\n```" },
            { cat: "Diagrams", name: "mermaid state", status: B, desc: "State-machine transitions.", snippet: "```mermaid\nstateDiagram-v2\n    [*] --> Editing\n    Editing --> Saved\n```" },
            { cat: "Diagrams", name: "mermaid ER", status: B, desc: "Entity-relationship schemas.", snippet: "```mermaid\nerDiagram\n    NOTE ||--o{ TAG : has\n```" },
            { cat: "Diagrams", name: "mermaid pie", status: B, desc: "Quick share-donut charts.", snippet: "```mermaid\npie title Math\n    \"LaTeX\" : 60\n    \"GFM\" : 40\n```" },
            { cat: "Diagrams", name: "mermaid journey", status: B, desc: "User-journey maps with satisfaction scores.", snippet: "```mermaid\njourney\n    Write: 5: Editor\n```" },
            { cat: "Diagrams", name: "cytoscape JSON", status: B, desc: "Custom SVG graph renderer — colored nodes, labeled edges, pan/zoom.", snippet: "```cytoscape\n{\"nodes\":[{\"data\":{\"id\":\"a\",\"label\":\"Alpha\"}},{\"data\":{\"id\":\"b\",\"label\":\"Beta\"}}],\"edges\":[{\"data\":{\"source\":\"a\",\"target\":\"b\"}}]}\n```" },
            { cat: "Diagrams", name: "```latex block", status: E, desc: "Compile any fenced latex block to a live PDF card (fullscreen, zoom, cache).", snippet: "```latex\n\\begin{document}\n\\section{Hello}\n$E = mc^{2}$\n\\end{document}\n```" },

            // ---- Markdown / GFM extensions (preview, offline) ----
            { cat: "Markdown GFM", name: "Tables", status: B, desc: "GitHub-style grid tables.", snippet: "| Col | Val |\n| --- | --- |\n| A | 1 |" },
            { cat: "Markdown GFM", name: "Task lists", status: B, desc: "Checkable checkboxes in preview.", snippet: "- [x] done\n- [ ] todo" },
            { cat: "Markdown GFM", name: "Callouts", status: B, desc: "Obsidian/GitHub alert blocks — NOTE, TIP, WARNING, DANGER, INFO, QUESTION.", snippet: "> [!NOTE]\n> Helpful callout text." },
            { cat: "Markdown GFM", name: "Wiki links", status: B, desc: "Cross-note links that navigate on click.", snippet: "[[Another Note]]" },
            { cat: "Markdown GFM", name: "Inline math $…$", status: B, desc: "KaTeX inline LaTeX — code spans protected.", snippet: "$e^{i\\pi} + 1 = 0$" },
            { cat: "Markdown GFM", name: "Display math $$…$$", status: B, desc: "KaTeX display blocks, centered.", snippet: "$$\n\\int_0^1 f(x)\\,dx\n$$" },
            { cat: "Markdown GFM", name: "Strikethrough", status: B, desc: "GFM ~~deleted~~ text.", snippet: "~~obsolete~~" },
            { cat: "Markdown GFM", name: "Emoji palette", status: B, desc: "Unicode symbol quick-insert strip in the wheel (🙂 Insert).", snippet: "🙂 🧪 ⚛ 📐" }
        ];
    }

    initPackageBrowser() {
        this.packageModal = document.getElementById("package-modal");
        this.packageList = document.getElementById("package-list");
        this.packageSearch = document.getElementById("package-search");
        this.packageChips = document.getElementById("package-cat-chips");
        this._pkgCat = "All";

        document.getElementById("btn-close-package").addEventListener("click", () => this.closePackageBrowser());
        this.packageModal.addEventListener("mousedown", (e) => {
            if (e.target === this.packageModal) this.closePackageBrowser();
        });
        this.packageSearch.addEventListener("input", () => this.renderPackageList());

        // Category chips
        const cats = ["All", ...new Set(this.getPackageCatalog().map(p => p.cat))];
        this.packageChips.innerHTML = "";
        cats.forEach((c, i) => {
            const b = document.createElement("button");
            b.className = "pkg-chip" + (i === 0 ? " active" : "");
            b.textContent = c;
            b.addEventListener("click", () => {
                this._pkgCat = c;
                this.packageChips.querySelectorAll(".pkg-chip").forEach(x => x.classList.remove("active"));
                b.classList.add("active");
                this.renderPackageList();
            });
            this.packageChips.appendChild(b);
        });

        // Ctrl+Shift+B shortcut
        document.addEventListener("keydown", (e) => {
            if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === "b") {
                e.preventDefault();
                this.openPackageBrowser();
            }
        });
    }

    openPackageBrowser() {
        if (!this.packageModal) this.initPackageBrowser();
        this.packageSearch.value = "";
        this.renderPackageList();
        this.packageModal.classList.remove("hidden");
        this.packageSearch.focus();
    }

    closePackageBrowser() {
        this.packageModal.classList.add("hidden");
        this.editor.focus();
    }

    renderPackageList() {
        const q = this.packageSearch.value.toLowerCase();
        const items = this.getPackageCatalog().filter(p =>
            (this._pkgCat === "All" || p.cat === this._pkgCat) &&
            (!q || p.name.toLowerCase().includes(q) || p.desc.toLowerCase().includes(q) || p.cat.toLowerCase().includes(q))
        );

        // Engine status hint on the section banner
        const engineReady = this.latexEngineAvailable();
        this.packageList.innerHTML = "";
        if (!items.length) {
            const empty = document.createElement("div");
            empty.className = "pkg-empty";
            empty.textContent = "No packages match — try another search.";
            this.packageList.appendChild(empty);
            return;
        }

        const note = document.createElement("div");
        note.className = "pkg-note";
        note.innerHTML = engineReady
            ? "⚙️ Tectonic engine <b style='color:var(--accent-emerald)'>installed</b> — ENGINE items compile live. First use of a package fetches it from CTAN."
            : "⚙️ ENGINE items need the LaTeX engine (<b>Browse LaTeX Block → card → Install Tectonic</b>, one click). BUILT-IN items work offline right now.";
        this.packageList.appendChild(note);

        let shownCat = "";
        items.forEach((p) => {
            if (p.cat !== shownCat) {
                shownCat = p.cat;
                const h = document.createElement("div");
                h.className = "pkg-cat-head";
                h.textContent = shownCat;
                this.packageList.appendChild(h);
            }
            const card = document.createElement("div");
            card.className = "pkg-card" + (p.status === "ENGINE" ? " pkg-engine" : "");

            const head = document.createElement("div");
            head.className = "pkg-card-head";
            const nameEl = document.createElement("span");
            nameEl.className = "pkg-name";
            nameEl.textContent = p.name;
            const chip = document.createElement("span");
            chip.className = "pkg-chip-status " + (p.status === "ENGINE" ? "st-engine" : "st-builtin");
            chip.textContent = p.status === "ENGINE" ? "⚙ ENGINE" : "✓ OFFLINE";
            head.appendChild(nameEl);
            head.appendChild(chip);

            const desc = document.createElement("div");
            desc.className = "pkg-desc";
            desc.textContent = p.desc;

            const snip = document.createElement("pre");
            snip.className = "pkg-snippet";
            snip.textContent = p.snippet;

            const row = document.createElement("div");
            row.className = "pkg-actions";
            const btn = document.createElement("button");
            btn.className = "mg-btn pkg-insert";
            btn.textContent = "＋ Insert";
            btn.title = "Insert starter snippet at cursor";
            btn.addEventListener("click", () => {
                this.insertAtCursor("\n" + p.snippet + "\n");
                this.showToast(`＋ Inserted ${p.name} starter snippet`);
                this.closePackageBrowser();
            });
            row.appendChild(btn);
            card.appendChild(head);
            card.appendChild(desc);
            card.appendChild(snip);
            card.appendChild(row);
            this.packageList.appendChild(card);
        });
    }

    // =====================================================================
    // Emoji quick-insert palette
    // =====================================================================

    initEmojiPopover() {
        this.emojiPopover = document.getElementById("emoji-popover");
        const grid = document.getElementById("emoji-grid");
        const emojis = [
            "😀", "😂", "😊", "😍", "🤔", "😎", "😴", "🙃", "🥳", "😢", "😡", "🤯",
            "✅", "☑️", "⚠️", "❌", "❓", "❗", "🔥", "⭐", "💡", "📌", "📎", "🗂️",
            "📈", "📉", "🧪", "⚛️", "🧬", "🔭", "🛠️", "⚙️", "🚀", "🎯", "💾", "❤️",
            "🔗", "🌐", "🖥️", "⌨️", "📝", "📄", "📊", "🗓️", "⏰", "📍", "🧠", "🧭"
        ];
        grid.innerHTML = "";
        emojis.forEach(em => {
            const btn = document.createElement("button");
            btn.textContent = em;
            btn.addEventListener("click", () => {
                this.insertAtCursor(em);
                this.closeEmojiPopover();
            });
            grid.appendChild(btn);
        });
        document.getElementById("btn-close-emoji").addEventListener("click", () => this.closeEmojiPopover());
    }

    openEmojiPopover() {
        this.emojiPopover.classList.remove("hidden");
    }

    closeEmojiPopover() {
        this.emojiPopover.classList.add("hidden");
    }

    openSyntaxGuide() {
        const guide = [
            "# Markdown Syntax Guide (GFM + LaTeX + Mermaid)",
            "",
            "## Headers",
            "# H1", "## H2", "### H3   (up to H6)",
            "",
            "## Emphasis",
            "- Bold: `**text**` -> **text**",
            "- Italic: `*text*` -> *text*",
            "- Strike: `~~text~~` -> ~~text~~",
            "- Highlight: `==text==`",
            "- Underline: `<u>text</u>`",
            "",
            "## Lists",
            "- Unordered: `- item`",
            "- Ordered: `1. item`",
            "- Task: `- [ ] item`",
            "",
            "## Links & Images",
            "- [Title](https://example.com)",
            "- ![Alt text](image-url)",
            "- Wiki link: `[[Document Title]]`",
            "",
            "## Code",
            "Inline: `code`  |  Fenced: ```lang ... ```",
            "",
            "## Math (KaTeX)",
            "Inline: `$e^{i\\pi} + 1 = 0$`",
            "Display: `$$\\int_0^1 f(x)\\,dx$$`",
            "",
            "## Tables",
            "| Col | Val |",
            "| --- | --- |",
            "| A   | 1   |",
            "",
            "## Callouts",
            "> [!NOTE]",
            "> Obsidian-style alert note.",
            "",
            "## Diagrams (Mermaid)",
            "```mermaid",
            "flowchart TD",
            "    A --> B",
            "```",
            "",
            "## Horizontal Rule",
            "---",
            ""
        ].join("\n");
        let doc = this.documents.find(d => d.title === "Markdown Syntax Guide");
        if (!doc) {
            doc = {
                id: "doc_" + Date.now(),
                title: "Markdown Syntax Guide",
                isPinned: false,
                updatedAt: Date.now(),
                content: guide
            };
            this.documents.unshift(doc);
        }
        this.activeDocId = doc.id;
        this.saveDocuments();
        this.renderSidebar();
        this.loadActiveDocument();
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

    insertAtCursor(text) {
        const start = this.editor.selectionStart;
        const end = this.editor.selectionEnd;
        const content = this.editor.value;

        this.editor.value = content.substring(0, start) + text + content.substring(end);
        this.editor.focus();
        this.editor.selectionStart = this.editor.selectionEnd = start + text.length;

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

        // Case-sensitive or folded search — folded uses lastIndexOf over lowercased text
        const text = this.editor.value;
        if (!this.findCaseSensitive) {
            const lower = text.toLowerCase();
            const ql = q.toLowerCase();
            let pos = 0;
            while ((pos = lower.indexOf(ql, pos)) !== -1) {
                this.findMatches.push(pos);
                pos += ql.length;
            }
        } else {
            let pos = 0;
            while ((pos = text.indexOf(q, pos)) !== -1) {
                this.findMatches.push(pos);
                pos += q.length;
            }
        }

        if (this.findMatches.length > 0) {
            this.currentMatchIdx = 0;
            this.highlightMatch(false);
        } else {
            this.findMatchCount.textContent = "0/0";
        }
    }

    highlightMatch(stealFocus = true) {
        if (this.currentMatchIdx >= 0 && this.currentMatchIdx < this.findMatches.length) {
            const pos = this.findMatches[this.currentMatchIdx];
            const q = this.findQuery.value;
            // Only next/prev navigation (and inline calls) may take focus;
            // typing in the find box must NOT be interrupted.
            if (stealFocus) this.editor.focus();
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
        const dpr = window.devicePixelRatio || 1;
        // HiDPI-aware canvas: physical pixels scaled down to CSS pixels
        this.graphCanvas.width = Math.max(1, Math.round(rect.width * dpr));
        this.graphCanvas.height = Math.max(1, Math.round(rect.height * dpr));
        this.graphCanvas.style.width = rect.width + "px";
        this.graphCanvas.style.height = rect.height + "px";
        this.graphCss = { w: rect.width, h: rect.height, dpr: dpr };
    }

    buildGraphNodes() {
        this.nodes = [];
        this.edges = [];

        const centerX = ((this.graphCss && this.graphCss.w) || this.graphCanvas.clientWidth || 900) / 2;
        const centerY = ((this.graphCss && this.graphCss.h) || this.graphCanvas.clientHeight || 640) / 2;

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
        const seenLinkEdges = new Set();
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

            // Cross-document links: explicit [[WikiLink]] first (case-insensitive),
            // then plain title mentions; de-duplicated both directions.
            const linked = new Set();
            const wikiRe = /\[\[([^\]\[]+)\]\]/g;
            let wm;
            while ((wm = wikiRe.exec(doc.content || ""))) {
                const target = this.documents.find(d =>
                    d.id !== doc.id && d.title.toLowerCase() === wm[1].trim().toLowerCase());
                if (target) linked.add(target.id);
            }
            this.documents.forEach(other => {
                if (other.id === doc.id) return;
                const mentions = doc.content.includes(other.title);
                if (linked.has(other.id) || mentions) {
                    const key = [doc.id, other.id].sort().join("→");
                    if (seenLinkEdges.has(key)) return;
                    seenLinkEdges.add(key);
                    this.edges.push({ source: doc.id, target: other.id, color: "#F472B6", isLink: true });
                }
            });
        });

        // 4. Tag Nodes — real #tags harvested from document content
        if (this.showTags) {
            const counts = {};
            const tagRe = /(?:^|[\s(])#([A-Za-z][\w-]{1,28})\b/g;
            this.documents.forEach(doc => {
                const text = doc.content || "";
                let m;
                while ((m = tagRe.exec(text))) {
                    const t = m[1].toLowerCase();
                    counts[t] = (counts[t] || 0) + 1;
                }
            });
            const tags = Object.keys(counts)
                .sort((a, b) => counts[b] - counts[a])
                .slice(0, 10);

            tags.forEach((tag, i) => {
                const arc = Math.PI / Math.max(1, tags.length - 1 || 1);
                const a = i * arc; // fan across the bottom
                const tx = centerX - 300 + Math.abs(Math.cos(a)) * 600 * (i % 2 ? 0.85 : 1);
                const ty = centerY + 260 + (i % 2) * 40;
                const tagNode = { id: "tag_" + tag, title: "#" + tag, type: "tag", x: tx, y: ty, color: "#FBBF24", size: 20 };
                this.nodes.push(tagNode);

                this.documents.forEach(doc => {
                    if (new RegExp(`(?:^|[\\s(])#${tag}\\b`, "i").test(doc.content)) {
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
        const dpr = (this.graphCss && this.graphCss.dpr) || window.devicePixelRatio || 1;
        const width = (this.graphCss && this.graphCss.w) || this.graphCanvas.clientWidth || 900;
        const height = (this.graphCss && this.graphCss.h) || this.graphCanvas.clientHeight || 640;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
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

        // Wheel Zoom — anchored at the pointer, like the diagram viewer
        this.graphCanvas.addEventListener("wheel", (e) => {
            e.preventDefault();
            const rect = this.graphCanvas.getBoundingClientRect();
            const px = e.clientX - rect.left;
            const py = e.clientY - rect.top;
            const zoomFactor = e.deltaY < 0 ? 1.15 : 0.85;
            const s2 = Math.min(3.8, Math.max(0.35, this.graphScale * zoomFactor));
            if (s2 === this.graphScale) return;
            // keep the graph point under the cursor stationary on screen
            this.graphOffset.x = px - (px - this.graphOffset.x) * (s2 / this.graphScale);
            this.graphOffset.y = py - (py - this.graphOffset.y) * (s2 / this.graphScale);
            this.graphScale = s2;
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

        const snippetLine = doc.content.split("\n").find(l => l.trim() && !l.startsWith("#")) || doc.content.substring(0, 80);
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
                        <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">${d.content.length} chars • ${d.content.split(/\s+/).length} words</div>
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
