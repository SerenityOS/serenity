load("test-common.js");

try {
    let p = new Proxy({}, { defineProperty: null });
    assert(Object.defineProperty(p, "foo", {}) === p);
    p = new Proxy({}, { defineProperty: undefined });
    assert(Object.defineProperty(p, "foo", {}) === p);
    p = new Proxy({}, {});
    assert(Object.defineProperty(p, "foo", {}) == p);

    let o = {};
    p = new Proxy(o, {
        defineProperty(target, name, descriptor) {
            assert(target === o);
            assert(name === "foo");
            assert(descriptor.configurable === true);
            assert(descriptor.enumerable === undefined);
            assert(descriptor.writable === true);
            assert(descriptor.value === 10);
            assert(descriptor.get === undefined);
            assert(descriptor.set === undefined);
            return true;
        },
    });

    Object.defineProperty(p, "foo", { configurable: true, writable: true, value: 10 });

    p = new Proxy(o, {
        defineProperty(target, name, descriptor) {
            if (target[name] === undefined)
                Object.defineProperty(target, name, descriptor);
            return true;
        },
    });

    Object.defineProperty(p, "foo", { value: 10, enumerable: true, configurable: false, writable: true });
    let d = Object.getOwnPropertyDescriptor(p, "foo");
    assert(d.enumerable === true);
    assert(d.configurable === false);
    assert(d.writable === true);
    assert(d.value === 10);
    assert(d.get === undefined);
    assert(d.set === undefined);

    Object.defineProperty(p, "foo", { value: 20, enumerable: true, configurable: false, writable: true });
    d = Object.getOwnPropertyDescriptor(p, "foo");
    assert(d.enumerable === true);
    assert(d.configurable === false);
    assert(d.writable === true);
    assert(d.value === 10);
    assert(d.get === undefined);
    assert(d.set === undefined);


    // Invariants

    p = new Proxy({}, {
        defineProperty() { return false; }
    });

    assertThrowsError(() => {
        Object.defineProperty(p, "foo", {});
    }, {
        error: TypeError,
        message: "Proxy handler's defineProperty method returned false",
    });

    o = {};
    Object.preventExtensions(o);
    p = new Proxy(o, {
        defineProperty() {
            return true;
        }
    });
    assertThrowsError(() => {
        Object.defineProperty(p, "foo", {});
    }, {
        error: TypeError,
        message: "Proxy handler's defineProperty trap violates invariant: a property cannot be reported as being defined if the property does not exist on the target and the target is non-extensible",
    });

    o = {};
    Object.defineProperty(o, "foo", { value: 10, configurable: true });
    p = new Proxy(o, {
        defineProperty() {
            return true;
        },
    });

    assertThrowsError(() => {
        Object.defineProperty(p, "bar", { value: 6, configurable: false });
    }, {
        error: TypeError,
        message: "Proxy handler's defineProperty trap violates invariant: a property cannot be defined as non-configurable if it does not already exist on the target object",
    });

    assertThrowsError(() => {
        Object.defineProperty(p, "foo", { value: 6, configurable: false });
    }, {
        error: TypeError,
        message: "Proxy handler's defineProperty trap violates invariant: a property cannot be defined as non-configurable if it already exists on the target object as a configurable property",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
