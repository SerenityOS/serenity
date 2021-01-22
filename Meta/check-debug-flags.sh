#!/usr/bin/env bash

set -eo pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

MISSING_FLAGS=n

while IFS= read -r FLAG; do
    # We simply search whether the CMakeLists.txt *ever* sets the flag.
    # There are (basically) no false positives, but there might be false negatives,
    # for example we intentionally don't check for commented-out lines here.
    if ! grep -qP "add_compile_definitions\(\"${FLAG}" Meta/CMake/all_the_debug_macros.cmake ; then
        echo "ALL_THE_DEBUG_MACROS probably doesn't include ${FLAG}"
        MISSING_FLAGS=y
    fi
done < <(
    git ls-files -- \
        '*.cpp' \
        '*.h' \
        ':!:Kernel/FileSystem/ext2_fs.h' \
        ':!:Userland/Libraries/LibELF/exec_elf.h' \
    | xargs grep -P '^ *#.*DEBUG' \
    | sed -re 's,^.*[^a-zA-Z0-9_]([a-zA-Z0-9_]*DEBUG[a-zA-Z0-9_]*).*$,\1,' \
    | sort \
    | uniq
)

if [ "n" != "${MISSING_FLAGS}" ] ; then
    echo "Some flags are missing for the ALL_THE_DEBUG_MACROS feature in CMakeLists.txt."
    echo "If you just added a new SOMETHING_DEBUG flag, that's great!"
    echo "We want to enable all of these in automated builds, so that the code doesn't rot."
    echo "Please add it to the ALL_THE_DEBUG_MACROS section."
    exit 1
fi
