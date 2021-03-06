#!/bin/sh

echo "==== Running Tests on SerenityOS ===="

run(index) {
    shift
    test_cmd=($*)
    echo "Running test $index out of $count_of_all_tests -- $test_cmd"
    if $test_cmd {
        echo "::debug file=$test_cmd:: $test_cmd passed!"
    } else {
        echo "::error file=$test_cmd:: $test_cmd returned non-zero exit code, check logs!"
    }
}

# TODO: test-web requires the window server
system_tests=((test-js --show-progress=false) test-pthread test-compress /usr/Tests/LibM/test-math (test-crypto bigint -t))
# FIXME: Running too much at once is likely to run into #5541. Remove commented out find below when stable
all_tests=${concat_lists $system_tests} #$(find /usr/Tests -type f | grep -v Kernel | grep -v .inc | shuf))
count_of_all_tests=${length $all_tests}

for index i cmd in $all_tests {
    run $(expr $i + 1) $cmd
}

echo "==== Done running tests ===="

if test $DO_SHUTDOWN_AFTER_TESTS {
    shutdown -n
}
