#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR="$(dirname "${0}")"
SERENITY_ROOT="$(realpath "${SCRIPT_DIR}"/../../..)"
LADYBIRD_BUILD_DIR="${SERENITY_ROOT}/Build/ladybird"

for filename in "$SCRIPT_DIR"/input/*; do
    name=$(basename "$filename" .html)
    input_html_path=$(realpath "$SCRIPT_DIR")/input/"$name".html
    output_layout_dump=$(cd "$LADYBIRD_BUILD_DIR"; ./ladybird -d "$input_html_path")
    expected_layout_dump_path="$(realpath "$SCRIPT_DIR")/expected/$name.txt"
    if cmp <(echo "$output_layout_dump") "$expected_layout_dump_path"; then
        echo "$name PASSED"
    else
        echo "$name FAILED"
        diff "$expected_layout_dump_path" <(echo "$output_layout_dump")
    fi
done
