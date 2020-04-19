load("test-common.js");

try {
    var o = {};
    Object.defineProperty(o, "foo", { value: 1, writable: false, enumerable: false });

    assert(o.foo === 1);
    o.foo = 2;
    assert(o.foo === 1);

    var d = Object.getOwnPropertyDescriptor(o, "foo");
    assert(d.configurable === false);
    assert(d.enumerable === false);
    assert(d.writable === false);
    assert(d.value === 1);

    Object.defineProperty(o, "bar", { value: "hi", writable: true, enumerable: true });

    assert(o.bar === "hi");
    o.bar = "ho";
    assert(o.bar === "ho");

    d = Object.getOwnPropertyDescriptor(o, "bar");
    assert(d.configurable === false);
    assert(d.enumerable === true);
    assert(d.writable === true);
    assert(d.value === "ho");

    assertThrowsError(() => {
        Object.defineProperty(o, "bar", { value: "xx", enumerable: false });
    }, {
        error: TypeError
    });

    Object.defineProperty(o, "baz", { value: 9, configurable: true, writable: false });
    Object.defineProperty(o, "baz", { configurable: true, writable: true });

    d = Object.getOwnPropertyDescriptor(o, "baz");
    assert(d.configurable === true);
    assert(d.writable === true);
    assert(d.value === 9);

    console.log("PASS");
} catch (e) {
    console.log(e)
}
