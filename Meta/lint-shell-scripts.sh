#!/usr/bin/env bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files -- \
            '*.sh' \
            ':!:Toolchain' \
            ':!:Ports' \
            ':!:Userland/Shell/Tests' \
            ':!:Base/home/anon/tests'
    )
else
    files=()
    for file in "$@"; do
        if [[ "${file}" == *".sh" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    if ! command -v shellcheck &>/dev/null ; then
        echo "shellcheck is not available, but shell files need linting! Either skip this script, or install shellcheck."
        exit 1
    fi

    shellcheck "${files[@]}"
else
    echo "No .sh files to check."
fi
