#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files \
            --exclude-from .prettierignore \
            -- \
            '*.js' '*.mjs'
    )
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".js" ]] || [[ "${file}" == *".mjs" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    if ! command -v prettier >/dev/null 2>&1 ; then
        echo "prettier is not available, but JS files need linting! Either skip this script, or install prettier."
        exit 1
    fi

    if ! prettier --version | grep -qF '2.' ; then
        echo "You are using '$(prettier --version)', which appears to not be prettier 2."
        exit 1
    fi

    prettier --check "${files[@]}"
else
    echo "No .js or .mjs files to check."
fi
