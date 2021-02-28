#!/bin/sh

echo "==== Running Tests on SerenityOS ===="

run() {
    test_cmd=($*)
    echo "Running test -- $test_cmd"
    if $test_cmd {
        echo "::debug file=$test_cmd:: $test_cmd passed!"
    } else {
        echo "::error file=$test_cmd:: $test_cmd returned non-zero exit code, check logs!"
    }
}

# TODO: It'd be nice to have a list+list op (as opposed to nest-on-in-another)
# TODO: It'd be nice to have a list.length or enumerate(list) operation to allow terminal progress bar
# TODO: test-web requires the window server
system_tests=(test-js test-pthread test-compress /usr/Tests/LibM/test-math (test-crypto bigint -t))
# FIXME: Running too much at once is likely to run into #5541. Remove commented out find below when stable
all_tests=($system_tests) #$(find /usr/Tests -type f | grep -v Kernel | grep -v .inc | shuf))

for list in $all_tests {
    for $list { run $it }
}

echo "==== Done running tests ===="

if test $DO_SHUTDOWN_AFTER_TESTS {
    shutdown -n
}
