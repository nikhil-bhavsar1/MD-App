#!/usr/bin/env python3
"""
HyperMark Desktop Launcher for Linux.
Serves the app locally and shows it in a REAL native application window
(no browser chrome), using the first available window engine:

  1. Bundled/fetched Electron runtime  -> native window (like VS Code)
  2. WebKitGTK (if present)            -> native GTK window
  3. Browser app-window fallback       -> Chromium --app / GNOME Web / Firefox

Zero installation: the only hard requirement is python3.
"""

import sys
import os
import json
import re
import shutil
import hashlib
import socket
import threading
import urllib.request
import webbrowser
import subprocess
import time
import urllib.parse
from pathlib import Path
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


def get_base_dir():
    """Resolve the directory containing index.html, in every run mode."""
    # Running from a self-extracting AppImage (AppRun exports APPDIR)
    appdir = os.environ.get("APPDIR")
    if appdir:
        candidate = Path(appdir) / "usr" / "share" / "hypermark"
        if (candidate / "index.html").exists():
            return candidate

    here = Path(__file__).resolve().parent
    # Source checkout: linux/src/hypermark-launcher.py  ->  linux/src/app
    if (here / "app" / "index.html").exists():
        return here / "app"
    # RPM/system install: /usr/bin/hypermark -> /usr/share/hypermark
    if (here.parent / "share" / "hypermark" / "index.html").exists():
        return here.parent / "share" / "hypermark"
    # Repo root convenience: ./run.sh + app next to the launcher tree
    entry = Path(sys.argv[0]).resolve().parent
    for candidate in (entry / "linux" / "src" / "app", entry / "app"):
        if (candidate / "index.html").exists():
            return candidate
    raise SystemExit("HyperMark: could not locate application assets (index.html).")


def get_vault_dir():
    vault = Path.home() / ".local" / "share" / "hypermark" / "vault"
    vault.mkdir(parents=True, exist_ok=True)
    return vault


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('127.0.0.1', 0))
        return s.getsockname()[1]


# ---------------------------------------------------------------------------
# LaTeX engine support (opt-in, on-demand): compile ```latex blocks to PDF.
# Prefers tectonic (single binary, auto-fetches CTAN packages), falls back
# to any installed pdflatex/xelatex. Nothing is installed automatically.
# ---------------------------------------------------------------------------

TECTONIC_VERSION = "0.15.0"
TECTONIC_URL = ("https://github.com/tectonic-typesetting/tectonic/releases/download/"
                f"tectonic%40{TECTONIC_VERSION}/"
                f"tectonic-{TECTONIC_VERSION}-x86_64-unknown-linux-musl.tar.gz")

LATEX_TEMPLATE = """\\documentclass[11pt]{article}
\\usepackage{amsmath, amssymb, graphicx, hyperref}
\\usepackage[a4paper, margin=1.8cm]{geometry}
\\pagestyle{empty}

\\begin{document}
%(body)s
\\end{document}
"""

LATEX_PREAMBLE = """\\documentclass[11pt]{article}
\\usepackage{amsmath, amssymb, graphicx, hyperref}
\\usepackage[a4paper, margin=1.8cm]{geometry}
\\pagestyle{empty}

"""

_LATEX_LOCK = threading.Lock()


def latex_bin_dir():
    d = Path.home() / ".local" / "share" / "hypermark" / "bin"
    d.mkdir(parents=True, exist_ok=True)
    return d


def find_tex_engine():
    """Return {'engine': 'tectonic'|'pdflatex'|'xelatex', 'path': str} or None."""
    candidates = [str(latex_bin_dir() / "tectonic")] + \
                 [shutil.which(n) for n in ("tectonic", "pdflatex", "xelatex")]
    for path in candidates:
        if path and os.path.isfile(path) and os.access(path, os.X_OK):
            name = os.path.basename(path)
            if "tectonic" in name.lower():
                return {"engine": "tectonic", "path": path}
            return {"engine": name, "path": path}
    return None


