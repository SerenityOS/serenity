#!/usr/bin/env bash

set -eo pipefail

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

for f in Tests/LibGfx/test-inputs/jbig2/json/*.json; do
  f_jb2=Tests/LibGfx/test-inputs/jbig2/$(basename "${f%.json}.jbig2")
  "$JBIG2_FROM_JSON_BINARY" -o "$f_jb2" "$f"
done

# annex-h.jbig2 is fixed data from Annex H of the JBIG2 spec.
# It should never change, if it does, that's a bug.
ANNEX_H_EXPECTED_SHA=5ac786acab9b481d36c3fd1d23cf45d0896d1e27
ANNEX_H_ACTUAL_SHA=$(shasum "Tests/LibGfx/test-inputs/jbig2/annex-h.jbig2" | awk '{print $1}')

if [ "$ANNEX_H_ACTUAL_SHA" != "$ANNEX_H_EXPECTED_SHA" ]; then
    echo "annex-h.jbig2 has changed"
    exit 1
fi
