#!/bin/Shell

source $(dirname "$0")/test-commons.inc

rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
pushd /tmp/shell-test

    time sleep 1 2>timeerr >timeout
    cat timeout
    # We cannot be sure about the values, so just assert that they're not empty.
    if not test -n "$(cat timeerr)" { fail "'time' stderr output not redirected correctly" }
    if not test -e timeout { fail "'time' stdout output not redirected correctly" }

    time ls 2> /dev/null | head > timeout
    if not test -n "$(cat timeout)" { fail "'time' stdout not piped correctly" }

popd
rm -rf /tmp/shell-test # TODO (#7339): Remove this file at the end once we have `trap'

echo PASS
