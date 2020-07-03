#!/bin/bash

if [ "$(uname)" = "SerenityOS" ]; then
    js_program=/bin/test-js
    test_root=/home/anon/js-tests
else
    [ -z "$js_program" ] && js_program="$SERENITY_ROOT/Build/Meta/Lagom/test-js"
    test_root="$SERENITY_ROOT/Libraries/LibJS/Tests"

    # Enable back traces if sanitizers are enabled
    export UBSAN_OPTIONS=print_stacktrace=1
fi

# FIXME: Support "find -name" in Serenity to remove the file name checks below
test_files_tmp=$(find . -type f | cut -b 3- | sort)

for f in $test_files_tmp; do
    if [ "$f" = "test-common.js" ] || [ "$f" = "run-tests.sh" ]; then
        continue
    fi
    test_files=("${test_files[@]}" "$f");
done

$js_program "$test_root" "${test_files[@]}"

exit $!
