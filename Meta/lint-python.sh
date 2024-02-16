#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files '*.py'
    )
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".py" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    if ! command -v black >/dev/null 2>&1 ; then
        echo "black is not available, but python files need linting! Either skip this script, or install black."
        exit 1
    fi

    black "${files[@]}"
else
    echo "No py files to check."
fi
