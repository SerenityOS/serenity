#!/bin/sh

set -e

if [ -z "$SERENITY_ROOT" ]
then
    SERENITY_ROOT="$(git rev-parse --show-toplevel)"
    echo "Serenity root not set. This is fine! Other scripts may require you to set the environment variable first, e.g.:"
    echo "    export SERENITY_ROOT=${SERENITY_ROOT}"
fi

cd "$SERENITY_ROOT"

find . \( -name Base -o -name Patches -o -name Ports -o -name Root -o -name Toolchain -o -name Build \) -prune -o \( -name '*.ipc' -or -name '*.cpp' -or -name '*.idl' -or -name '*.c' -or -name '*.h' -or -name '*.S' -or -name '*.css' -or -name '*.json' \) -print > serenity.files
