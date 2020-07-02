load("test-common.js");

try {
    assert(Reflect.defineProperty.length === 3);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.defineProperty(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.defineProperty() must be an object"
        });

        assertThrowsError(() => {
            Reflect.defineProperty({}, "foo", value);
        }, {
            error: TypeError,
            message: "Descriptor argument is not an object"
        });
    });

    var o = {};

    assert(Reflect.defineProperty(o, "foo", { value: 1, writable: false, enumerable: false }) === true);
    assert(o.foo === 1);
    o.foo = 2;
    assert(o.foo === 1);

    assert(Reflect.defineProperty(o, "bar", { value: "hi", writable: true, enumerable: true }) === true);
    assert(o.bar === "hi");
    o.bar = "ho";
    assert(o.bar === "ho");

    assert(Reflect.defineProperty(o, "bar", { value: "xx", enumerable: false }) === false);

    var d = Reflect.getOwnPropertyDescriptor(o, "foo");
    assert(d.configurable === false);
    assert(d.enumerable === false);
    assert(d.writable === false);
    assert(d.value === 1);

    d = Reflect.getOwnPropertyDescriptor(o, "bar");
    assert(d.configurable === false);
    assert(d.enumerable === true);
    assert(d.writable === true);
    assert(d.value === "ho");

    assert(Reflect.defineProperty(o, "baz", { value: 9, configurable: true, writable: false }) === true);
    assert(Reflect.defineProperty(o, "baz", { configurable: true, writable: true }) === true);

    d = Reflect.getOwnPropertyDescriptor(o, "baz");
    assert(d.configurable === true);
    assert(d.writable === true);
    assert(d.value === 9);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
