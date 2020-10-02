#!/bin/sh

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.." || exit 1

# The __cxa_guard_* calls are generated for (non trivial) initialization of local static objects.
# These symbols are OK to use within serenity code, but they are problematic in LibC because their
# existence breaks ports (the implementation of these symbols resides in libstdc++.a which we do not link against in ports).
# To eliminate the need for these symbols, avoid doing non-trivial construction of local statics in LibC.

FORBIDDEN_SYMBOLS="__cxa_guard_acquire __cxa_guard_release"
LIBC_PATH="Build/Libraries/LibC/libc.a" 
for forbidden_symbol in $FORBIDDEN_SYMBOLS; do
    # check if symbol is undefined
    if nm $LIBC_PATH | grep "U $forbidden_symbol" ; then
        echo "Forbidden undefined symbol in LibC: $forbidden_symbol"
        echo "See comment in Meta/check-symbols.sh for more info"
        exit 1
    fi
done
