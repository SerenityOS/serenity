load("test-common.js");

try {
    assert((new Proxy({}, { get: undefined })).foo === undefined);
    assert((new Proxy({}, { get: null })).foo === undefined);
    assert((new Proxy({}, {})).foo === undefined);

    let o = {};
    let p = new Proxy(o, {
        get(target, property, receiver) {
            assert(target === o);
            assert(property === "foo");
            assert(receiver === p);
        },
    });

    p.foo;

    o = { foo: 1 };
    p = new Proxy(o, {
        get(target, property, receiver) {
            if (property === "bar") {
                return 2;
            } else if (property === "baz") {
                return receiver.qux;
            } else if (property === "qux") {
                return 3;
            }
            return target[property];
        }
    });

    assert(p.foo === 1);
    assert(p.bar === 2);
    assert(p.baz === 3);
    assert(p.qux === 3);
    assert(p.test === undefined);

    // Invariants

    o = {};
    Object.defineProperty(o, "foo", { value: 5, configurable: false, writable: true });
    Object.defineProperty(o, "bar", { value: 10, configurable: false, writable: false });

    p = new Proxy(o, {
        get() {
            return 8;
        },
    });

    assert(p.foo === 8);

    assertThrowsError(() => {
        p.bar;
    }, {
        error: TypeError,
        message: "Proxy handler's get trap violates invariant: the returned value must match the value on the target if the property exists on the target as a non-writable, non-configurable own data property",
    });

    Object.defineProperty(o, "baz", { configurable: false, set(_) {} });
    assertThrowsError(() => {
        p.baz;
    }, {
        error: TypeError,
        message: "Proxy handler's get trap violates invariant: the returned value must be undefined if the property exists on the target as a non-configurable accessor property with an undefined get attribute",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
