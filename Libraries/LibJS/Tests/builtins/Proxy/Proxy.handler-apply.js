load("test-common.js");

try {
    let p = new Proxy(() => 5, { apply: null });
    assert(p() === 5);
    let p = new Proxy(() => 5, { apply: undefined });
    assert(p() === 5);
    let p = new Proxy(() => 5, {});
    assert(p() === 5);

    const f = (a, b) => a + b;
    const handler = {
        apply(target, this_, arguments) {
            assert(target === f);
            assert(this_ === handler);
            if (arguments[2])
                return arguments[0] * arguments[1];
            return f(...arguments);
        },
    };
    p = new Proxy(f, handler);

    assert(p(2, 4) === 6);
    assert(p(2, 4, true) === 8);

    // Invariants
    [{}, [], new Proxy({}, {})].forEach(item => {
        assertThrowsError(() => {
            new Proxy(item, {})();
        }, {
            error: TypeError,
            message: "[object ProxyObject] is not a function",
        });
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
