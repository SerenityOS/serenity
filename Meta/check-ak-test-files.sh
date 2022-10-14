#!/usr/bin/env bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

MISSING_FILES=n

while IFS= read -r FILENAME; do
    # Simply search whether the CMakeLists.txt *ever* mention the test files.
    if ! grep -qF "${FILENAME}" Tests/AK/CMakeLists.txt ; then
        echo "Tests/AK/CMakeLists.txt is either missing the test file ${FILENAME} or is not in the commit's staged changes"
        MISSING_FILES=y
    fi
done < <(
    git ls-files 'Tests/AK/Test*.cpp' | xargs -n1 basename
)

if [ "n" != "${MISSING_FILES}" ] ; then
    echo "Some files are missing from the Tests/AK/CMakeLists.txt."
    echo "If a new test file is being added, ensure it is in the CMakeLists.txt."
    echo "This ensures the new tests are always run."
    exit 1
fi
