#!/usr/bin/env bash

set -e

trap 'git diff --exit-code' EXIT

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ -z "${GML_FORMAT:-}" ] ; then
    if ! [ -d Build/lagom/ ] ; then
        echo "Directory Build/lagom/ does not exist. Skipping GML formatting."
        exit 0
    fi
    if ! [ -r Build/lagom/bin/gml-format ] ; then
        echo "Lagom executable gml-format was not built. Skipping GML formatting."
        echo "To enable this check, you may need to run './Meta/serenity.sh build lagom' first."
        exit 0
    fi
    GML_FORMAT="Build/lagom/bin/gml-format"
fi

if [ "$#" -gt "0" ] ; then
    # We're in the middle of a pre-commit run, so we should only check the files that have
    # actually changed. The reason is that "git ls-files | grep" on the entire repo takes
    # about 100ms. That is perfectly fine during a CI run, but becomes noticeable during a
    # pre-commit hook. It is unnecessary to check the entire repository on every single
    # commit, so we save some time here.
    for file in "$@"; do
        if [[ "${file}" =~ \.gml ]]; then
            echo "$file"
        fi
    done
else
    find AK Base Documentation Kernel Meta Ports Tests Userland -type f -name '*.gml' -print
fi \
| xargs -r "${GML_FORMAT}" -i
