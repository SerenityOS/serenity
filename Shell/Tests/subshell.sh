#/bin/sh

setopt --verbose

rm -rf shell-test
mkdir shell-test
cd shell-test

    # Simple sequence (grouping)
    { echo test > testfile }
    test "$(cat testfile)" = "test" || echo cannot write to file in subshell && exit 1

    # Simple sequence - many commands
    { echo test1 > testfile; echo test2 > testfile }
    test "$(cat testfile)" = "test2" || echo cannot write to file in subshell 2 && exit 1


    # Does it exit with the last exit code?
    { test -z "a" }
    exitcode=$?
    test "$exitcode" -eq 1 || echo exits with $exitcode when it should exit with 1 && exit 1

    { test -z "a" || echo test }
    exitcode=$?
    test "$exitcode" -eq 0 || echo exits with $exitcode when it should exit with 0 && exit 1

cd ..
rm -rf shell-test
