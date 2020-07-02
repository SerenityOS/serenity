load("test-common.js");

try {
    assert("foo" in new Proxy({}, { has: null }) === false);
    assert("foo" in new Proxy({}, { has: undefined}) === false);
    assert("foo" in new Proxy({}, {}) === false);

    let o = {};
    let p = new Proxy(o, {
        has(target, prop) {
            assert(target === o);
            assert(prop === "foo");
            return true;
        }
    });

    "foo" in p;

    p = new Proxy(o, {
        has(target, prop) {
            if (target.checkedFoo)
                return true;
            if (prop === "foo")
                target.checkedFoo = true;
            return false;
        }
    });

    assert("foo" in p === false);
    assert("foo" in p === true);

    // Invariants

    o = {};
    Object.defineProperty(o, "foo", { configurable: false });
    Object.defineProperty(o, "bar", { value: 10, configurable: true });
    p = new Proxy(o, {
        has() {
            return false;
        }
    });

    assertThrowsError(() => {
        "foo" in p;
    }, {
        error: TypeError,
        message: "Proxy handler's has trap violates invariant: a property cannot be reported as non-existent if it exists on the target as a non-configurable property",
    });

    Object.preventExtensions(o);

    assertThrowsError(() => {
        "bar" in p;
    }, {
        error: TypeError,
        message: "Proxy handler's has trap violates invariant: a property cannot be reported as non-existent if it exists on the target and the target is non-extensible",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
