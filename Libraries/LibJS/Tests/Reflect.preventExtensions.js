load("test-common.js");

try {
    assert(Reflect.preventExtensions.length === 1);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.preventExtensions(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.preventExtensions() must be an object"
        });
    });

    var o = {};
    assert(Reflect.isExtensible(o) === true);
    assert(Reflect.preventExtensions(o) === true);
    assert(Reflect.isExtensible(o) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
