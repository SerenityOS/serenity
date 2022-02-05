#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ -z "${GML_FORMAT:-}" ] ; then
    if ! [ -d Build/lagom/ ] ; then
        echo "Directory Build/lagom/ does not exist. Skipping GML formatting."
        exit 0
    fi
    if ! [ -r Build/lagom/gml-format ] ; then
        echo "Lagom executable gml-format was not built. Skipping GML formatting."
        echo "To enable this check, you may need to run './Meta/serenity.sh build lagom' first."
        exit 0
    fi
    GML_FORMAT="Build/lagom/gml-format"
fi

find AK Base Documentation Kernel Meta Ports Tests Userland -type f -name '*.gml' -print0 | xargs -0 "${GML_FORMAT}" -i
