load("test-common.js");

try {
    assert((new Proxy({}, { set: undefined }).foo = 1) === 1);
    assert((new Proxy({}, { set: null }).foo = 1) === 1);
    assert((new Proxy({}, {}).foo = 1) === 1);

    let o = {};
    let p = new Proxy(o, {
        set(target, prop, value, receiver) {
            assert(target === o);
            assert(prop === "foo");
            assert(value === 10);
            assert(receiver === p);
            return true;
        },
    });

    p.foo = 10;

    p = new Proxy(o, {
        set(target, prop, value, receiver) {
            if (target[prop] === value) {
                target[prop] *= 2;
            } else {
                target[prop] = value;
            }
        },
    });

    p.foo = 10;
    assert(p.foo === 10);
    p.foo = 10;
    assert(p.foo === 20);
    p.foo = 10;
    assert(p.foo === 10);

    // Invariants

    o = {};
    Object.defineProperty(o, "foo", { value: 10 });
    p = new Proxy(o, {
        set() {
            return true;
        },
    });

    assertThrowsError(() => {
        p.foo = 12;
    }, {
        error: TypeError,
        message: "Proxy handler's set trap violates invariant: cannot return true for a property on the target which is a non-configurable, non-writable own data property",
    });

    Object.defineProperty(o, "bar", { get() {} });
    assertThrowsError(() => {
        p.bar = 12;
    }, {
        error: TypeError,
        message: "Proxy handler's set trap violates invariant: cannot return true for a property on the target which is a non-configurable own accessor property with an undefined set attribute",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
