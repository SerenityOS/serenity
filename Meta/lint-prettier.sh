#!/bin/bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if ! command -v prettier >/dev/null 2>&1 ; then
    echo "prettier is not available. Either skip this script, or install prettier."
    exit 1
fi

if ! prettier --version | grep -qF '2.' ; then
    echo "You are using '$(prettier --version)', which appears to not be prettier 2."
    exit 1
fi

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files \
            --exclude-from .prettierignore \
            -- \
            '*.js'
    )
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".js" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    prettier --check "${files[@]}"
else
    echo "No .js files to check."
fi
