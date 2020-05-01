load("test-common.js");

try {
    assert(Reflect.set.length === 3);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.set(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.set() must be an object"
        });
    });

    assert(Reflect.set({}) === true);
    assert(Reflect.set({}, "foo") === true);
    assert(Reflect.set({}, "foo", "bar") === true);

    var o = {};
    assert(o.foo === undefined);
    assert(Reflect.set(o, "foo", 1) === true);
    assert(o.foo === 1);
    assert(Reflect.set(o, "foo", 2) === true);
    assert(o.foo === 2);

    Object.defineProperty(o, "bar", { value: 2, configurable: true, writable: false });
    assert(Reflect.set(o, "bar") === false);
    assert(o.bar === 2);

    Object.defineProperty(o, "baz", { value: 3, configurable: false, writable: true });
    assert(Reflect.set(o, "baz") === true);
    assert(o.baz === undefined);

    var a = [];
    assert(a.length === 0);
    assert(Reflect.set(a, "0") === true);
    assert(a.length === 1);
    assert(a[0] === undefined);
    assert(Reflect.set(a, 1, "foo") === true);
    assert(a.length === 2);
    assert(a[0] === undefined);
    assert(a[1] === "foo");
    assert(Reflect.set(a, 4, "bar") === true);
    assert(a.length === 5);
    assert(a[0] === undefined);
    assert(a[1] === "foo");
    assert(a[2] === undefined);
    assert(a[3] === undefined);
    assert(a[4] === "bar");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
