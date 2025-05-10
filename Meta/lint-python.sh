#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

# Either use the Serenity base directory, or the files passed on the command line.
if [ "$#" -eq "0" ]; then
    files=(".")
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".py" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    if ! command -v ruff >/dev/null 2>&1 ; then
        echo "ruff is not available, but python files need linting! Either skip this script, or install ruff."
        exit 1
    fi

    # First run formatting, then style checks
    ruff format "${files[@]}"
    ruff check --fix "${files[@]}"
else
    echo "No Python files to check."
fi
