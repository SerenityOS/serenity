#!/bin/Shell
# shellcheck disable=all

echo
echo "==== Running Tests on SerenityOS ===="

export LLVM_PROFILE_FILE="$HOME/profiles/%p-profile.profraw"

echo "architecture is: >>$(uname -m)<<"
skip_args=()
if [ "$(uname -m)" = "AArch64" ] {
    # FIXME: This is just temporary. Without this, Aarch64 breaks CI for everyone.
    skip_args=("-e" "^/usr/Tests/(AK/TestSIMD|Kernel/TestMemoryDeviceMmap|Kernel/crash|LibC/TestAbort|LibC/TestLibCSetjmp|LibC/TestLibCTime|LibC/TestMath|LibGfx/TestDeltaE|LibGfx/TestICCProfile|LibTLS/TestTLSHandshake|LibVideo/TestVP9Decode|LibWeb/TestCSSIDSpeed|LibWeb/TestHTMLTokenizer|test-js/test-js|test-spreadsheet/test-spreadsheet|test-wasm/test-wasm)\$")
}
echo "Skip args is" $skip_args
run-tests $skip_args --show-progress=false --unlink-coredumps
fail_count=$?

unset LLVM_PROFILE_FILE

echo "Failed: $fail_count" > ./test-results.log

if test $DO_SHUTDOWN_AFTER_TESTS {
    sync
    shutdown -n
}

exit $fail_count
