load("test-common.js");

try {
    assert(Reflect.getOwnPropertyDescriptor.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.getOwnPropertyDescriptor(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.getOwnPropertyDescriptor() must be an object"
        });
    });

    assert(Reflect.getOwnPropertyDescriptor({}) === undefined);
    assert(Reflect.getOwnPropertyDescriptor({}, "foo") === undefined);

    var o = { foo: "bar" };
    var d = Reflect.getOwnPropertyDescriptor(o, "foo");
    assert(d.value === "bar");
    assert(d.writable === true);
    assert(d.enumerable === true);
    assert(d.configurable === true);

    var a = [];
    d = Reflect.getOwnPropertyDescriptor(a, "length");
    assert(d.value === 0);
    assert(d.writable === true);
    assert(d.enumerable === false);
    assert(d.configurable === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
