load("test-common.js");

/**
 * This file tests automatic semicolon insertion rules.
 * If this file produces syntax errors, something is wrong.
 */

function bar() {
    // https://github.com/SerenityOS/serenity/issues/1829
    if (1)
        return 1;
    else
        return 0;

    if (1)
        return 1
    else
        return 0

    if (1)
        return 1
    else
        return 0;
    
}

function foo() {
    label:
    for (var i = 0; i < 4; i++) {
        break // semicolon inserted here
        continue // semicolon inserted here

        break label // semicolon inserted here
        continue label // semicolon inserted here
    }

    var j // semicolon inserted here

    do {
    } while (1 === 2) // semicolon inserted here

    return // semicolon inserted here
    1;
var curly/* semicolon inserted here */}

function baz() {
    let counter = 0;
    let outer;

    outer:
    for (let i = 0; i < 5; ++i) {
        for (let j = 0; j < 5; ++j) {
            continue // semicolon inserted here
            outer // semicolon inserted here
        }
        counter++;
    }

    return counter;
}

try {
    assert(foo() === undefined);
    assert(baz() === 5);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}

// This vardecl must appear exactly at the end of the file (no newline or whitespace after it)
var eof
