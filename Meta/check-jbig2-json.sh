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
