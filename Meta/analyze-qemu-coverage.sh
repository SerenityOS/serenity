#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR="$(dirname "${0}")"
SERENITY_ROOT="$(realpath "${SCRIPT_DIR}"/..)"

if [ -z "$SERENITY_ARCH" ]; then
    SERENITY_ARCH="x86_64"
fi

toolchain_suffix=
if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    toolchain_suffix="clang"
fi

BUILD_DIR="${SERENITY_ROOT}/Build/${SERENITY_ARCH}${toolchain_suffix}"
TEMP_PROFDATA="$BUILD_DIR/tmp_profile_data"

mkdir -p "$TEMP_PROFDATA"
mkdir -p "$BUILD_DIR/mnt"

[ -z "${ELEVATE+x}" ] && ELEVATE="sudo"
echo "ELEVATE is $ELEVATE"

echo "[ELEVATED] Mounting _disk_image into $BUILD_DIR/mnt, this requires elevated privileges..."
"$ELEVATE" mount "$BUILD_DIR/_disk_image" "$BUILD_DIR/mnt"
echo "[ELEVATED] Copying profile data files out of $BUILD_DIR/mnt, this requires elevated privileges..."
"$ELEVATE" cp -r "$BUILD_DIR/mnt/home/anon/profiles" "$TEMP_PROFDATA/"
echo "[ELEVATED] Unmounting _disk_image, this requires elevated privileges..."
"$ELEVATE" umount "$BUILD_DIR/mnt"
echo "[ELEVATED] Making sure profile data files are owned by the current user, this requires elevated privileges..."
"$ELEVATE" chown -R "$(id -u)":"$(id -g)" "$TEMP_PROFDATA/"


echo "Moving profile data into $TEMP_PROFDATA directly..."
mv "$TEMP_PROFDATA"/profiles/* "$TEMP_PROFDATA/"

echo "Discovering all binaries and shared libraries in $BUILD_DIR/Root"
# shellcheck disable=SC2156 # The recommended fix on the Shellcheck github page for this warning causes the script to not find any files at all
mapfile -d '\n' all_binaries < <(find "$BUILD_DIR"/Root -type f -exec sh -c "file {} | grep -vi relocatable | grep -Eiq ': elf (32|64)-bit'" \; -print | grep -Ev '(usr\/Tests|usr\/local|boot\/|Loader.so)')

# FIXME: Come up with our own coverage prep script instead of using llvm's
COVERAGE_PREPARE="$BUILD_DIR/prepare-code-coverage-artifact.py"
if [ ! -f "$COVERAGE_PREPARE" ]; then
    # Download coverage prep script from github
    LLVM_14_RELEASE_HASH=329fda39c507e8740978d10458451dcdb21563be
    SHA256_SUM=2cf1019d1df9a10c87234e0ec9c984dbb97d5543688b7f4a7387cb377ced7f21
    URL=https://raw.githubusercontent.com/llvm/llvm-project/${LLVM_14_RELEASE_HASH}/llvm/utils/prepare-code-coverage-artifact.py

    echo "Downloading prepare-code-coverage-artifact.py from ${URL}"
    wget "$URL" -P "$BUILD_DIR"

    # Verify hash matches for integrity
    echo "Expecting sha256sum: $SHA256_SUM"
    CALC_SUM="$(sha256sum "${COVERAGE_PREPARE}" | cut -f1 -d' ')"
    echo "sha256sum($COVERAGE_PREPARE) = '$CALC_SUM'"
    if [ "$CALC_SUM" != "$SHA256_SUM" ]; then
        # remove downloaded file to re-download on next run
        rm -f "$COVERAGE_PREPARE"
        echo "sha256sums mismatching, removed erroneous download. Please re-try."
        exit 1
    fi
fi

CLANG_BINDIR="${SERENITY_ROOT}/Toolchain/Local/clang/bin"

# shellcheck disable=SC2128,SC2086 # all_binaries variable needs expanded to space separated string, not newline separated string
python3 "$COVERAGE_PREPARE" \
    --unified-report \
    -C "${SERENITY_ROOT}" \
    "$CLANG_BINDIR/llvm-profdata" "$CLANG_BINDIR/llvm-cov" \
    "$TEMP_PROFDATA/" \
    "$BUILD_DIR/reports/" \
    $all_binaries
