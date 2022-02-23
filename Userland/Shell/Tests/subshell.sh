#!/bin/sh

source $(dirname "$0")/test-commons.inc

setopt --verbose

rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
pushd /tmp/shell-test

    # Simple sequence (grouping)
    { echo test > testfile }
    if not internal:string_equal "$(cat testfile)" "test" {
        fail cannot write to file in subshell
    }

    # Simple sequence - many commands
    { echo test1 > testfile; echo test2 > testfile }
    if not internal:string_equal "$(cat testfile)" "test2" {
        fail cannot write to file in subshell 2
    }

    # Does it exit with the last exit code?
    { internal:string_equal "" "a" }
    exitcode=$?
    if not internal:number_equal "$exitcode" 1 {
        fail exits with $exitcode when it should exit with 1
    }

    { internal:string_equal "" "a" || echo test }
    exitcode=$?
    if not internal:number_equal "$exitcode" 0 {
        fail exits with $exitcode when it should exit with 0
    }

popd
rm -rf /tmp/shell-test

echo PASS
