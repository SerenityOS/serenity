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

$js_program "$test_root"

exit $!
