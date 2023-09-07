#!/usr/bin/env bash

set -eo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

SERENITY_ROOT="$(realpath "${DIR}"/../..)"

# shellcheck source=/dev/null
. "${SERENITY_ROOT}/Meta/shell_include.sh"

# shellcheck source=/dev/null
. "${SERENITY_ROOT}/Meta/find_compiler.sh"

pick_host_compiler

BUILD_DIR=${BUILD_DIR:-"${SERENITY_ROOT}/Build"}
CACHE_DIR=${CACHE_DIR:-"${BUILD_DIR}/caches"}

cmake -S "$SERENITY_ROOT/Meta/Lagom" -B "$BUILD_DIR/lagom-tools" \
    -GNinja -Dpackage=LagomTools \
    -DCMAKE_INSTALL_PREFIX="$BUILD_DIR/lagom-tools-install"  \
    -DCMAKE_C_COMPILER="$CC" \
    -DCMAKE_CXX_COMPILER="$CXX" \
    -DSERENITY_CACHE_DIR="$CACHE_DIR"

ninja -C "$BUILD_DIR/lagom-tools" install
