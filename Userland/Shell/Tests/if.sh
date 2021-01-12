#!/bin/sh

setopt --verbose

if test 1 -eq 1 {
    # Are comments ok?
    # Basic 'if' structure, empty block.
    if true {
    } else {
        echo "if true runs false branch"
        exit 2
    }
    if false {
        echo "if false runs true branch"
        exit 2
    } else {
    }

    # Basic 'if' structure, without 'else'
    if false {
        echo "Fail: 'if false' runs the branch"
        exit 2
    }

    # Extended 'cond' form.
    if false {
        echo "Fail: 'if false' with 'else if' runs first branch"
        exit 2
    } else if true {
    } else {
        echo "Fail: 'if false' with 'else if' runs last branch"
        exit 2
    }

    # FIXME: Some form of 'not' would be nice
    # &&/|| in condition
    if true || false {
    } else {
        echo "Fail: 'if true || false' runs false branch"
        exit 2
    }

    if true && false {
        echo "Fail: 'if true && false' runs true branch"
        exit 2
    }
} else {
    echo "Fail: 'if test 1 -eq 1' runs false branch"
    exit 1
}
