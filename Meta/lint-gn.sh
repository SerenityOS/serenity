#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files '*.gn' '*.gni'
    )
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".gn" ]] || [[ "${file}" == *".gni" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    if ! command -v gn >/dev/null 2>&1 ; then
        echo "gn is not available, but gn files need linting! Either skip this script, or install gn."
        exit 1
    fi
    gn format "${files[@]}"
else
    echo "No .gn or .gni files to check."
fi
