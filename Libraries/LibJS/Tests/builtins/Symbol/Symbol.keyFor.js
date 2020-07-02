load("test-common.js")

try {
    const localSym = Symbol("foo");
    const globalSym = Symbol.for("foo");

    assert(Symbol.keyFor(localSym) === undefined);
    assert(Symbol.keyFor(globalSym) === "foo");

    const testThrows = (value, str) => {
        assertThrowsError(() => {
            Symbol.keyFor(value);
        }, {
            error: TypeError,
            message: str + " is not a symbol",
        });
    };

    testThrows(1, "1");
    testThrows(null, "null");
    testThrows(undefined, "undefined");
    testThrows([], "[object Array]");
    testThrows({}, "[object Object]");
    testThrows(true, "true");
    testThrows("foobar", "foobar");
    testThrows(function(){}, "[object ScriptFunction]"); // FIXME: Better function stringification

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
