#!/usr/bin/env bash

# This script builds the CMake build system
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildCMake.sh as root, parts of your Toolchain directory will become root-owned"

PREFIX_DIR="$DIR/Local/cmake"
BUILD_DIR="$DIR/Build/cmake"
TARBALLS_DIR="$DIR/Tarballs"

NPROC="nproc"
SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    NPROC="sysctl -n hw.ncpuonline"
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    NPROC="sysctl -n hw.ncpu"
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    NPROC="sysctl -n hw.ncpu"
fi

[ -z "$MAKEJOBS" ] && MAKEJOBS=$($NPROC)

check_sha() {
    if [ $# -ne 2 ]; then
        error "Usage: check_sha FILE EXPECTED_HASH"
        return 1
    fi

    FILE="${1}"
    EXPECTED_HASH="${2}"

    if [ "$SYSTEM_NAME" = "Darwin" ]; then
        SEEN_HASH="$(shasum -a 256 "${FILE}" | cut -d " " -f 1)"
    else
        SEEN_HASH="$(sha256sum "${FILE}" | cut -d " " -f 1)"
    fi
    test "${EXPECTED_HASH}" = "${SEEN_HASH}"
}

# Note: Update this alongside Meta/CMake/cmake-version.cmake
CMAKE_VERSION=3.26.0
CMAKE_ARCHIVE_SHA256=4256613188857e95700621f7cdaaeb954f3546a9249e942bc2f9b3c26e381365
CMAKE_ARCHIVE=cmake-${CMAKE_VERSION}.tar.gz
CMAKE_ARCHIVE_URL=https://github.com/Kitware/CMake/releases/download/v${CMAKE_VERSION}/${CMAKE_ARCHIVE}

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs
    if [ ! -e "${CMAKE_ARCHIVE}" ]; then
        echo "Downloading CMake from ${CMAKE_ARCHIVE_URL}..."
        curl "${CMAKE_ARCHIVE_URL}" -L -o "${CMAKE_ARCHIVE}"
    else
        echo "${CMAKE_ARCHIVE} already exists, not downloading archive"
    fi

    if ! check_sha "${CMAKE_ARCHIVE}" "${CMAKE_ARCHIVE_SHA256}"; then
        echo "CMake archive SHA256 sum mismatch, please run script again"
        rm -f "${CMAKE_ARCHIVE}"
        exit 1
    fi

    if [ ! -d "cmake-${CMAKE_VERSION}" ]; then
        echo "Extracting ${CMAKE_ARCHIVE}..."
        tar -xf "${CMAKE_ARCHIVE}"
    else
        echo "cmake-${CMAKE_VERSION} already exists, not extracting archive"
    fi
popd

mkdir -p "${PREFIX_DIR}"
mkdir -p "${BUILD_DIR}"

pushd "${BUILD_DIR}"
    "${TARBALLS_DIR}"/cmake-"${CMAKE_VERSION}"/bootstrap --prefix="${PREFIX_DIR}" --parallel="${MAKEJOBS}"
    make -j "${MAKEJOBS}"
    make install
popd
