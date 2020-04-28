load("test-common.js");

try {
    assert(Object.values.length === 1);
    assert(Object.values(true).length === 0);
    assert(Object.values(45).length === 0);
    assert(Object.values(-998).length === 0);
    assert(Object.values("abcd").length === 4);
    assert(Object.values([1, 2, 3]).length === 3);
    assert(Object.values({ a: 1, b: 2, c: 3 }).length === 3);
    
    assertThrowsError(() => {
        Object.values(null);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });

    assertThrowsError(() => {
        Object.values(undefined);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });
    
    let values = Object.values({ foo: 1, bar: 2, baz: 3 });
    assert(values[0] === 1 && values[1] === 2 && values[2] === 3);

    values = Object.values(["a", "b", "c"]);
    assert(values[0] === "a" && values[1] === "b" && values[2] === "c");

    let obj = { foo: 1 };
    Object.defineProperty(obj, 'getFoo', {
        value: function() { return this.foo; },
    });
    let values = Object.values(obj);
    assert(values.length === 1 && values[0] === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}