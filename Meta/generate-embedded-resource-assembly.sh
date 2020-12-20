#!/bin/bash

set -e

if [ -z "$1" ]; then
    echo "ERROR: No output file specified"
    exit 1
fi

OUTPUT_FILE="$1"
shift

rm -f "${OUTPUT_FILE}"

while (( "$#" >= 2)); do
    SECTION_NAME="$1"
    INPUT_FILE="$2"

    {
        printf '    .section %s\n' "${SECTION_NAME}"
        printf '    .type %s, @object\n' "${SECTION_NAME}"
        printf '    .align 4\n'
        printf '    .incbin "%s"\n' "${INPUT_FILE}"
        printf '\n'
    } >> "${OUTPUT_FILE}"
    shift 2
done
