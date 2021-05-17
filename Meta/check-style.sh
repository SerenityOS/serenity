#!/usr/bin/env bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.." || exit 1

# Ensure copyright headers match this format and are followed by a blank line:
# /*
#  * Copyright (c) YYYY(-YYYY), Whatever
#  * ... more of these ...
#  *
#  * SPDX-License-Identifier: BSD-2-Clause
#  */
GOOD_LICENSE_HEADER_PATTERN=$'^/\*\n( \* Copyright \(c\) [0-9]{4}(-[0-9]{4})?, .*\n)+ \*\n \* SPDX-License-Identifier: BSD-2-Clause\n \*/\n\n'
BAD_LICENSE_HEADER_ERRORS=()
LICENSE_HEADER_CHECK_EXCLUDES=(AK/Checked.h AK/Function.h Userland/Libraries/LibC/elf.h Userland/DevTools/HackStudio/LanguageServers/Cpp/Tests/* Userland/Libraries/LibCpp/Tests/*)

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
    if [[ ! "${LICENSE_HEADER_CHECK_EXCLUDES[*]} " =~ $f ]]; then
        if [[ ! "$file_content" =~ $GOOD_LICENSE_HEADER_PATTERN ]]; then
            BAD_LICENSE_HEADER_ERRORS+=("$f")
        fi
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
)

exit_status=0
if (( ${#BAD_LICENSE_HEADER_ERRORS[@]} )); then
    echo "Files with missing or incorrect license header: ${BAD_LICENSE_HEADER_ERRORS[*]}"
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
