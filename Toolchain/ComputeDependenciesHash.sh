#!/usr/bin/env bash
set -eu
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

# Patches and the scripts are the only things that affect the build of the toolchain
FILE_LIST=$(find Toolchain/Patches -name "*.patch")

# Sort for reproducability,
FILE_LIST=$(echo "${FILE_LIST}" | LC_ALL=C sort -u)

echo "$0: Computing hashes" >&2
# "$@" is the md5sum invocation. The piping might hide non-zero exit-codes,
# but thankfully only the first command can reasonably fail.
# Also, abuse the deplist file as a temporary buffer.
cat /dev/stdin > "${DEPLIST_FILE}"
HASHES=$(xargs "$@" <<EOLIST
${FILE_LIST}
Toolchain/ComputeDependenciesHash.sh
${DEPLIST_FILE}
EOLIST
)
# Caller (probably BuildIt.sh) should inject it's own hash via stdin.

# Mask the temporary (= non-reproducable) name of the DEPLIST_FILE:
HASHES=$(echo "${HASHES}" | sed -re 's,/tmp/serenity_deps_........\.lst,CONFIG,')

echo "$0: Hashes are:" >&2
echo "${HASHES}" >&2
echo "$0: Toolchain hash:" >&2
cat <<EOHASH | "$@" - | cut -f1 -d' ' | tee /dev/stderr
${HASHES}
EOHASH

echo "$0: Great success!" >&2
