load("test-common.js");

try {
    assert(Reflect.getPrototypeOf.length === 1);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.getPrototypeOf(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.getPrototypeOf() must be an object"
        });
    });

    assert(Reflect.getPrototypeOf({}) === Object.prototype);
    assert(Reflect.getPrototypeOf([]) === Array.prototype);
    assert(Reflect.getPrototypeOf(new String()) === String.prototype);

    var o = {};
    Reflect.setPrototypeOf(o, { foo: "bar" });
    assert(Reflect.getPrototypeOf(o).foo === "bar");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
