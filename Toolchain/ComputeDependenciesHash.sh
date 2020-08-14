#!/usr/bin/env bash
set -euo pipefail
# This file will need to be run in bash, for now.

if [ $# -lt 1 ] ; then
    echo "USAGE: echo \"YOURCONFIG\" | $0 <HASH-INVOCATION>" >&2
    echo "Example: echo \"uname=Linux,TARGET=i686-pc-serenity\" | $0 md5sum" >&2
    echo "Example: echo \"uname=OpenBSD,TARGET=i686-pc-serenity\" | $0 md5 -q" >&2
    exit 1
fi

DIR=$( cd "$( dirname "$0" )" && pwd )
cd "${DIR}/.."
if [ ! -r LICENSE ] ; then
    echo "$0: Got confused by the directories, giving up." >&2
    exit 1
fi

# Ensure cleanup
DEPLIST_FILE=$(mktemp /tmp/serenity_deps_XXXXXXXX.lst)
function finish {
    rm -f "${DEPLIST_FILE}"
}
trap finish EXIT

# First, capture the caller's input.
echo "$0: Configuration:" >&2
cat /dev/stdin | tee /dev/stderr > "${DEPLIST_FILE}"
# "$@" is the md5sum invocation.
"$@" Toolchain/ComputeDependenciesHash.sh | tee /dev/stderr >> "${DEPLIST_FILE}"

# libstdc++ depends on the *headers* of libc, so we pessimistically assume it depends
# on *all* of them.
# This list of files can be cut down considerably:
#     strace -ff -e trace=file "make install-target-libstdc++-v3" 2>&1 >/dev/null | perl -ne 's/^[^"]+"(([^\\"]|\\[\\"nt])*)".*/$1/ && print' | sort -u | grep -P 'serenity/Build/Root/usr/include/.*\.h$'
# However, we don't want to risk breaking the build when we upgrade gcc in the future.
#
# If you want to further cut down the Toolchain rebuilds on Travis,
# one way would be to reduce this list somehow.
cd Libraries/LibC/
find -name '*.h' | sort | xargs "$@" | tee /dev/stderr >> "${DEPLIST_FILE}"

# The piping might hide non-zero exit-codes,
# but thankfully only the first command can reasonably fail.
echo "$0: Toolchain hash:" >&2
"$@" "${DEPLIST_FILE}" | cut -f1 -d' ' | tee /dev/stderr

echo "$0: Great success!" >&2
