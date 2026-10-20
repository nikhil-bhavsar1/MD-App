#!/usr/bin/env python3
"""
HyperMark Desktop Launcher for Linux (Fedora 44)
Zero-installation standalone runtime engine and desktop launcher.
"""

import sys
import os
import shutil
import socket
import threading
import webbrowser
import subprocess
import time
from pathlib import Path
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

def get_base_dir():
    # If frozen as an executable or running from AppDir
    if getattr(sys, 'frozen', False):
        return Path(sys.executable).parent
    # If running from AppImage AppDir
    appdir = os.environ.get("APPDIR")
    if appdir:
        candidate = Path(appdir) / "usr" / "share" / "hypermark"
        if candidate.exists():
            return candidate
        return Path(appdir)
    return Path(__file__).resolve().parent / "app"

def get_vault_dir():
    vault = Path.home() / ".local" / "share" / "hypermark" / "vault"
    vault.mkdir(parents=True, exist_ok=True)
    return vault

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('127.0.0.1', 0))
        return s.getsockname()[1]

class HyperMarkHTTPHandler(SimpleHTTPRequestHandler):
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

def launch_desktop_window(url):
    """
    Launches the application in dedicated native app-window mode.
    Probes for common desktop web runtimes available on Fedora 44:
    Chromium/Brave/Edge/Chrome (--app=), Epiphany/GNOME Web (--application-mode),
    or standard desktop xdg-open.
    """
    user_data = f"/tmp/hypermark-profile-{os.getuid()}"

    # 1. Check for Chromium-based browsers (provides borderless native desktop app window)
    chromium_candidates = [
        "chromium",
        "google-chrome",
        "google-chrome-stable",
        "brave-browser",
        "microsoft-edge"
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
                return True
            except Exception:
                continue

    # 2. Check for Epiphany / GNOME Web (standard Fedora GNOME browser)
    epiphany_path = shutil.which("epiphany")
    if epiphany_path:
        try:
            subprocess.Popen([
                epiphany_path,
                f"--application-mode={url}"
            ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return True
        except Exception:
            pass

    # 3. Check for Firefox
    firefox_path = shutil.which("firefox")
    if firefox_path:
        try:
            subprocess.Popen([
                firefox_path,
                "--new-window",
                url
            ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return True
        except Exception:
            pass

    # 4. Fallback to standard xdg-open / system browser
    try:
        webbrowser.open(url)
        return True
    except Exception as e:
        print(f"Error launching browser: {e}")
        return False

def main():
    # Ensure local vault directory exists on Fedora
    vault_dir = get_vault_dir()

    port = find_free_port()
    server = ThreadingHTTPServer(('127.0.0.1', port), HyperMarkHTTPHandler)

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    url = f"http://127.0.0.1:{port}/index.html"
    print(f"======================================================")
    print(f"  HyperMark — Cyberpunk Markdown Studio for Fedora 44")
    print(f"  Local Vault: {vault_dir}")
    print(f"  App URL:     {url}")
    print(f"======================================================")

    # Launch desktop application window
    launch_desktop_window(url)

    try:
        # Keep process alive while user is editing
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down HyperMark...")
        server.shutdown()
        sys.exit(0)

if __name__ == "__main__":
    main()
