#!/bin/bash

set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$DIR/../../../../.."
LAGOM_BUILD="$ROOT/Build/lagom"
JBIG2_FROM_JSON=${1:-"$LAGOM_BUILD/bin/jbig2-from-json"}

export DIR
export JBIG2_FROM_JSON

run_jbig2() {
    f="$1"
    filename=$(basename "${f%.json}.jbig2")
    f_jb2="$DIR/../$filename"

    if ! output=$("$JBIG2_FROM_JSON" -o "$f_jb2" "$f" 2>&1); then
        echo failed to run:
        echo "$JBIG2_FROM_JSON" -o "$f_jb2" "$f"
        echo "$output"
        return 1
    fi
}

. "$ROOT/Meta/shell_include.sh"
NPROC=$(get_number_of_processing_units)

export -f run_jbig2
find "$DIR" -maxdepth 1 -name "*.json" -print0 | \
    xargs -0 -P "$NPROC" -I {} bash -c 'run_jbig2 "$@"' _ {}
