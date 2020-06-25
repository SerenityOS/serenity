#!/bin/bash

if [ "$(uname)" = "SerenityOS" ]; then
    js_program=/bin/js
else
    [ -z "$js_program" ] && js_program="$SERENITY_ROOT/Build/Meta/Lagom/js"

    # Enable back traces if sanitizers are enabled
    export UBSAN_OPTIONS=print_stacktrace=1
fi

pass_count=0
fail_count=0
count=0
test_count=0

GLOBIGNORE=test-common.js

for f in *.js; do
    (( ++test_count ))
done

for f in *.js; do
    result="$("$js_program" "$@" -t "$f" 2>/dev/null)"
    if [ "$result" = "PASS" ]; then
        (( ++pass_count ))
        echo -ne "( \033[32;1mPass\033[0m ) "
    else
        echo -ne "( \033[31;1mFail\033[0m ) "
        (( ++fail_count ))
    fi
    echo -ne "\033]9;${count};${test_count}\033\\"
    echo "$f"

    if [ "$result" != "PASS" ]; then
        if [ -z "$result" ]; then
            echo -e "    \033[31;1mNo output. Did you forget 'console.log(\"PASS\");'?\033[0m"
        else
            readarray -t split_result <<< "$result";

            echo -ne "    \033[31;1mOutput:\033[0m: "
            echo "${split_result[0]}";

            for (( i = 1; i < ${#split_result[@]}; i++ )); do
                echo "             ${split_result[i]}"
            done
        fi
    fi

    (( ++count ))
done

echo -e "\033]9;-1\033\\"

pass_color=""
fail_color=""
color_off="\033[0m"

exit_code=0

if (( pass_count > 0 )); then
    pass_color="\033[32;1m"
fi

if (( fail_count > 0 )); then
    fail_color="\033[31;1m"
    exit_code=1
fi

echo
echo -e "Ran $count tests. Passed: ${pass_color}${pass_count}${color_off}, Failed: ${fail_color}${fail_count}${color_off}"

exit $exit_code
