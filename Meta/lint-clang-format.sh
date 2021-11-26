#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

if [ "$#" -eq "1" ]; then
    mapfile -t files < <(
        git ls-files -- \
            '*.cpp' \
            '*.h' \
            ':!:Base' \
            ':!:Kernel/FileSystem/ext2_fs.h' \
            ':!:Userland/DevTools/HackStudio/LanguageServers/Cpp/Tests/*' \
            ':!:Userland/Libraries/LibCpp/Tests/parser/*' \
            ':!:Userland/Libraries/LibCpp/Tests/preprocessor/*'
    )
else
    files=()
    for file in "${@:2}"; do
        if [[ "${file}" == *".cpp" || "${file}" == *".h" ]]; then
            files+=("${file}")
        fi
    done
fi

if (( ${#files[@]} )); then
    CLANG_FORMAT=false
    if command -v clang-format-11 >/dev/null 2>&1 ; then
        CLANG_FORMAT=clang-format-11
    elif command -v clang-format >/dev/null 2>&1 ; then
        CLANG_FORMAT=clang-format
        if ! "${CLANG_FORMAT}" --version | awk '{ if (substr($NF, 1, index($NF, ".") - 1) < 11) exit 1; }'; then
            echo "You are using '$("${CLANG_FORMAT}" --version)', which appears to not be clang-format 11 or later."
            echo "It is very likely that the resulting changes are not what you wanted."
        fi
    else
        echo "clang-format-11 is not available, but C or C++ files need linting! Either skip this script, or install clang-format-11."
        echo "(If you install a package 'clang-format', please make sure it's version 11 or later.)"
        exit 1
    fi

    if [ "$#" -gt "0" ] && [ "--overwrite-inplace" = "$1" ] ; then
        true # The only way to run this script.
    else
        # Note that this branch also covers --help, -h, -help, -?, etc.
        echo "USAGE: $0 --overwrite-inplace"
        echo "The argument is necessary to make you aware that this *will* overwrite your local files."
        exit 1
    fi

    echo "Using ${CLANG_FORMAT}"

    "${CLANG_FORMAT}" -style=file -i "${files[@]}"
    echo "Maybe some files have changed. Sorry, but clang-format doesn't indicate what happened."
else
    echo "No .cpp or .h files to check."
fi
