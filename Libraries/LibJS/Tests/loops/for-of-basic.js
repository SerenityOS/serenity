load("test-common.js");

try {
    assertThrowsError(() => {
        for (const _ of 123) {}
    }, {
        error: TypeError,
        message: "for..of right-hand side must be iterable"
    });

    assertThrowsError(() => {
        for (const _ of {foo: 1, bar: 2}) {}
    }, {
        error: TypeError,
        message: "for..of right-hand side must be iterable"
    });

    assertVisitsAll(visit => {
        for (const num of [1, 2, 3]) {
            visit(num);
        }
    }, [1, 2, 3]);

    assertVisitsAll(visit => {
        for (const char of "hello") {
            visit(char);
        }
    }, ["h", "e", "l", "l", "o"]);

    assertVisitsAll(visit => {
        for (const char of new String("hello")) {
            visit(char);
        }
    }, ["h", "e", "l", "l", "o"]);

    var char;
    for (char of "abc");
    assert(char === "c");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
