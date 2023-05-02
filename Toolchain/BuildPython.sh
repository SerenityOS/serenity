#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildPython.sh as root, parts of your Toolchain directory will become root-owned"

PREFIX_DIR="$DIR/Local/python"
BUILD_DIR="$DIR/Build/python"
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

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

mkdir -p "${PREFIX_DIR}"
mkdir -p "${BUILD_DIR}"

pushd "${BUILD_DIR}"
    "${TARBALLS_DIR}"/Python-"${PYTHON_VERSION}"/configure --prefix="${PREFIX_DIR}"
    make -j "${MAKEJOBS}"
    make install
popd
