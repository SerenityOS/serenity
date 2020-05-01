load("test-common.js");

try {
    assert(Reflect.deleteProperty.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.deleteProperty(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.deleteProperty() must be an object"
        });
    });

    assert(Reflect.deleteProperty({}) === true);
    assert(Reflect.deleteProperty({}, "foo") === true);

    var o = { foo: 1 };
    assert(o.foo === 1);
    assert(Reflect.deleteProperty(o, "foo") === true);
    assert(o.foo === undefined);
    assert(Reflect.deleteProperty(o, "foo") === true);
    assert(o.foo === undefined);

    Object.defineProperty(o, "bar", { value: 2, configurable: true, writable: false });
    assert(Reflect.deleteProperty(o, "bar") === true);
    assert(o.bar === undefined);

    Object.defineProperty(o, "baz", { value: 3, configurable: false, writable: true });
    assert(Reflect.deleteProperty(o, "baz") === false);
    assert(o.baz === 3);

    var a = [1, 2, 3];
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(Reflect.deleteProperty(a, 1) === true);
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === undefined);
    assert(a[2] === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
