load("test-common.js");

try {
    ["foo", 123, null, undefined].forEach(value => {
        assertThrowsError(() => {
            "prop" in value;
        }, {
            error: TypeError,
            message: "'in' operator must be used on object"
        });
    });

    var o = {foo: "bar", bar: undefined};
    assert("" in o === false);
    assert("foo" in o === true);
    assert("bar" in o === true);
    assert("baz" in o === false);
    assert("toString" in o === true);

    var a = ["hello", "friends"];
    assert(0 in a === true);
    assert(1 in a === true);
    assert(2 in a === false);
    assert("0" in a === true);
    assert("hello" in a === false);
    assert("friends" in a === false);
    assert("length" in a === true);

    var s = new String("foo");
    assert("length" in s);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
