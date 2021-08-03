#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ARCH=${ARCH:-"i686"}
PREFIX_DIR="$DIR/Local/$ARCH"
BUILD_DIR="$DIR/Build/$ARCH"
TARBALLS_DIR="$DIR/Tarballs"

# shellcheck source=/dev/null
source "$DIR/../Ports/python3/version.sh"

mkdir -p "${TARBALLS_DIR}"

pushd "${TARBALLS_DIR}"
    if [ ! -e "${PYTHON_ARCHIVE}" ]; then
        echo "Downloading Python from ${PYTHON_ARCHIVE_URL}..."
        curl -O "${PYTHON_ARCHIVE_URL}"
    else
        echo "${PYTHON_ARCHIVE} already exists, not downloading archive"
    fi

    if ! sha256sum --status -c <(echo "${PYTHON_ARCHIVE_SHA256SUM}" "${PYTHON_ARCHIVE}"); then
        echo "Python archive SHA256 sum mismatch, please run script again"
        rm -f "${PYTHON_ARCHIVE}"
        exit 1
    fi

    if [ ! -d "Python-${PYTHON_VERSION}" ]; then
        echo "Extracting ${PYTHON_ARCHIVE}..."
        tar -xf "${PYTHON_ARCHIVE}"
    else
        echo "Python-${PYTHON_VERSION} already exists, not extracting archive"
    fi
popd

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

mkdir -p "${PREFIX_DIR}"
mkdir -p "${BUILD_DIR}/python"

pushd "${BUILD_DIR}/python"
    "${TARBALLS_DIR}"/Python-"${PYTHON_VERSION}"/configure --prefix="${PREFIX_DIR}"
    make -j "${MAKEJOBS}"
    make install
popd
