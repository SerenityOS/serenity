load("test-common.js");

try {
    assert(Object.keys.length === 1);
    assert(Object.keys(true).length === 0);
    assert(Object.keys(45).length === 0);
    assert(Object.keys(-998).length === 0);
    assert(Object.keys("abcd").length === 4);
    assert(Object.keys([1, 2, 3]).length === 3);
    assert(Object.keys({ a: 1, b: 2, c: 3 }).length === 3);

    assertThrowsError(() => {
        Object.keys(null);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });

    assertThrowsError(() => {
        Object.keys(undefined);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });

    let keys = Object.keys({ foo: 1, bar: 2, baz: 3 });
    assert(keys[0] === "foo" && keys[1] === "bar" && keys[2] === "baz");

    keys = Object.keys(["a", "b", "c"]);
    assert(keys[0] === "0" && keys[1] === "1" && keys[2] === "2");

    let obj = { foo: 1 };
    Object.defineProperty(obj, 'getFoo', {
        value: function() { return this.foo; },
    });
    keys = Object.keys(obj);
    assert(keys.length === 1 && keys[0] === 'foo');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
} 
