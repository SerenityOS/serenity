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
    p = {};
    assert(Object.setPrototypeOf(o, p) === o);

    Object.preventExtensions(o);
    assertThrowsError(() => {
        Object.setPrototypeOf(o, {});
    }, {
        error: TypeError,
        message: "Can't set prototype of non-extensible object"
    });
    assert(Object.setPrototypeOf(o, p) === o);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
