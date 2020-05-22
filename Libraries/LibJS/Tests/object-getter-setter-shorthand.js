load("test-common.js")

try {
    let o = {
        get() { return 5; },
        set() { return 10; },
    };
    assert(o.get() === 5);
    assert(o.set() === 10);

    o = {
        get x() { return 5; },
        set x(_) { },
    };
    assert(o.x === 5);
    o.x = 10;
    assert(o.x === 5);

    o = {
        get x() {
            return this._x + 1;
        },
        set x(value) {
            this._x = value + 1;
        },
    };

    assert(isNaN(o.x));
    o.x = 10;
    assert(o.x === 12);
    o.x = 20;
    assert(o.x === 22);

    o = {
        get x() { return 5; },
        get x() { return 10; },
    };

    assert(o.x === 10);

    o = {
        set x(value) { return 10; },
    };

    assert((o.x = 20) === 20);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
