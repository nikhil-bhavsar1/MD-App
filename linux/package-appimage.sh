#!/bin/bash
# HyperMark — Builds the standalone click-to-run executable ("AppImage").
#
# Output: linux/dist/HyperMark-x86_64.AppImage
#   * Single file, zero installation, no root needed
#   * Downloads nothing at runtime, works fully offline
#   * Only system requirement: python3 (preinstalled on virtually every distro)
#
# Run again after editing linux/src to refresh the payload.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${DIR}/src"
APP_DIR="${DIR}/AppDir"
DIST_DIR="${DIR}/dist"
VERSION="$(sed -n 's/^Version:[[:space:]]*//p' "${DIR}/rpmbuild/SPECS/hypermark.spec" | head -1)"
OUTPUT_APPIMAGE="${DIST_DIR}/HyperMark-${VERSION}-x86_64.AppImage"

mkdir -p "${DIST_DIR}"

# ---------------------------------------------------------------------------
# 1. Sync linux/src -> AppDir staging tree
# ---------------------------------------------------------------------------
rm -rf "${APP_DIR}"
mkdir -p "${APP_DIR}/usr/bin" "${APP_DIR}/usr/share/hypermark"

cp -r "${SRC_DIR}/app/." "${APP_DIR}/usr/share/hypermark/"
find "${APP_DIR}" -name "__pycache__" -type d -prune -exec rm -rf {} +
install -m 755 "${SRC_DIR}/hypermark-launcher.py" "${APP_DIR}/usr/bin/hypermark"
install -m 644 "${SRC_DIR}/icons/hypermark.svg" "${APP_DIR}/hypermark.svg"
install -m 644 "${SRC_DIR}/icons/hypermark.png" "${APP_DIR}/hypermark.png"
install -m 644 "${SRC_DIR}/icons/hypermark.png" "${APP_DIR}/.DirIcon"

# Desktop launcher for the AppImage run dir
mkdir -p "${APP_DIR}/usr/share/applications" "${APP_DIR}/usr/share/icons/hicolor/256x256/apps" "${APP_DIR}/usr/share/icons/hicolor/scalable/apps"
install -m 644 "${SRC_DIR}/hypermark.desktop" "${APP_DIR}/usr/share/applications/hypermark.desktop"
install -m 644 "${SRC_DIR}/icons/hypermark.svg" "${APP_DIR}/usr/share/icons/hicolor/scalable/apps/hypermark.svg"
install -m 644 "${SRC_DIR}/icons/hypermark.png" "${APP_DIR}/usr/share/icons/hicolor/256x256/apps/hypermark.png"

# Bundle the Electron runtime for a true native window (optional, if fetched)
ELECTRON_DIR="${DIR}/.cache/electron-current"
if [ -x "${ELECTRON_DIR}/electron" ]; then
    echo " Bundling Electron native-window runtime..."
    install -m 644 "${SRC_DIR}/main-electron.js" "${APP_DIR}/usr/share/hypermark/main-electron.js"
    realpath_dir="$(readlink -f "${ELECTRON_DIR}")"
    mkdir -p "${APP_DIR}/usr/share/hypermark/electron"
    cp -aL "${realpath_dir}/." "${APP_DIR}/usr/share/hypermark/electron/"
else
    echo " NOTE: Electron runtime not fetched — native window engine will be"
    echo "       unavailable in this build. Run linux/fetch-electron.sh first."
fi

cat << 'EOF' > "${APP_DIR}/AppRun"
#!/bin/bash
# AppRun entrypoint for the HyperMark click-to-run executable
HERE="$(dirname "$(readlink -f "${0}")")"
export APPDIR="${HERE}"
export GI_TYPELIB_PATH="${HERE}/usr/lib/girepository-1.0"
if command -v python3 >/dev/null 2>&1; then
    exec python3 "${HERE}/usr/bin/hypermark" "$@"
elif command -v python >/dev/null 2>&1; then
    exec python "${HERE}/usr/bin/hypermark" "$@"
else
    echo "HyperMark needs python3 to run." >&2
    exit 1
fi
EOF
chmod 755 "${APP_DIR}/AppRun"

# ---------------------------------------------------------------------------
# 2. Package AppDir into a compressed self-extracting payload
# ---------------------------------------------------------------------------
PAYLOAD_TAR="${DIST_DIR}/appdir_payload.tar.gz"
tar -czf "${PAYLOAD_TAR}" -C "${APP_DIR}" .

cat << 'EOF' > "${OUTPUT_APPIMAGE}"
#!/bin/bash
# HyperMark — Standalone Click-to-Run Executable for Linux
# Zero installation: extracts itself to a per-user cache and launches.
set -e

CACHE_DIR="${TMPDIR:-/tmp}/.hypermark-app-${USER:-$$}"
MD5_SELF="$(md5sum "$0" 2>/dev/null | awk '{print $1}' || echo "v1")"
RUN_DIR="${CACHE_DIR}/${MD5_SELF}"

if [ ! -f "${RUN_DIR}/AppRun" ]; then
    mkdir -p "${RUN_DIR}"
    PAYLOAD_LINE=$(awk '/^__PAYLOAD_BELOW__/ {print NR + 1; exit 0; }' "$0")
    tail -n +"${PAYLOAD_LINE}" "$0" | tar -xzf - -C "${RUN_DIR}"
fi

export APPDIR="${RUN_DIR}"
exec "${RUN_DIR}/AppRun" "$@"
__PAYLOAD_BELOW__
EOF

# Append the binary payload
cat "${PAYLOAD_TAR}" >> "${OUTPUT_APPIMAGE}"
rm -f "${PAYLOAD_TAR}"
chmod +x "${OUTPUT_APPIMAGE}"

echo "=========================================================="
echo " Standalone Click-to-Run executable created:"
echo "   ${OUTPUT_APPIMAGE}"
echo " Copy it anywhere, make it executable (chmod +x) and run:"
echo "   ./HyperMark-${VERSION}-x86_64.AppImage"
echo "=========================================================="