def latex_cache_dir():
    d = Path.home() / ".cache" / "hypermark" / "latex"
    d.mkdir(parents=True, exist_ok=True)
    return d


def latex_cache_get(key):
    pdf = latex_cache_dir() / f"{key}.pdf"
    if pdf.is_file():
        return pdf.read_bytes()
    return None


def latex_cache_put(key, data):
    try:
        (latex_cache_dir() / f"{key}.pdf").write_bytes(data)
    except OSError:
        pass


def wrap_latex_template(source):
    if "\\documentclass" in source:
        return source          # full document, compile as written
    if "\\begin{document}" in source:
        return LATEX_PREAMBLE + source  # body only; add preamble, keep its own \begin{document}
    return LATEX_TEMPLATE % {"body": source}  # bare math/text body; full wrap


def run_tex_engine(engine, tex_path, out_dir, timeout=240):
    """Compile tex_path; return (pdf_bytes | None, log_tail | None)."""
    env = dict(os.environ)
    if engine["engine"] == "tectonic":
        cmd = [engine["path"], tex_path]
    elif engine["engine"] == "xelatex":
        cmd = [engine["path"], "-interaction=nonstopmode", "-halt-on-error",
               f"-output-directory={out_dir}", tex_path]
    else:
        cmd = [engine["path"], "-interaction=nonstopmode", "-halt-on-error",
               f"-output-directory={out_dir}", tex_path]
    try:
        proc = subprocess.run(cmd, cwd=str(out_dir), env=env, timeout=timeout,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        log = (proc.stdout or b"").decode("utf-8", "replace")
    except subprocess.TimeoutExpired:
        return None, "compile timed out"
    except FileNotFoundError:
        return None, f"engine binary not runnable: {engine['path']}"

    stem = Path(tex_path).stem
    pdf = Path(out_dir) / f"{stem}.pdf"
    if proc.returncode == 0 and pdf.is_file():
        return pdf.read_bytes(), None
    tail = "\n".join(log.splitlines()[-25:])
    if not tail.strip():
        tail = f"exit code {proc.returncode}"
    return None, tail


def latex_compile(source):
    """Compile source text; returns (id | None, log_tail | None)."""
    engine = find_tex_engine()
    if not engine:
        return None, "NO_ENGINE"

    key = None
    if engine["engine"] == "tectonic":
        key = hashlib.sha256(source.encode("utf-8")).hexdigest()[:24]
        cached = latex_cache_get(key)
        if cached is not None:
            return key, None

    import tempfile
    with _LATEX_LOCK:
        with tempfile.TemporaryDirectory(prefix="hypermark-tex-") as tmp:
            tex = Path(tmp) / "main.tex"
            tex.write_text(wrap_latex_template(source), encoding="utf-8")
            data, log = run_tex_engine(engine, tex, tmp)
    if data is None:
        return None, log
    if key and not latex_cache_get(key):
        latex_cache_put(key, data)
    return key, None


def latex_install(progress_cb=None):
    """Download Tectonic into the app bin dir (user-triggered, needs network)."""
    bin_dir = latex_bin_dir()
    target = bin_dir / "tectonic"
    if target.is_file() and os.access(str(target), os.X_OK):
        return {"ok": True, "message": "already installed"}
    tmp = target.with_suffix(".download")
    try:
        with urllib.request.urlopen(TECTONIC_URL, timeout=120) as resp, \
                open(str(tmp) + ".tgz", "wb") as out:
            while True:
                chunk = resp.read(65536)
                if not chunk:
                    break
                out.write(chunk)
    except Exception as e:
        try:
            Path(str(tmp) + ".tgz").unlink()
        except OSError:
            pass
        return {"ok": False, "error": f"download failed: {e}"}
    try:
        subprocess.run(["tar", "-xzf", str(tmp) + ".tgz", "-C", str(bin_dir)],
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        os.chmod(str(target), 0o755)
        Path(str(tmp) + ".tgz").unlink(missing_ok=True)
    except Exception as e:
        try:
            Path(str(tmp) + ".tgz").unlink(missing_ok=True)
        except OSError:
            pass
        return {"ok": False, "error": f"extract failed: {e}"}
    found = find_tex_engine()
    if not found or found["engine"] != "tectonic":
        return {"ok": False, "error": "installed but tectonic not runnable"}
    return {"ok": True, "message": f"tectonic {TECTONIC_VERSION} installed"}


class HyperMarkHTTPHandler(SimpleHTTPRequestHandler):
    VAULT_EXTS = ('.md', '.markdown', '.txt')

    def __init__(self, *args, **kwargs):
        self.base_dir = get_base_dir()
        super().__init__(*args, directory=str(self.base_dir), **kwargs)

    def log_message(self, format, *args):
        # Suppress noisy HTTP request logging in terminal
        pass

    def end_headers(self):
        # Disable caching for live desktop app edits
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()

    # ---------------- Local Vault API (real .md files on disk) ------------

    def safe_vault_name(self, name):
        """Reject path traversal / unsafe names; allow plain md/txt filenames."""
        base = os.path.basename(str(name or '').strip()).strip()
        if not base or base.startswith('.'):
            return None
        if base.lower().endswith(self.VAULT_EXTS):
            return base
        return None

    def do_GET(self):
        path = urllib.parse.urlparse(self.path).path
        if path == '/api/vault/list':
            vault = get_vault_dir()
            files = []
            for f in sorted(vault.iterdir()):
                if f.is_file() and f.suffix.lower() in self.VAULT_EXTS:
                    try:
                        st = f.stat()
                        files.append({'name': f.name, 'mtime': int(st.st_mtime * 1000),
                                      'size': st.st_size})
                    except OSError:
                        continue
            self._json({'files': files})
            return
        if path == '/api/vault/read':
            qs = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            name = self.safe_vault_name((qs.get('name') or [''])[0])
            if not name:
                self._json({'error': 'bad name'}, 400)
                return
            target = get_vault_dir() / name
            if not target.is_file():
                self._json({'error': 'not found'}, 404)
                return
            try:
                data = target.read_text(encoding='utf-8', errors='replace')
            except OSError as e:
                self._json({'error': str(e)}, 500)
                return
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.send_header('X-Vault-Name', name)
            self.end_headers()
            self.wfile.write(data.encode('utf-8'))
            return
        if path == '/api/latex/status':
            engine = find_tex_engine()
            self._json({'engine': engine['engine'] if engine else None,
                        'available': bool(engine),
                        'installable': True})
            return
        if path == '/api/latex/install':
            try:
                result = latex_install()
            except Exception as e:
                result = {'ok': False, 'error': str(e)}
            if result.get('ok'):
                self._json({'ok': True, 'message': result.get('message', '')})
            else:
                self._json({'ok': False, 'error': result.get('error', 'unknown')}, 500)
            return
        if path == '/api/latex/pdf':
            qs = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            tex_id = (qs.get('id') or [''])[0]
            if not re.fullmatch(r'[0-9a-f]{24}', tex_id):
                self._json({'error': 'bad id'}, 400)
                return
            data = latex_cache_get(tex_id)
            if data is None:
                self._json({'error': 'not found'}, 404)
                return
            self.send_response(200)
            self.send_header('Content-Type', 'application/pdf')
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        super().do_GET()

    def do_POST(self):
        path = urllib.parse.urlparse(self.path).path
        if path in ('/api/vault/write', '/api/vault/delete'):
            try:
                length = int(self.headers.get('Content-Length', 0) or 0)
                body = json.loads(self.rfile.read(length).decode('utf-8') or '{}')
            except Exception:
                self._json({'error': 'bad json'}, 400)
                return
            name = self.safe_vault_name(body.get('name'))
            if not name:
                self._json({'error': 'bad name'}, 400)
                return
            vault = get_vault_dir()
            target = vault / name
            if path == '/api/vault/write':
                content = str(body.get('content', ''))
                try:
                    tmp = target.with_suffix(target.suffix + '.tmp')
                    tmp.write_text(content, encoding='utf-8')
                    os.replace(str(tmp), str(target))
                except OSError as e:
                    self._json({'error': str(e)}, 500)
                    return
                self._json({'ok': True})
            elif path == '/api/vault/delete':
                try:
                    if target.is_file():
                        target.unlink()
                except OSError as e:
                    self._json({'error': str(e)}, 500)
                    return
                self._json({'ok': True})
            return
        if path == '/api/latex/compile':
            try:
                length = int(self.headers.get('Content-Length', 0) or 0)
                body = json.loads(self.rfile.read(length).decode('utf-8') or '{}')
            except Exception:
                self._json({'error': 'bad json'}, 400)
                return
            source = str(body.get('source', ''))
            if not source.strip():
                self._json({'ok': False, 'error': 'empty source'})
                return
            tex_id, log = latex_compile(source)
            if tex_id is None:
                self._json({'ok': False,
                            'error': 'NO_ENGINE' if log == 'NO_ENGINE' else (log or 'compile failed')})
            else:
                self._json({'ok': True, 'id': tex_id})
            return
        self._json({'error': 'not found'}, 404)

    def _json(self, obj, code=200):
        data = json.dumps(obj).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


# ---------------------------------------------------------------------------
# Window engine 1: bundled Electron runtime (native window, no browser chrome)
# ---------------------------------------------------------------------------

def find_electron():
    """Return [electron_binary, main_js] if a usable runtime is found."""
    here = Path(__file__).resolve().parent
    main_candidates = [here / "main-electron.js"]   # linux/src/main-electron.js (dev) or bundled copy

    explicit = os.environ.get("HYPERMARK_ELECTRON")
    binary_candidates = []
    if explicit:
        binary_candidates.append(Path(explicit))
    appdir = os.environ.get("APPDIR")
    if appdir:
        binary_candidates.append(Path(appdir) / "usr" / "share" / "hypermark" / "electron" / "electron")
        main_candidates.insert(0, Path(appdir) / "usr" / "share" / "hypermark" / "main-electron.js")
    binary_candidates += [
        here / "electron" / "electron",                                  # bundled next to launcher
        here.parent / ".cache" / "electron-current" / "electron",        # linux/.cache (dev runtime)
        Path("/usr/share/hypermark/electron/electron"),                  # RPM install
    ]
    main_candidates.insert(1, here / "app" / "main-electron.js")
    main_candidates.insert(2, Path("/usr/share/hypermark/main-electron.js"))   # RPM install
    explicit_main = os.environ.get("HYPERMARK_ELECTRON_MAIN")
    if explicit_main:
        main_candidates.insert(0, Path(explicit_main))

    binary = None
    for candidate in binary_candidates:
        if candidate.exists() and os.access(str(candidate), os.X_OK):
            binary = candidate
            break
    if binary is None:
        return None

    for main_js in main_candidates:
        if main_js.exists():
            return [str(binary), str(main_js)]
    return None


def launch_electron_native(cmd, url):
    """Launch the Electron native window. Blocks until the window closes.

    Returns False quickly if the engine crashed on startup (with sandbox
    issues on some setups), letting the caller fall back to a browser."""
    started = time.time()
    proc = subprocess.Popen(cmd + [url], cwd=str(Path(cmd[0]).parent),
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print("  Window engine: native (Electron)")
    try:
        returncode = proc.wait()
    except KeyboardInterrupt:
        proc.terminate()
        print("\nShutting down HyperMark...")
        sys.exit(0)

    if returncode not in (0, None) and (time.time() - started) < 6:
        print("  Electron failed to start; falling back to browser window.")
        return False
    return returncode in (0, None)


# ---------------------------------------------------------------------------
# Window engine 2: WebKitGTK native GTK window (if the engine is installed)
# ---------------------------------------------------------------------------

def run_webkit_native(url):
    try:
        import gi
        gi.require_version('Gtk', '3.0')
        gi.require_version('WebKit2', '4.1')
        from gi.repository import Gtk
        from gi.repository import WebKit2
        win = Gtk.Window(title="HyperMark")
        win.set_default_size(1280, 830)
        try:
            win.set_icon_name("hypermark")
        except Exception:
            pass
        view = WebKit2.WebView()
        view.load_uri(url)
        win.add(view)
        win.connect("destroy", Gtk.main_quit)
        win.show_all()
        print("  Window engine: native (WebKitGTK)")
        Gtk.main()
        return True
    except Exception:
        pass
    try:
        import gi
        gi.require_version('Gtk', '4.0')
        gi.require_version('WebKit', '6.0')
        from gi.repository import Gtk
        from gi.repository import WebKit
        win = Gtk.Window(title="HyperMark")
        win.set_default_size(1280, 830)
        view = WebKit.WebView()
        view.load_uri(url)
        win.set_child(view)
        win.connect("close-request", Gtk.main_quit)
        win.show()
        print("  Window engine: native (WebKitGTK 6)")
        Gtk.main()
        return True
    except Exception:
        return False


# ---------------------------------------------------------------------------
# Window engine 3: browser application-window fallback
# ---------------------------------------------------------------------------

def launch_desktop_window(url):
    """Launches the application in a dedicated browser app-window."""
    user_data = f"/tmp/hypermark-profile-{os.getuid()}"

    # 1. Chromium-based browsers (borderless native desktop app window)
    chromium_candidates = [
        "chromium", "google-chrome", "google-chrome-stable",
        "brave-browser", "microsoft-edge"
    ]
    for binary in chromium_candidates:
        path = shutil.which(binary)
        if path:
            try:
                subprocess.Popen([
                    path,
                    f"--app={url}",
                    f"--user-data-dir={user_data}",
                    "--disable-plugins",
                    "--window-size=1280,820"
                ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                print("  Window engine: browser app-window (Chromium-based)")
                return True
            except Exception:
                continue

    # 2. Epiphany / GNOME Web (standard Fedora GNOME browser)
    epiphany_path = shutil.which("epiphany")
    if epiphany_path:
        try:
            subprocess.Popen([
                epiphany_path,
                f"--application-mode={url}"
            ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print("  Window engine: browser app-window (GNOME Web)")
            return True
        except Exception:
            pass

    # 3. Firefox
    firefox_path = shutil.which("firefox")
    if firefox_path:
        try:
            subprocess.Popen([
                firefox_path,
                "--new-window",
                url
            ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print("  Window engine: browser app-window (Firefox)")
            return True
        except Exception:
            pass

    # 4. Fallback to standard xdg-open / system browser
    try:
        webbrowser.open(url)
        print("  Window engine: system default browser")
        return True
    except Exception as e:
        print(f"Error launching browser: {e}")
        return False


def run_server():
    port = find_free_port()
    server = ThreadingHTTPServer(('127.0.0.1', port), HyperMarkHTTPHandler)
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    return server, f"http://127.0.0.1:{port}/index.html"


def main():
    vault_dir = get_vault_dir()
    server, url = run_server()

    print(f"======================================================")
    print(f"  HyperMark — Cyberpunk Markdown Studio for Linux")
    print(f"  Local Vault: {vault_dir}")
    print(f"  App URL:     {url}")
    print(f"======================================================")
    print("  Press Ctrl+C to quit.")

    if os.environ.get("HYPERMARK_NO_BROWSER") == "1":
        print("  (headless mode: no window opened)")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nShutting down HyperMark...")
            server.shutdown()
        return

    # Native window first, then browser fallback.
    # HYPERMARK_WINDOW=browser|native|auto overrides window engine selection.
    window_mode = os.environ.get("HYPERMARK_WINDOW", "auto")
    electron_cmd = find_electron() if window_mode != "browser" else None
    if electron_cmd:
        if launch_electron_native(electron_cmd, url):
            print("Shutting down HyperMark...")
            server.shutdown()
            return
    elif window_mode == "auto" and run_webkit_native(url):
        print("Shutting down HyperMark...")
        server.shutdown()
        return

    launch_desktop_window(url)
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down HyperMark...")
        server.shutdown()
        sys.exit(0)


if __name__ == "__main__":
    main()
