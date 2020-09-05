#!/bin/bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.." || exit 1

# We simply check if the file starts with:
# /*
#  * Copyright
LICENSE_HEADER_PATTERN=$'^/\*\n \* Copyright'
MISSING_LICENSE_HEADER_ERRORS=()

# We check that "#pragma once" is present
PRAGMA_ONCE_PATTERN='#pragma once'
MISSING_PRAGMA_ONCE_ERRORS=()

# We make sure that there's a blank line before and after pragma once
GOOD_PRAGMA_ONCE_PATTERN=$'(^|\\S\n\n)#pragma once(\n\n\\S.|$)'
BAD_PRAGMA_ONCE_ERRORS=()

# We check that "#include <LibM/math.h>" is not being used
LIBM_MATH_H_INCLUDE_PATTERN='#include <LibM/math.h>'
LIBM_MATH_H_INCLUDE_ERRORS=()

while IFS= read -r f; do
    file_content="$(< "$f")"
    if [[ ! "$file_content" =~ $LICENSE_HEADER_PATTERN ]]; then
        MISSING_LICENSE_HEADER_ERRORS+=("$f")
    fi
    if [[ "$file_content" =~ $LIBM_MATH_H_INCLUDE_PATTERN ]]; then
        LIBM_MATH_H_INCLUDE_ERRORS+=("$f")
    fi
    if [[ "$f" =~ \.h$ ]]; then
        if [[ ! "$file_content" =~ $PRAGMA_ONCE_PATTERN ]]; then
            MISSING_PRAGMA_ONCE_ERRORS+=("$f")
        elif [[ ! "$file_content" =~ $GOOD_PRAGMA_ONCE_PATTERN ]]; then
            BAD_PRAGMA_ONCE_ERRORS+=("$f")
        fi
    fi
done < <(git ls-files -- \
    '*.cpp' \
    '*.h' \
    ':!:Base' \
    ':!:Kernel/FileSystem/ext2_fs.h' \
    ':!:Libraries/LibC/getopt.cpp' \
    ':!:Libraries/LibCore/puff.h' \
    ':!:Libraries/LibCore/puff.cpp' \
    ':!:Libraries/LibELF/exec_elf.h' \
)

exit_status=0
if (( ${#MISSING_LICENSE_HEADER_ERRORS[@]} )); then
    echo "Files missing license headers: ${MISSING_LICENSE_HEADER_ERRORS[*]}"
    exit_status=1
fi
if (( ${#MISSING_PRAGMA_ONCE_ERRORS[@]} )); then
    echo "Header files missing \"#pragma once\": ${MISSING_PRAGMA_ONCE_ERRORS[*]}"
    exit_status=1
fi
if (( ${#BAD_PRAGMA_ONCE_ERRORS[@]} )); then
    echo "\"#pragma once\" should have a blank line before and after in these files: ${BAD_PRAGMA_ONCE_ERRORS[*]}"
    exit_status=1
fi
if (( ${#LIBM_MATH_H_INCLUDE_ERRORS[@]} )); then
    echo "\"#include <LibM/math.h>\" should be replaced with just \"#include <math.h>\" in these files: ${LIBM_MATH_H_INCLUDE_ERRORS[*]}"
    exit_status=1
fi
exit "$exit_status"
