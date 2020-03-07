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

# libstdc++ depends on libc and libm, so we pessimistically assume it depends
# on *all* of their implementation and recursive dependencies.
# Scan all files for potential dependencies.
# Thinking in graphs, this computes the edge list:
cat <(find AK/ Libraries/ Servers/ Kernel/ -name '*.h') \
    <(find Libraries/LibC/ Libraries/LibM/ -name '*.cpp' ! -name 'Test*.cpp' ) | \
    xargs grep -F '#include ' | \
    sed -r \
        -e 's,^(.*/)([^/]+:)#include "(.*)",\1\2\1\3,' \
        -e 's^#include <(Kernel/.*)>^\1^' \
        -e 's^#include <(AK/.*)>^\1^' \
        -e 's^#include <(Lib[A-Za-z]+/.*)>^Libraries/\1^' \
        -e 's^#include <((bits|netinet|sys|arpa|net)/.*)>^Libraries/LibC/\1^' \
        -e 's^#include <fd_set.h>^Libraries/LibC/fd_set.h^' \
        -e 's^#include <([a-z]{3,10}(_numbers)?\.h)>^Libraries/LibC/\1^' \
        -e 's^#include <([A-Z][a-z]+Server/.*)>^Servers/\1^' \
        -e 's^#include <(.*)>^UNRESOLVED_I/\1^' \
        -e 's^#include "(.*)"^UNRESOLVED_L/\1^' > "${DEPLIST_FILE}"
# Some #include's cannot be resolved, like <chrono>. However, these are only
# a problem if they turn up as a transitive dependency of libc and libm.
# We will check for that when the time comes.

# The initial guess is pessimistic: *all* of libc and libm.
FILE_LIST=$(find Libraries/LibC/ Libraries/LibM/ \( -name '*.cpp' -o -name '*.c' -o -name '*.h' \) ! -name 'Test*')
echo "$0: Exploring dependencies of libstdc++" >&2
FILE_LIST_COMPLETE="n"
# In each iteration, we extend FILE_LIST by the dependencies not listed yet in
# FILE_LIST.  Note that the results are always semantically the same,
# but the order depends on the initial `find` runs.
for _ in $(seq 10) ; do
    FILE_REGEX=$(echo "${FILE_LIST}" | sed -zr -e 's,\n$,,' -e 's,\.,\\.,g' -e 's,\n,|,g')
    FURTHER_FILE_LIST=$(grep -P "^(${FILE_REGEX}):" "${DEPLIST_FILE}" | grep -Pv ":(${FILE_REGEX})\$" | sed -re 's,^.*:(.*)$,\1,' | sort -u)
    if [ -n "${FURTHER_FILE_LIST}" ] ; then
        # FILE_LIST should grow to a maximum of "number of all .cpp and .c and .h files",
        # i.e. roughly 700 lines.  This should be managable, even as the project grows.
        FILE_LIST="${FILE_LIST}
${FURTHER_FILE_LIST}"
    else
        FILE_LIST_COMPLETE="y"
        break
    fi
done
FURTHER_FILE_LIST=""
FILE_REGEX=""
if [ "${FILE_LIST_COMPLETE}" != "y" ] ; then
    # Dependency chains might grow very long. Also, if for some reason we fail
    # to filter out the already listed files, the FILE_LIST would grow
    # exponentially. Both of these unpleasant cases are handled by capping the
    # iteration count to 10 and giving up:
    echo "$0: Dependencies don't seem to converge, giving up." >&2
    exit 1
fi

# Sort for reproducability,
FILE_LIST=$(echo "${FILE_LIST}" | LC_ALL=C sort -u)
if grep -F 'UNRESOLVED' <<EOLIST >&2 ; then
${FILE_LIST}
EOLIST
    echo "$0: Unresolved dependency, giving up."
    exit 1
fi

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
