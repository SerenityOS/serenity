#!/bin/shell

source $(dirname "$0")/test-commons.inc

setopt --verbose

if test 1 -eq 1 {
    # Are comments ok?
    # Basic 'if' structure, empty block.
    if true {
    } else {
        fail "if true runs false branch"
    }
    if false {
        fail "if false runs true branch"
    } else {
    }

    # Can we put `else` on a new line?
    if false {
        echo "if false runs true branch"
        exit 2
    }
    else {
    }

    # Basic 'if' structure, without 'else'
    if false {
        fail "'if false' runs the branch"
    }

    # Extended 'cond' form.
    if false {
        fail "'if false' with 'else if' runs first branch"
    } else if true {
    } else {
        fail "'if false' with 'else if' runs last branch"
    }

    # FIXME: Some form of 'not' would be nice
    # &&/|| in condition
    if true || false {
    } else {
        fail "'if true || false' runs false branch"
    }

    if true && false {
        fail "'if true && false' runs true branch"
    }
} else {
    fail "'if test 1 -eq 1' runs false branch"
}

echo PASS
