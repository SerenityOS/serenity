#!/bin/Shell

source $(dirname "$0")/test-commons.inc

setopt --verbose

rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
pushd /tmp/shell-test

    # Simple sequence (grouping)
    { echo test > testfile }
    if not test "$(cat testfile)" = "test" {
        fail cannot write to file in subshell
    }

    # Simple sequence - many commands
    { echo test1 > testfile; echo test2 > testfile }
    if not test "$(cat testfile)" = "test2" {
        fail cannot write to file in subshell 2
    }

    # Does it exit with the last exit code?
    { test -z "a" }
    exitcode=$?
    if not test "$exitcode" -eq 1 {
        fail exits with $exitcode when it should exit with 1
    }

    { test -z "a" || echo test }
    exitcode=$?
    if not test "$exitcode" -eq 0 {
        fail exits with $exitcode when it should exit with 0
    }

popd
rm -rf /tmp/shell-test

echo PASS
