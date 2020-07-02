load("test-common.js");

try {
    assert(Reflect.isExtensible.length === 1);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.isExtensible(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.isExtensible() must be an object"
        });
    });

    assert(Reflect.isExtensible({}) === true);

    var o = {};
    o.foo = "foo";
    assert(o.foo === "foo");
    assert(Reflect.isExtensible(o) === true);
    Reflect.preventExtensions(o);
    o.bar = "bar";
    assert(o.bar === undefined);
    assert(Reflect.isExtensible(o) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
