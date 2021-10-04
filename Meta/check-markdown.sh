#!/usr/bin/env bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

if [ -z "${CHECK_MARKDOWN_BINARY:-}" ] ; then
    if ! [ -d Build/lagom/ ] ; then
        echo "Directory Build/lagom/ does not exist. Skipping markdown check."
        exit 0
    fi
    if ! [ -r Build/lagom/markdown-check ] ; then
        echo "Lagom executable markdown-check was not built. Skipping markdown check."
        echo "To enable this check, you may need to run './Meta/serenity.sh build lagom' first."
        exit 0
    fi
    CHECK_MARKDOWN_BINARY="Build/lagom/markdown-check"
fi

find AK Base Documentation Kernel Meta Ports Tests Userland -path 'Ports/*/*' -prune -o -type f -name '*.md' -print0 | xargs -0 "${CHECK_MARKDOWN_BINARY}" README.md
