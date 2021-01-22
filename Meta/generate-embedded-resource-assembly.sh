#!/usr/bin/env bash

set -e

if [ -z "$1" ]; then
    echo "ERROR: No output file specified"
    exit 1
fi

OUTPUT_FILE="$1"
shift

rm -f "${OUTPUT_FILE}"

while (( "$#" >= 3 )); do
    SECTION_NAME="$1"
    INPUT_FILE="$2"
    FILE_SIZE="$3"

    {
        printf '    .file "%s"\n' "${OUTPUT_FILE}"
        printf '    .data\n'
        printf '    .section %s, "a", @progbits\n' "${SECTION_NAME}"
        printf '    .align 4\n'
        printf '    .globl %s\n' "${SECTION_NAME}_start"
        printf '    .type %s, @object\n' "${SECTION_NAME}_start"
        printf '    .size %s, 4\n' "${SECTION_NAME}_start"
        printf '%s:\n' "${SECTION_NAME}_start"
        printf '    .incbin "%s"\n' "${INPUT_FILE}"
        printf '    .section serenity_embedded_resource_info, "a", @progbits\n'
        printf '    .align 4\n'
        printf '    .globl %s\n' "${SECTION_NAME}_size"
        printf '    .type %s, @object\n' "${SECTION_NAME}_size"
        printf '    .size %s, 4\n' "${SECTION_NAME}_size"
        printf '%s:\n' "${SECTION_NAME}_size"
        printf '    .long %s\n' "${FILE_SIZE}"
        printf '\n'
    } >> "${OUTPUT_FILE}"
    shift 3
done
