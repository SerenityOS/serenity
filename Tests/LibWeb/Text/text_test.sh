#!/usr/bin/env bash

set -eo pipefail

SCRIPT_DIR="$(cd -P -- "$(dirname -- "${0}")" && pwd -P)"
LADYBIRD_BUILD_DIR="${1}"

if [[ -z "${LADYBIRD_BUILD_DIR}" ]] ; then
    echo "Provide path to the Ladybird build directory"
    exit 1
fi

BROWSER_BINARY="headless-browser"

find "${SCRIPT_DIR}/input/" -type f -name "*.html" -print0 | while IFS= read -r -d '' input_html_path; do
    input_html_file=${input_html_path/${SCRIPT_DIR}"/input/"/}
    input_html_file=${input_html_file/".html"/}

    output_text_dump=$(cd "${LADYBIRD_BUILD_DIR}"; timeout 300s "../bin/${BROWSER_BINARY}" --dump-text "${input_html_path}")
    expected_text_dump_path="${SCRIPT_DIR}/expected/${input_html_file}.txt"

    if cmp <(echo "${output_text_dump}") "${expected_text_dump_path}"; then
        echo "${input_html_file} PASSED"
    else
        echo "${input_html_file} FAILED"
        diff -u "${expected_text_dump_path}" <(echo "${output_text_dump}")
    fi
done
