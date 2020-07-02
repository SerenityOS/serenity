load("test-common.js");

try {
    const child = {};
    const childProto = { foo: "bar" };

    Object.setPrototypeOf(child, childProto);
    assert(child.foo === "bar");

    Object.getPrototypeOf(new Proxy(child, { getPrototypeOf: null }));
    Object.getPrototypeOf(new Proxy(child, { getPrototypeOf: undefined }));

    let o = {};
    let p = new Proxy(o, {
        getPrototypeOf(target) {
            assert(target === o);
            return null;
        }
    });

    Object.getPrototypeOf(p);

    p = new Proxy(o, {
        getPrototypeOf(target) {
            if (target.foo)
                return { bar: 1 };
            return { bar: 2 };
        },
    });

    assert(Object.getPrototypeOf(p).bar === 2);
    o.foo = 20
    assert(Object.getPrototypeOf(p).bar === 1);

    // Invariants

    assertThrowsError(() => {
        Object.getPrototypeOf(new Proxy(child, { getPrototypeOf: 1 }));
    }, {
        error: TypeError,
        message: "Proxy handler's getPrototypeOf trap wasn't undefined, null, or callable",
    });

    assertThrowsError(() => {
        Object.getPrototypeOf(new Proxy(child, { getPrototypeOf() { return 1; } }));
    }, {
        error: TypeError,
        message: "Proxy handler's getPrototypeOf trap violates invariant: must return an object or null",
    });

    p = new Proxy(child, {
        getPrototypeOf(target) {
            assert(target === child);
            return { baz: "qux" };
        },
    });

    assert(Object.getPrototypeOf(p).baz === "qux");

    Object.preventExtensions(child);
    p = new Proxy(child, {
        getPrototypeOf(target) {
            assert(target === child);
            return childProto;
        }
    });

    assert(Object.getPrototypeOf(p).foo === "bar");

    p = new Proxy(child, {
        getPrototypeOf(target) {
            assert(target === child);
            return { baz: "qux" };
        }
    });

    assertThrowsError(() => {
        Object.getPrototypeOf(p);
    }, {
        error: TypeError,
        message: "Proxy handler's getPrototypeOf trap violates invariant: cannot return a different prototype object for a non-extensible target"
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
