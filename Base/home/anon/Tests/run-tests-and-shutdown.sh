#!/bin/Shell
# shellcheck disable=all

echo
echo "==== Running Tests on SerenityOS ===="

echo "architecture is: >>$(uname -m)<<"
if [ "$(uname -m)" = "AArch64" ] && [ "$1" != "--force" ] {
    fail_count=0
}
else {
    export LLVM_PROFILE_FILE="$HOME/profiles/%p-profile.profraw"
    run-tests --show-progress=false --unlink-coredumps
    fail_count=$?
    unset LLVM_PROFILE_FILE
}

echo "Failed: $fail_count" > ./test-results.log

if test $DO_SHUTDOWN_AFTER_TESTS {
    sync
    shutdown -n
}

exit $fail_count
