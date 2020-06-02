load("test-common.js");

try {
    assert(Object.setPrototypeOf.length === 2);

    assertThrowsError(() => {
        Object.setPrototypeOf({}, "foo");
    }, {
        error: TypeError,
        message: "Prototype must be null or object"
    });

    o = {};
    assert(Object.setPrototypeOf(o, {}) === o);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
