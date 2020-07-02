load("test-common.js");

try {
    assert(Reflect.has.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.has(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.has() must be an object"
        });
    });

    assert(Reflect.has({}) === false);
    assert(Reflect.has({ undefined }) === true);
    assert(Reflect.has({ 0: "1" }, "0") === true);
    assert(Reflect.has({ foo: "bar" }, "foo") === true);
    assert(Reflect.has({ bar: "baz" }, "foo") === false);
    assert(Reflect.has({}, "toString") === true);

    assert(Reflect.has([]) === false);
    assert(Reflect.has([], 0) === false);
    assert(Reflect.has([1, 2, 3], "0") === true);
    assert(Reflect.has([1, 2, 3], 0) === true);
    assert(Reflect.has([1, 2, 3], 1) === true);
    assert(Reflect.has([1, 2, 3], 2) === true);
    assert(Reflect.has([1, 2, 3], 3) === false);
    assert(Reflect.has([], "pop") === true);

    assert(Reflect.has(new String()) === false);
    assert(Reflect.has(new String("foo"), "0") === true);
    assert(Reflect.has(new String("foo"), 0) === true);
    assert(Reflect.has(new String("foo"), 1) === true);
    assert(Reflect.has(new String("foo"), 2) === true);
    assert(Reflect.has(new String("foo"), 3) === false);
    assert(Reflect.has(new String("foo"), "charAt") === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
