load("test-common.js");

/**
 * This file tests automatic semicolon insertion rules.
 * If this file produces syntax errors, something is wrong.
 */

function foo() {
    for (var i = 0; i < 4; i++) {
        break // semicolon inserted here
        continue // semicolon inserted here
    }

    var j // semicolon inserted here

    do {
    } while (1 === 2) // semicolon inserted here

    return // semicolon inserted here
    1;
var curly/* semicolon inserted here */}

try {
    assert(foo() === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}

// This vardecl must appear exactly at the end of the file (no newline or whitespace after it)
var eof
