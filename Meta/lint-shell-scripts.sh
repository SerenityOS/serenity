#!/bin/bash
set -e pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

ERRORS=()

for f in $(find . -path ./Root -prune -o \
    -path ./Ports -prune -o \
    -path ./.git -prune -o \
    -path ./Toolchain -prune -o \
    -type f | sort -u); do
    if file "$f" | grep --quiet shell; then
        {
            shellcheck "$f" && echo -e "[\033[0;32mOK\033[0m]: sucessfully linted $f"
        } || {
            ERRORS+=("$f")
        }
fi
done

if (( ${#ERRORS[@]} )); then
    echo "Files failing shellcheck: ${ERRORS[*]}"
    exit 1
fi
