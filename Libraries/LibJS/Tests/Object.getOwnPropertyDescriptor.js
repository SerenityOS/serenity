load("test-common.js");

try {
    let o = {
        foo: "bar",
        get x() { },
        set ["hi" + 1](_) { },
    };

    Object.defineProperty(o, "baz", {
        enumerable: false,
        writable: true,
        value: 10,
    });

    let d = Object.getOwnPropertyDescriptor(o, "foo");
    assert(d.enumerable === true);
    assert(d.configurable === true);
    assert(d.writable === true);
    assert(d.value === "bar");
    assert(d.get === undefined);
    assert(d.set === undefined);

    let d = Object.getOwnPropertyDescriptor(o, "x");
    assert(d.enumerable === true);
    assert(d.configurable === true);
    assert(d.writable === undefined);
    assert(d.value === undefined);
    assert(typeof d.get === "function");
    assert(d.set === undefined);

    let d = Object.getOwnPropertyDescriptor(o, "hi1");
    assert(d.enumerable === true);
    assert(d.configurable === true);
    assert(d.writable === undefined);
    assert(d.value === undefined);
    assert(d.get === undefined);
    assert(typeof d.set === "function");

    let d = Object.getOwnPropertyDescriptor(o, "baz");
    assert(d.enumerable === false);
    assert(d.configurable === false);
    assert(d.writable === true);
    assert(d.value === 10);
    assert(d.get === undefined);
    assert(d.set === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
