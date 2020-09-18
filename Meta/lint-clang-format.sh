#!/bin/bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

CLANG_FORMAT=false
if command -v clang-format-10 >/dev/null 2>&1 ; then
    CLANG_FORMAT=clang-format-10
elif command -v clang-format >/dev/null 2>&1 ; then
    CLANG_FORMAT=clang-format
    if ! "${CLANG_FORMAT}" --version | grep -qF ' 10.' ; then
        echo "You are using '$("${CLANG_FORMAT}" --version)', which appears to not be clang-format 10."
        echo "It is very likely that the resulting changes are not what you wanted."
    fi
else
    echo "clang-format-10 is not available. Either skip this script, or install clang-format-10."
    echo "(If you install a package 'clang-format', please make sure it's version 10.)"
    exit 1
fi

if [ "$#" -eq "1" ] && [ "x--overwrite-inplace" = "x$1" ] ; then
    true # The only way to run this script.
else
    # Note that this branch also covers --help, -h, -help, -?, etc.
    echo "USAGE: $0 --overwrite-inplace"
    echo "The argument is necessary to make you aware that this *will* overwrite your local files."
    exit 1
fi

echo "Running ${CLANG_FORMAT} ..."

{
    git ls-files -- \
        '*.cpp' \
        '*.h' \
        ':!:Base' \
        ':!:Kernel/Arch/i386/CPU.cpp' \
        ':!:Kernel/FileSystem/ext2_fs.h' \
        ':!:Libraries/LibC/getopt.cpp' \
        ':!:Libraries/LibC/syslog.h' \
        ':!:Libraries/LibCore/puff.h' \
        ':!:Libraries/LibCore/puff.cpp' \
        ':!:Libraries/LibELF/exec_elf.h' \
    || echo "'git ls-files failed!'"
} | xargs -d'\n' clang-format-10 -style=file -i

echo "Maybe some files have changed. Sorry, but clang-format doesn't indicate what happened."
