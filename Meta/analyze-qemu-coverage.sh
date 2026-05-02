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

mkdir "$TEMP_PROFDATA"
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


echo "Discovering all binaries and shared libraries in $BUILD_DIR/Root"
# shellcheck disable=SC2156 # The recommended fix on the Shellcheck github page for this warning causes the script to not find any files at all
all_binaries=$(find "$BUILD_DIR"/Root -type f -executable -exec sh -c "file {} | grep -vi relocatable | grep -Eiq ': elf (32|64)-bit'" \; -printf "-object %p\n" | grep -Ev '(usr\/local|boot\/|Loader.so)')

CLANG_BINDIR="${SERENITY_ROOT}/Toolchain/Local/clang/bin"
LLVM_PROFDATA="$CLANG_BINDIR/llvm-profdata"
PROFDATA_INVOCATION="$LLVM_PROFDATA merge -sparse"
LLVM_COV="$CLANG_BINDIR/llvm-cov"

# Merge profiles
LIBRARY_DIRS=$(find "$TEMP_PROFDATA/profiles/" -mindepth 1 -type d)
for dir in $LIBRARY_DIRS;
do
    library=$(basename "$dir")
    echo "Merging profiles for $library"
    prof_raws=$(find "$dir" -name "*.profraw")
    # shellcheck disable=SC2086 # The solution would be to use a bash array but it's just easier to omit the quotes.
    $PROFDATA_INVOCATION $prof_raws -o "$dir/$library.profdata"
done

echo "Merging all profiles"
GLOBAL_PROFILE="$TEMP_PROFDATA/global_coverage.profdata"
PROF_DATA_FILES=$(find "$TEMP_PROFDATA/profiles/" -name "*.profdata")
# shellcheck disable=SC2086
$PROFDATA_INVOCATION $PROF_DATA_FILES -o "$GLOBAL_PROFILE"

echo "Generating global html report"
# shellcheck disable=SC2086
$LLVM_COV show $all_binaries \
          -format html \
          -instr-profile "$GLOBAL_PROFILE" \
          -o "$BUILD_DIR/reports" \
          -show-line-counts-or-regions -show-directory-coverage \
          -Xdemangler "$CLANG_BINDIR/llvm-cxxfilt" -Xdemangler -n \
          -compilation-dir="${SERENITY_ROOT}"
