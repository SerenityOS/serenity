load("test-common.js");

try {
    assert(delete (new Proxy({}, { deleteProperty: undefined })).foo === true);
    assert(delete (new Proxy({}, { deleteProperty: null })).foo === true);
    assert(delete (new Proxy({}, {})).foo === true);

    let o = {};
    let p = new Proxy(o, {
        deleteProperty(target, property) {
            assert(target === o);
            assert(property === "foo");
            return true;
        }
    });

    delete p.foo;

    o = { foo: 1, bar: 2 };
    p = new Proxy(o, {
        deleteProperty(target, property) {
            if (property === "foo") {
                delete target[property];
                return true;
            }
            return false;
        }
    });

    assert(delete p.foo === true);
    assert(delete p.bar === false);

    assert(o.foo === undefined);
    assert(o.bar === 2);

    // Invariants

    o = {};
    Object.defineProperty(o, "foo", { configurable: false });
    p = new Proxy(o, {
        deleteProperty() {
            return true;
        },
    });

    assertThrowsError(() => {
        delete p.foo;
    }, {
        error: TypeError,
        message: "Proxy handler's deleteProperty trap violates invariant: cannot report a non-configurable own property of the target as deleted",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
