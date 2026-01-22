#!/usr/bin/env bash

# This script builds the CMake build system
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildCMake.sh as root, parts of your Toolchain directory will become root-owned"

PREFIX_DIR="$DIR/Local/cmake"
BUILD_DIR="$DIR/Build/cmake"
TARBALLS_DIR="$DIR/Tarballs"

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

# Note: Update this alongside the cmake port, and Meta/CMake/cmake-version.cmake if the build requires this version of cmake.
CMAKE_VERSION=3.30.0
CMAKE_ARCHIVE_SHA256=157e5be6055c154c34f580795fe5832f260246506d32954a971300ed7899f579
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

    if ! check_sha256 "${CMAKE_ARCHIVE}" "${CMAKE_ARCHIVE_SHA256}"; then
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

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

pushd "${BUILD_DIR}"
    "${TARBALLS_DIR}"/cmake-"${CMAKE_VERSION}"/bootstrap --generator="Ninja" --prefix="${PREFIX_DIR}" --parallel="${MAKEJOBS}"
    ninja -j "${MAKEJOBS}"
    ninja install
popd
