load("test-common.js");

try {
    const child = {};
    const childProto = { foo: "bar" };

    Object.setPrototypeOf(child, childProto);
    assert(child.foo === "bar");

    Object.setPrototypeOf(new Proxy(child, { setPrototypeOf: null }), childProto);
    Object.setPrototypeOf(new Proxy(child, { setPrototypeOf: undefined }), childProto);

    let o = {};
    let theNewProto = { foo: "bar" };
    let p = new Proxy(o, {
        setPrototypeOf(target, newProto) {
            assert(target === o);
            assert(newProto === theNewProto);
            return true;
        }
    });

    Object.setPrototypeOf(p, theNewProto);

    p = new Proxy(o, {
        setPrototypeOf(target, newProto) {
            if (target.shouldSet)
                Object.setPrototypeOf(target, newProto);
            return true;
        },
    });

    Object.setPrototypeOf(p, { foo: 1 });
    assert(Object.getPrototypeOf(p).foo === undefined);
    p.shouldSet = true;
    assert(o.shouldSet === true);
    Object.setPrototypeOf(p, { foo: 1 });
    assert(Object.getPrototypeOf(p).foo === 1);

    // Invariants

    assertThrowsError(() => {
        Object.setPrototypeOf(new Proxy(child, { setPrototypeOf: 1 }), {});
    }, {
        error: TypeError,
        message: "Proxy handler's setPrototypeOf trap wasn't undefined, null, or callable",
    });

    p = new Proxy(child, {
        setPrototypeOf(target, newProto) {
            assert(target === child);
            return false;
        },
    });

    assertThrowsError(() => {
        Object.setPrototypeOf(p, {});
    }, {
        error: TypeError,
        message: "Object's setPrototypeOf method returned false"
    });
    assert(Object.getPrototypeOf(p) === childProto);

    p = new Proxy(child, {
        setPrototypeOf(target, newProto) {
            assert(target === child);
            return true;
        },
    });

    assert(Object.setPrototypeOf(p, {}) === p);
    assert(Object.getPrototypeOf(p) === childProto);

    Object.preventExtensions(child);
    p = new Proxy(child, {
        setPrototypeOf(target, newProto) {
            assert(target === child);
            return true;
        },
    });

    assert(Object.setPrototypeOf(p, childProto) === p);
    assert(Object.getPrototypeOf(p) === childProto);

    assertThrowsError(() => {
        Object.setPrototypeOf(p, {});
    }, {
        error: TypeError,
        message: "Proxy handler's setPrototypeOf trap violates invariant: the argument must match the prototype of the target if the target is non-extensible",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
