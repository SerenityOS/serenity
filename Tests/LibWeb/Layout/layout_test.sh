#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR="$(cd -P -- "$(dirname -- "${0}")" && pwd -P)"
LADYBIRD_BUILD_DIR="${1}"

if [[ -z "${LADYBIRD_BUILD_DIR}" ]] ; then
    echo "Provide path to the Ladybird build directory"
    exit 1
fi

BROWSER_BINARY="./headless-browser"

for input_html_path in "${SCRIPT_DIR}"/input/*.html; do
    input_html_file="$(basename "${input_html_path}" .html)"

    output_layout_dump=$(cd "${LADYBIRD_BUILD_DIR}"; "${BROWSER_BINARY}" -d "${input_html_path}")
    expected_layout_dump_path="${SCRIPT_DIR}/expected/${input_html_file}.txt"

    if cmp <(echo "${output_layout_dump}") "${expected_layout_dump_path}"; then
        echo "${input_html_file} PASSED"
    else
        echo "${input_html_file} FAILED"
        diff -u "${expected_layout_dump_path}" <(echo "${output_layout_dump}")
    fi
done
