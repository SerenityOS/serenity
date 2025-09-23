#!/bin/bash

set -eu

DIR=$(dirname "$0")
LAGOM_BUILD=$DIR/../../../../../Build/lagom
JBIG2_FROM_JSON=$LAGOM_BUILD/bin/jbig2-from-json

for f in "$DIR"/*.json; do
  f_jb2="$DIR"/../$(basename "${f%.json}.jbig2")
  echo "$JBIG2_FROM_JSON" -o "$f_jb2" "$f"
  "$JBIG2_FROM_JSON" -o "$f_jb2" "$f"
done
