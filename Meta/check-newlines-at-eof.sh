#!/bin/bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.." || exit 1

MISSING_NEWLINE_AT_EOF_ERRORS=()
MORE_THAN_ONE_NEWLINE_AT_EOF_ERRORS=()

while IFS= read -r f; do
    [ -s "$f" ] || continue

    if [ "$(tail -n 1 "$f" | wc -l)" != 1 ]; then
        MISSING_NEWLINE_AT_EOF_ERRORS+=( "$f" )
    elif [[ "$(tail -n 1 "$f")" =~ ^[[:space:]]*$ ]]; then
        MORE_THAN_ONE_NEWLINE_AT_EOF_ERRORS+=( "$f" )
    fi
done < <(git ls-files -- \
    '*.cpp' \
    '*.h' \
    '*.gml' \
    '*.html' \
    '*.js' \
    '*.css' \
    '*.sh' \
    ':!:Base' \
    ':!:Kernel/FileSystem/ext2_fs.h' \
    ':!:Libraries/LibC/getopt.cpp' \
    ':!:Libraries/LibCore/puff.h' \
    ':!:Libraries/LibCore/puff.cpp' \
    ':!:Libraries/LibELF/exec_elf.h' \
)

exit_status=0
if (( ${#MISSING_NEWLINE_AT_EOF_ERRORS[@]} )); then
    echo "Files with no newline at the end: ${MISSING_NEWLINE_AT_EOF_ERRORS[*]}"
    exit_status=1
fi
if (( ${#MORE_THAN_ONE_NEWLINE_AT_EOF_ERRORS[@]} )); then
    echo "Files that have blank lines at the end: ${MORE_THAN_ONE_NEWLINE_AT_EOF_ERRORS[*]}"
    exit_status=1
fi
exit "$exit_status"
