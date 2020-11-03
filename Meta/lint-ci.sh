#!/bin/bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

for cmd in \
        Meta/check-style.sh \
        Meta/lint-executable-resources.sh \
        Meta/lint-ipc-ids.sh \
        Meta/lint-shell-scripts.sh ; do
    echo "Running $cmd"
    "$cmd"
    echo "$cmd successful"
done

echo "Running Meta/lint-clang-format.sh"
Meta/lint-clang-format.sh --overwrite-inplace && git diff --exit-code
echo "Meta/lint-clang-format.sh successful"
echo "(Not running lint-missing-resources.sh due to high false-positive rate.)"
echo "(Also look out for check-symbols.sh, which can only be executed after the build!)"
