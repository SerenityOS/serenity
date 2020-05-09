#!/bin/bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.." || exit 1

# We simply check if the file starts with:
# /*
#  * Copyright
PATTERN=$'^/\*\n \* Copyright'
ERRORS=()

while IFS= read -r f; do
    if [[ ! $(cat "$f") =~ $PATTERN ]]; then
        ERRORS+=("$f")
    fi
done < <(git ls-files -- \
'*.cpp' \
'*.h' \
':!:Tests' \
':!:Base' \
':!:Kernel/FileSystem/ext2_fs.h' \
':!:Libraries/LibC/getopt.cpp' \
':!:Libraries/LibCore/puff.h' \
':!:Libraries/LibELF/exec_elf.h' \
)

if (( ${#ERRORS[@]} )); then
    echo "Files missing license headers: ${ERRORS[*]}"
    exit 1
fi
