#!/bin/bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

if ! command -v shellcheck &>/dev/null ; then
    echo "shellcheck is not available. Either skip this script, or install shellcheck."
    exit 1
fi

if [ "$#" -eq "0" ]; then
    mapfile -t files < <(
        git ls-files -- \
            '*.sh' \
            ':!:Toolchain' \
            ':!:Ports' \
            ':!:Shell/Tests'
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
    shellcheck "${files[@]}"
else
    echo "No .sh files to check."
fi
