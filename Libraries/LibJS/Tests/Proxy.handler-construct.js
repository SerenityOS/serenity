load("test-common.js");

try {
    let p = new Proxy(function() { this.x = 5; }, { construct: null });
    assert((new p).x === 5);
    let p = new Proxy(function() { this.x = 5; }, { construct: undefined });
    assert((new p).x === 5);
    let p = new Proxy(function() { this.x = 5; }, {});
    assert((new p).x === 5);

    function f(value) {
        this.x = value;
    }

    let p;
    const handler = {
        construct(target, arguments, newTarget) {
            assert(target === f);
            assert(newTarget === p);
            if (arguments[1])
                return Reflect.construct(target, [arguments[0] * 2], newTarget);
            return Reflect.construct(target, arguments, newTarget);
        },
    };
    p = new Proxy(f, handler);

    assert(new p(15).x === 15);
    assert(new p(15, true).x === 30);

    // Invariants
    [{}, [], new Proxy({}, {})].forEach(item => {
        assertThrowsError(() => {
            new (new Proxy(item, {}));
        }, {
            error: TypeError,
            message: "[object ProxyObject] is not a constructor",
        });
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
