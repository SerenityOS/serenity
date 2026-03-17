#!/usr/bin/env bash

set -euo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SERENITY_ROOT="$(realpath "${DIR}"/../../..)"

cd "$SERENITY_ROOT/Ports/os-test"
./package.sh
