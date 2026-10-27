#!/bin/bash
# HyperMark — downloads a self-contained Electron runtime (no system install).
# The app uses it at runtime to show a REAL native window (no browser chrome),
# the same engine VS Code / Discord ship with. Result is cached in linux/.cache
# and bundled automatically into the RPM / AppImage by the build scripts.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE="${DIR}/.cache"
ARCH="$(uname -m)"            # x86_64 / aarch64
ELECTRON_ARCH="$([ "$ARCH" = "aarch64" ] && echo arm64 || echo x64)"
VERSION="45.0.0"

mkdir -p "${CACHE}/electron-v${VERSION}-linux-${ELECTRON_ARCH}/Electron"
cd "${CACHE}"

echo "==> Fetching Electron v${VERSION} (linux-${ELECTRON_ARCH})..."
ZIP="electron-v${VERSION}-linux-${ELECTRON_ARCH}.zip"
curl -fL --retry 3 -o "${ZIP}" \
  "https://github.com/electron/electron/releases/download/v${VERSION}/electron-v${VERSION}-linux-${ELECTRON_ARCH}.zip"

echo "==> Unpacking..."
unzip -oq "${ZIP}" -d "electron-v${VERSION}-linux-${ELECTRON_ARCH}"
rm -f "${ZIP}"

ln -sfn "electron-v${VERSION}-linux-${ELECTRON_ARCH}" "${CACHE}/electron-current"

echo "Done: ${CACHE}/electron-v${VERSION}-linux-${ELECTRON_ARCH}"
rm -rf /tmp/opencode/electron-dl 2>/dev/null || true
