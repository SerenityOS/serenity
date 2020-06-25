load("test-common.js");

try {
    assert(Reflect.get.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.get(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.get() must be an object"
        });
    });

    assert(Reflect.get({}) === undefined);
    assert(Reflect.get({ undefined: 1 }) === 1);
    assert(Reflect.get({ foo: 1 }) === undefined);
    assert(Reflect.get({ foo: 1 }, "foo") === 1);

    assert(Reflect.get([]) === undefined);
    assert(Reflect.get([1, 2, 3]) === undefined);
    assert(Reflect.get([1, 2, 3], "0") === 1);
    assert(Reflect.get([1, 2, 3], 0) === 1);
    assert(Reflect.get([1, 2, 3], 1) === 2);
    assert(Reflect.get([1, 2, 3], 2) === 3);
    assert(Reflect.get([1, 2, 3], 4) === undefined);

    assert(Reflect.get(new String()) === undefined);
    assert(Reflect.get(new String(), 0) === undefined);
    assert(Reflect.get(new String("foo"), "0") === "f");
    assert(Reflect.get(new String("foo"), 0) === "f");
    assert(Reflect.get(new String("foo"), 1) === "o");
    assert(Reflect.get(new String("foo"), 2) === "o");
    assert(Reflect.get(new String("foo"), 3) === undefined);

    const foo = {
        get prop() {
            this.getPropCalled = true;
        }
    };
    const bar = {};
    Object.setPrototypeOf(bar, foo);

    assert(foo.getPropCalled === undefined);
    assert(bar.getPropCalled === undefined);
    Reflect.get(bar, "prop");
    assert(foo.getPropCalled === undefined);
    assert(bar.getPropCalled === true);
    Reflect.get(bar, "prop", foo);
    assert(foo.getPropCalled === true);
    assert(bar.getPropCalled === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
