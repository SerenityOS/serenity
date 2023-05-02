#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildRuby.sh as root, parts of your Toolchain directory will become root-owned"

PREFIX_DIR="$DIR/Local/ruby"
BUILD_DIR="$DIR/Build/ruby"
TARBALLS_DIR="$DIR/Tarballs"

# shellcheck source=/dev/null
source "$DIR/../Ports/ruby/version.sh"

mkdir -p "${TARBALLS_DIR}"

pushd "${TARBALLS_DIR}"
    if [ ! -e "${RUBY_ARCHIVE}" ]; then
        echo "Downloading Ruby from ${RUBY_ARCHIVE_URL}..."
        curl -O "${RUBY_ARCHIVE_URL}"
    else
        echo "${RUBY_ARCHIVE} already exists, not downloading archive"
    fi

    if ! sha256sum --status -c <(echo "${RUBY_ARCHIVE_SHA256SUM}" "${RUBY_ARCHIVE}"); then
        echo "Ruby archive SHA256 sum mismatch, please run script again"
        rm -f "${RUBY_ARCHIVE}"
        exit 1
    fi

    if [ ! -d "ruby-${RUBY_VERSION}" ]; then
        echo "Extracting ${RUBY_ARCHIVE}..."
        tar -xf "${RUBY_ARCHIVE}"
    else
        echo "ruby-${RUBY_VERSION} already exists, not extracting archive"
    fi
popd

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

mkdir -p "${PREFIX_DIR}"
mkdir -p "${BUILD_DIR}"

pushd "${BUILD_DIR}"
    "${TARBALLS_DIR}"/ruby-"${RUBY_VERSION}"/configure --prefix="${PREFIX_DIR}"
    make -j "${MAKEJOBS}"
    make install
popd
