#!/bin/Shell

__proxy() {
    echo '{"kind":"proxy","argv":"'"${regex_replace '"' '\"' "$*"}"'"}'
}

# Builtins
_complete_time() {
    shift 2
    argsparser_parse \
        --add-option _ --help-string "Number of iterations" \
          --long-name iterations --short-name n --value-name iterations --type u32 \
        --add-positional-argument argv --help-string _ \
          --value-name _ --min 0 --max 9999999 \
        --stop-on-first-non-option \
        -- $*
    __proxy $argv
}

# Utilities
_complete_pls() {
    shift 2
    argsparser_parse \
        --add-option _ --help-string "User to execute as" --short-name u --value-name UID \
        --add-positional-argument argv --help-string "Command to run at elevated privilege level" \
          --value-name command --min 0 --max 999999 \
        --stop-on-first-non-option \
        -- $*
    __proxy $argv
}
