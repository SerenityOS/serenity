#!/bin/sh
set -e pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

BAD_FILES=$(find Base/etc/ Base/res/ Base/www/ -type f -executable)

if [ -n "${BAD_FILES}" ]
then
    echo "These files are marked as executable, but are in directories that do not commonly"
    echo "contain executables. Please double-check the permissions of these files:"
    echo "${BAD_FILES}" | xargs ls -ld
    exit 1
fi
