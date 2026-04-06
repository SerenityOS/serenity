#!/usr/bin/env bash

set -euo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SERENITY_ROOT="$(realpath "${DIR}"/../../..)"

. "${SERENITY_ROOT}/Meta/shell_include.sh"

SERENITY_ARCH="${SERENITY_ARCH:-x86_64}"
SERENITY_TOOLCHAIN="${SERENITY_TOOLCHAIN:-GNU}"

toolchain_suffix=
if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    toolchain_suffix="clang"
fi

BUILD_DIR="${SERENITY_ROOT}/Build/${SERENITY_ARCH}${toolchain_suffix}"
RESULTS="$BUILD_DIR/os-test-results"


mkdir -p "$RESULTS"
mkdir -p "$BUILD_DIR/mnt"

echo "[ELEVATED] Mounting _disk_image into $BUILD_DIR/mnt, this requires elevated privileges..."
${SUDO} mount "$BUILD_DIR/_disk_image" "$BUILD_DIR/mnt"
echo "[ELEVATED] Copying profile data files out of $BUILD_DIR/mnt, this requires elevated privileges..."
${SUDO} cp -r "$BUILD_DIR/mnt/usr/local/os-test/"* "$RESULTS"
echo "[ELEVATED] Unmounting _disk_image, this requires elevated privileges..."
${SUDO} umount "$BUILD_DIR/mnt"
echo "[ELEVATED] Making sure profile data files are owned by the current user, this requires elevated privileges..."
${SUDO} chown -R "$(id -u)":"$(id -g)" "$RESULTS/"

cd "$RESULTS"
make misc/html && misc/html
echo "Results can be viewed with \`open $RESULTS/html/index.html\`"
