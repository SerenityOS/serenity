#!/bin/sh
set -e pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

ALL_ENDPOINTS=$(find \( -name Toolchain -o -name Build -o -name .git -o -name Ports \) -prune -o -name '*.ipc' -print0 | xargs -0 grep -P '^endpoint ' | sort -k4 -n)

BAD_ENDPOINTS=$(echo "${ALL_ENDPOINTS}" | cut -d' ' -f4 | uniq -d)

if [ -n "${BAD_ENDPOINTS}" ]
then
    echo "This is the full list of all endpoints:"
    echo "${ALL_ENDPOINTS}"
    echo "These endpoint IDs are duplicated:"
    echo "${BAD_ENDPOINTS}"
    exit 1
fi
