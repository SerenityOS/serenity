load("test-common.js");

try {
    assert(Reflect.setPrototypeOf.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.setPrototypeOf(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.setPrototypeOf() must be an object"
        });
        if (value === null)
            return;
        assertThrowsError(() => {
            Reflect.setPrototypeOf({}, value);
        }, {
            error: TypeError,
            message: "Prototype must be an object or null"
        });
    });

    assert(Reflect.setPrototypeOf({}, null) === true);
    assert(Reflect.setPrototypeOf({}, {}) === true);
    assert(Reflect.setPrototypeOf({}, Object.prototype) === true);
    assert(Reflect.setPrototypeOf({}, Array.prototype) === true);
    assert(Reflect.setPrototypeOf({}, String.prototype) === true);
    assert(Reflect.setPrototypeOf({}, Reflect.getPrototypeOf({})) === true);

    var o = {};
    assert(o.foo === undefined);
    assert(Reflect.setPrototypeOf(o, { foo: "bar" }) === true);
    assert(o.foo === "bar");


    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
