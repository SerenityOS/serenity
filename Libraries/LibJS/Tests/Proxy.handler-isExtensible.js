load("test-common.js");

try {
    assert(Object.isExtensible(new Proxy({}, { isExtensible: null })) === true);
    assert(Object.isExtensible(new Proxy({}, { isExtensible: undefined })) === true);
    assert(Object.isExtensible(new Proxy({}, {})) === true);

    let o = {};
    let p = new Proxy(o, {
        isExtensible(target) {
            assert(target === o);
            return true;
        }
    });

    Object.isExtensible(p);

    // Invariants

    o = {};
    p = new Proxy(o, {
        isExtensible(proxyTarget) {
            assert(proxyTarget === o);
            return true;
        },
    });

    assert(Object.isExtensible(p) === true);
    Object.preventExtensions(o);

    assertThrowsError(() => {
        Object.isExtensible(p);
    }, {
        error: TypeError,
        message: "Proxy handler's isExtensible trap violates invariant: return value must match the target's extensibility",
    });

    p = new Proxy(o, {
        isExtensible(proxyTarget) {
            assert(proxyTarget === o);
            return false;
        },
    });
    assert(Object.isExtensible(p) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
