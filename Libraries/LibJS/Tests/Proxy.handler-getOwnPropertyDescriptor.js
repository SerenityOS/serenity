load("test-common.js");

try {
    assert(Object.getOwnPropertyDescriptor(new Proxy({}, { getOwnPropertyDescriptor: null }), "a") === undefined);
    assert(Object.getOwnPropertyDescriptor(new Proxy({}, { getOwnPropertyDescriptor: undefined }), "a") === undefined);
    assert(Object.getOwnPropertyDescriptor(new Proxy({}, {}), "a") === undefined);

    let o = {};
    let p = new Proxy(o, {
        getOwnPropertyDescriptor(target, property) {
            assert(target === o);
            assert(property === "foo");
        }
    });

    Object.getOwnPropertyDescriptor(p, "foo");

    o = { foo: "bar" };
    Object.defineProperty(o, "baz", { value: "qux", enumerable: false, configurable: true, writable: false });
    p = new Proxy(o, {
        getOwnPropertyDescriptor(target, property) {
            if (property === "baz")
                return Object.getOwnPropertyDescriptor(target, "baz");
             return { value: target[property], enumerable: false, configurable: true, writable: true };
        }
    });

    let d = Object.getOwnPropertyDescriptor(p, "baz");
    assert(d.configurable === true);
    assert(d.enumerable === false);
    assert(d.writable === false);
    assert(d.value === "qux");
    assert(d.get === undefined);
    assert(d.set === undefined);

    d = Object.getOwnPropertyDescriptor(p, "foo");
    assert(d.configurable === true);
    assert(d.enumerable === false);
    assert(d.writable === true);
    assert(d.value === "bar");
    assert(d.get === undefined);
    assert(d.set === undefined);

    // Invariants

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy({}, {
            getOwnPropertyDescriptor: 1
        }));
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap wasn't undefined, null, or callable",
    });

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy({}, {
            getOwnPropertyDescriptor() {
                return 1;
            },
        }));
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: must return an object or undefined",
    });

    o = {};
    Object.defineProperty(o, "foo", { value: 10, configurable: false });
    p = new Proxy(o, {
        getOwnPropertyDescriptor() {
            return undefined;
        },
    });

    assert(Object.getOwnPropertyDescriptor(p, "bar") === undefined);

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(p, "foo");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot return undefined for a property on the target which is a non-configurable property",
    });

    Object.defineProperty(o, "baz", { value: 20, configurable: true, writable: true, enumerable: true });
    Object.preventExtensions(o);

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(p, "baz");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot report a property as being undefined if it exists as an own property of the target and the target is non-extensible",
    });

    o = {};
    Object.defineProperty(o, "v1", { value: 10, configurable: false });
    Object.defineProperty(o, "v2", { value: 10, configurable: false, enumerable: true });
    Object.defineProperty(o, "v3", { configurable: false, get() { return 1; } });
    Object.defineProperty(o, "v4", { value: 10, configurable: false, writable: false, enumerable: true });

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy(o, {
            getOwnPropertyDescriptor() {
                return { configurable: true };
            },
        }), "v1");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target",
    });

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy(o, {
            getOwnPropertyDescriptor() {
                return { enumerable: false };
            },
        }), "v2");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target",
    });

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy(o, {
            getOwnPropertyDescriptor() {
                return { value: 10 };
            },
        }), "v3");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target",
    });

    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy(o, {
            getOwnPropertyDescriptor() {
                return { value: 10, writable: true };
            },
        }), "v4");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target",
    });

    o = {};
    Object.defineProperty(o, "v", { configurable: true });
    assertThrowsError(() => {
        Object.getOwnPropertyDescriptor(new Proxy(o, {
            getOwnPropertyDescriptor() {
                return { configurable: false };
            },
        }), "v");
    }, {
        error: TypeError,
        message: "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot report target's property as non-configurable if the property does not exist, or if it is configurable",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
