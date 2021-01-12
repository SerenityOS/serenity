#!/bin/sh

rm -rf shell-test
mkdir -p shell-test
cd shell-test

    time sleep 1 2>timeerr >timeout
    cat timeout
    # We cannot be sure about the values, so just assert that they're not empty.
    test -n "$(cat timeerr)" || echo "Failure: 'time' stderr output not redirected correctly" && exit 1
    test -e timeout || echo "Failure: 'time' stdout output not redirected correctly" && exit 1

    time ls 2> /dev/null | head > timeout
    test -n "$(cat timeout)" || echo "Failure: 'time' stdout not piped correctly" && exit 1

cd ..
rm -rf shell-test # TODO: Remove this file at the end once we have `trap'
