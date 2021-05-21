#!/bin/sh

echo
echo "==== Running Tests on SerenityOS ===="

run(index) {
    shift
    test_cmd=($*)
    echo "==== Running test $index out of $count_of_all_tests -- $test_cmd ===="
    echo "::group::$test_cmd"
    if $test_cmd {
        echo "::endgroup::"
        echo "==== $test_cmd passed! ===="
    } else {
        echo "::endgroup::"
        failed_tests=${concat_lists $failed_tests ($test_cmd)}
        echo "==== Added $test_cmd to list of failed tests. Failed tests so far: $failed_tests ===="
        echo "::error file=$test_cmd:: $test_cmd returned non-zero exit code, check logs!"
    }
}

# Files in /usr/Tests/* that we don't want to execute match these patterns:
#     Kernel/Legacy: Kernel tests that are either old patched exploits, or hang the system/shell
#     .inc: Shell test helper file that's not a test
#     UserEmulator: Tests designed to run inside the Userspace Emulator
#     stack-smash: Intentionally crashes by smashing the stack
#     TestJSON: AK/TestJSON makes assumptions about $PWD to load its input files
#     .frm: Test inputs that are not tests
#     test-web: Requires the window server in order to work
#     test-js: We start this one manually with the show progress flag set to false
exclude_patterns='Kernel/Legacy|.inc|UserEmulator|stack-smash|TestJSON|.frm|test-web|test-js'

system_tests=((test-js --show-progress=false) (test-crypto -c -t test))
all_tests=${concat_lists $system_tests $(find /usr/Tests -type f | grep -E -v $exclude_patterns | shuf) }
count_of_all_tests=${length $all_tests}
failed_tests=()

for index i cmd in $all_tests {
    run $(expr $i + 1) $cmd
}

fail_count=${length $failed_tests}

echo "==== Out of $count_of_all_tests tests, $(expr $count_of_all_tests - $fail_count) passed and $fail_count failed. ===="

if test $fail_count -gt 0 {
    echo "==== Failing tests: $failed_tests ===="
}

echo "Failed: $fail_count" > ./test-results.log

if test $DO_SHUTDOWN_AFTER_TESTS {
    shutdown -n
}

exit $fail_count
