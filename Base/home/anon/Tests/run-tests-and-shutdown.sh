#!/bin/sh

echo
echo "==== Running Tests on SerenityOS ===="

export LLVM_PROFILE_FILE="$HOME/profiles/%p-profile.profraw"

run-tests --show-progress=false
fail_count=$?

unset LLVM_PROFILE_FILE

echo "Failed: $fail_count" > ./test-results.log

if test $DO_SHUTDOWN_AFTER_TESTS {
    shutdown -n
}

exit $fail_count
