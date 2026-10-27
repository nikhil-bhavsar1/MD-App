#!/usr/bin/env bash
# HyperMark — Single-click run for Linux.
#
# No installation and no dependency commands required: the app is plain
# HTML/JS served locally by this launcher using only the Python 3 standard
# library (python3 is preinstalled on Fedora, Ubuntu, Debian, Arch, openSUSE...).
#
# Usage:
#   ./run.sh                    # or double-click it in your file manager
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# If the repo is new, make the launcher executable (harmless if already set)
chmod +x "$DIR/linux/src/hypermark-launcher.py" 2>/dev/null || true

if ! command -v python3 >/dev/null 2>&1; then
    echo "HyperMark needs 'python3'. Install it with:" >&2
    echo "  Fedora/Red Hat : sudo dnf install python3" >&2
    echo "  Ubuntu/Debian  : sudo apt install python3" >&2
    echo "  Arch           : sudo pacman -S python" >&2
    exit 1
fi

exec python3 "$DIR/linux/src/hypermark-launcher.py" "$@"

