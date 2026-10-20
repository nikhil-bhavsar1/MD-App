#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${DIR}/AppDir"
DIST_DIR="${DIR}/dist"
OUTPUT_APPIMAGE="${DIST_DIR}/HyperMark-1.0.0-x86_64.AppImage"
ROOT_OUTPUT="/HyperMark-1.0.0-x86_64.AppImage"

mkdir -p "${DIST_DIR}"

# Package AppDir into a compressed payload
PAYLOAD_TAR="${DIST_DIR}/appdir_payload.tar.gz"
tar -czf "${PAYLOAD_TAR}" -C "${APP_DIR}" .

# Create the standalone click-to-run executable script with self-extracting header
cat << 'EOF' > "${OUTPUT_APPIMAGE}"
#!/bin/bash
# HyperMark — Standalone Click-to-Run Executable for Fedora 44
# Zero-installation portable binary

set -e

# Cache directory per user and version to avoid re-extracting on every launch
CACHE_DIR="/tmp/.hypermark-app-${USER:-$EUID}"
MD5_SELF="$(md5sum "$0" 2>/dev/null | awk '{print $1}' || echo "v1")"
RUN_DIR="${CACHE_DIR}/${MD5_SELF}"

if [ ! -d "${RUN_DIR}/usr" ]; then
    mkdir -p "${RUN_DIR}"
    # Find payload start line
    PAYLOAD_LINE=$(awk '/^__PAYLOAD_BELOW__/ {print NR + 1; exit 0; }' "$0")
    tail -n +"${PAYLOAD_LINE}" "$0" | tar -xzf - -C "${RUN_DIR}"
fi

export APPDIR="${RUN_DIR}"
export PATH="${RUN_DIR}/usr/bin:${PATH}"

exec "${RUN_DIR}/AppRun" "$@"
exit 0
__PAYLOAD_BELOW__
EOF

# Append the binary payload
cat "${PAYLOAD_TAR}" >> "${OUTPUT_APPIMAGE}"
rm -f "${PAYLOAD_TAR}"

chmod +x "${OUTPUT_APPIMAGE}"
cp -f "${OUTPUT_APPIMAGE}" "${ROOT_OUTPUT}"
chmod +x "${ROOT_OUTPUT}"

echo "=========================================================="
echo " Standalone Click-to-Run AppImage created successfully:"
echo "   1. ${OUTPUT_APPIMAGE}"
echo "   2. ${ROOT_OUTPUT}"
echo "=========================================================="
