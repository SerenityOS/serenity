#!/bin/sh

set -e

if [ "$#" -lt "2" ]; then
    echo "USAGE: $0 <file> <cmd...>"
    exit 1
fi

DST_FILE="$1"
shift

# Just in case:
mkdir -p -- "$(dirname -- "${DST_FILE}")"

cleanup()
{
  rm -f -- "${DST_FILE}.tmp"
}
trap cleanup 0 1 2 3 6

"$@" > "${DST_FILE}.tmp"
# If we get here, the command was successful, and we can overwrite the destination.

if ! cmp --quiet -- "${DST_FILE}.tmp" "${DST_FILE}"; then
    # File changed, need to overwrite:
    mv -f -- "${DST_FILE}.tmp" "${DST_FILE}"
fi
