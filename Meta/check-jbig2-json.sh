#!/usr/bin/env bash

set -eo pipefail

jbig_files=(Tests/LibGfx/test-inputs/jbig2/*.jbig2)
json_files=(Tests/LibGfx/test-inputs/jbig2/json/*.json)

if [ "${#jbig_files[@]}" -gt "${#json_files[@]}" ]; then
    echo "More jbig2 than json files. Don't add non-json-based jbig2 files."
    exit 1
fi

if [ "${#jbig_files[@]}" -lt "${#json_files[@]}" ]; then
    echo "More json than jbig2 files. Did you forget to 'git add'?"
    exit 1
fi

trap 'git diff --exit-code' EXIT

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

if [ -z "${JBIG2_FROM_JSON_BINARY:-}" ] ; then
    if ! [ -d Build/lagom/ ] ; then
        echo "Directory Build/lagom/ does not exist. Skipping JBIG2 json check."
        exit 0
    fi
    if ! [ -r Build/lagom/bin/jbig2-from-json ] ; then
        echo "Lagom executable jbig2-from-json was not built. Skipping JBIG2 json check."
        echo "To enable this check, you may need to run './Meta/serenity.sh build lagom' first."
        exit 0
    fi
    JBIG2_FROM_JSON_BINARY="Build/lagom/bin/jbig2-from-json"
fi

Tests/LibGfx/test-inputs/jbig2/json/compile.sh "$JBIG2_FROM_JSON_BINARY"

# annex-h.jbig2 is fixed data from Annex H of the JBIG2 spec.
# It should never change, if it does, that's a bug.
ANNEX_H_EXPECTED_SHA=5ac786acab9b481d36c3fd1d23cf45d0896d1e27
ANNEX_H_ACTUAL_SHA=$(shasum "Tests/LibGfx/test-inputs/jbig2/annex-h.jbig2" | awk '{print $1}')

if [ "$ANNEX_H_ACTUAL_SHA" != "$ANNEX_H_EXPECTED_SHA" ]; then
    echo "annex-h.jbig2 has changed"
    exit 1
fi
