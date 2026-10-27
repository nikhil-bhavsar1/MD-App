#!/bin/bash
# HyperMark — One-command RPM builder.
#
#   ./build-rpm.sh
#
# Produces: linux/rpmbuild/RPMS/x86_64/hypermark-<version>.<dist>.rpm
#           (also copied to linux/dist/)
#
# Requires: rpmbuild (installed by default on Fedora, and via
# sudo dnf install rpm-build on RHEL/CentOS/openSUSE).
# No root privileges are needed — everything is built in-tree.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${DIR}/src"
RPMB_DIR="${DIR}/rpmbuild"
APP_SRC="${SRC_DIR}/app"

# Arch (x86_64 / aarch64) and dist tag (fc44 etc.)
ARCH="$(arch)"
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DIST="${ID^^}_$(echo "$VERSION_ID" | cut -d. -f1)"
    DIST_TAG=".${ID,,}${VERSION_ID%%.*}"
else
    DIST="LINUX"
    DIST_TAG=""
fi

# Packages the spec can see
mkdir -p "${RPMB_DIR}"/{BUILD,BUILDROOT,RPMS/x86_64,RPMS/aarch64,SRPMS,SPECS,SOURCES}

# Snyc -> Sync application sources into the RPM SOURCES dir
rm -rf "${RPMB_DIR}/SOURCES/app"
cp -r "${APP_SRC}" "${RPMB_DIR}/SOURCES/app"
find "${RPMB_DIR}/SOURCES/app" -name "__pycache__" -type d -prune -exec rm -rf {} +
install -m 755 "${SRC_DIR}/hypermark-launcher.py" "${RPMB_DIR}/SOURCES/hypermark-launcher.py"
install -m 644 "${SRC_DIR}/hypermark.desktop" "${RPMB_DIR}/SOURCES/hypermark.desktop"
install -m 644 "${SRC_DIR}/icons/hypermark.svg" "${RPMB_DIR}/SOURCES/hypermark.svg"
install -m 644 "${SRC_DIR}/icons/hypermark.png" "${RPMB_DIR}/SOURCES/hypermark.png"

# Bundle the Electron native-window runtime (optional, if fetched)
RPM_DEFINES=(--define "_topdir ${RPMB_DIR}" --define "_target_cpu ${ARCH}")
ELECTRON_DIR="${DIR}/.cache/electron-current"
if [ -x "${ELECTRON_DIR}/electron" ]; then
    echo "==> Bundling Electron native-window runtime into the RPM..."
    rm -rf "${RPMB_DIR}/SOURCES/electron"
    mkdir -p "${RPMB_DIR}/SOURCES/electron"
    cp -aL "${ELECTRON_DIR}/." "${RPMB_DIR}/SOURCES/electron/"
    install -m 644 "${SRC_DIR}/main-electron.js" "${RPMB_DIR}/SOURCES/main-electron.js"
    RPM_DEFINES+=(--define "with_electron 1")
else
    echo " NOTE: Electron runtime not fetched (linux/fetch-electron.sh) —"
    echo "       RPM will fall back to browser app-window."
fi

echo "==> Building RPM for ${DIST} (${ARCH})..."
cd "${RPMB_DIR}"
rpmbuild \
    "${RPM_DEFINES[@]}" \
    -bb SPECS/hypermark.spec

RPM_FILE="$(find "${RPMB_DIR}/RPMS/${ARCH}" -name 'hypermark-*.rpm' | head -1)"

# Also drop the result into linux/dist/
mkdir -p "${DIR}/dist"
cp -f "${RPM_FILE}" "${DIR}/dist/"

echo "=========================================================="
echo " RPM created: ${RPM_FILE}"
echo " Copy:        ${DIR}/dist/$(basename "${RPM_FILE}")"
echo
echo " Install it with:"
echo "   sudo dnf install $(basename "${RPM_FILE}")"
echo "   (or: sudo rpm -i $(basename "${RPM_FILE}"))"
echo "=========================================================="
