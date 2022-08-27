#!/usr/bin/env bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

files=()
for file in Base/res/emoji/*.png; do
    files+=("${file}")
done

found_invalid_filenames=0
for fn in "${files[@]}"; do
    basename=$(basename "$fn" .png)
    if [[ $basename =~ [^A-Z0-9+_] ]] ; then
        echo "$fn contains invalid characters in its filename. Only uppercase letters, numbers, +, and _ should be used."
        found_invalid_filenames=1
    fi
    if [[ $basename == *U+0* ]] ; then
        echo "$fn contains codepoint(s) with leading zeros. Leading zeros should be removed from codepoint(s)."
        found_invalid_filenames=1
    fi
    if [[ $basename == *+U* ]] ; then
        echo "$fn is incorrectly named. _ should be used as a separator between codepoints, not +."
        found_invalid_filenames=1
    fi
    if [[ $basename == *_U+FE0F* ]] ; then
        echo "$fn should not include any emoji presentation selectors. U+FE0F codepoints should be removed from the filename."
        found_invalid_filenames=1
    fi
done

exit $found_invalid_filenames
