load("test-common.js");

try {
    let p = new Proxy({}, { preventExtensions: null });
    assert(Object.preventExtensions(p) === p);
    p = new Proxy({}, { preventExtensions: undefined });
    assert(Object.preventExtensions(p) === p);
    p = new Proxy({}, {});
    assert(Object.preventExtensions(p) == p);

    let o = {};
    p = new Proxy(o, {
        preventExtensions(target) {
            assert(target === o);
            return true;
        }
    });

    Object.preventExtensions(o);
    Object.preventExtensions(p);

    // Invariants

    p = new Proxy({}, {
        preventExtensions() {
            return false;
        },
    });
    assertThrowsError(() => {
        Object.preventExtensions(p);
    }, {
        error: TypeError,
        message: "Object's [[PreventExtensions]] method returned false",
    });

    o = {};
    p = new Proxy(o, {
        preventExtensions() {
            return true;
        },
    });
    assertThrowsError(() => {
        Object.preventExtensions(p);
    }, {
        error: TypeError,
        message: "Proxy handler's preventExtensions trap violates invariant: cannot return true if the target object is extensible"
    });

    Object.preventExtensions(o);
    assert(Object.preventExtensions(p) === p);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
