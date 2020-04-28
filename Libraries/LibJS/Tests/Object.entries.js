load("test-common.js");

try {
    assert(Object.entries.length === 1);
    assert(Object.entries(true).length === 0);
    assert(Object.entries(45).length === 0);
    assert(Object.entries(-998).length === 0);
    assert(Object.entries("abcd").length === 4);
    assert(Object.entries([1, 2, 3]).length === 3);
    assert(Object.entries({ a: 1, b: 2, c: 3 }).length === 3);
    
    assertThrowsError(() => {
        Object.entries(null);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });

    assertThrowsError(() => {
        Object.entries(undefined);
    }, {
        error: TypeError,
        message: "ToObject on null or undefined.",
    });
    
    let entries = Object.entries({ foo: 1, bar: 2, baz: 3 });
    assert(
        entries.length === 3 && entries[0].length === 2 && 
        entries[1].length === 2 && entries[2].length === 2 &&
        entries[0][0] === "foo" && entries[0][1] === 1 &&
        entries[1][0] === "bar" && entries[1][1] === 2 &&
        entries[2][0] === "baz" && entries[2][1] === 3
    );

    entries = Object.entries(["a", "b", "c"]);
    assert(
        entries.length === 3 && entries[0].length === 2 && 
        entries[1].length === 2 && entries[2].length === 2 &&
        entries[0][0] === "0" && entries[0][1] === "a" &&
        entries[1][0] === "1" && entries[1][1] === "b" &&
        entries[2][0] === "2" && entries[2][1] === "c"
    );

    let obj = { foo: 1 };
    Object.defineProperty(obj, "getFoo", {
        value: function() { return this.foo; },
    });
    let entries = Object.entries(obj);
    assert(entries.length === 1 && entries[0][0] === "foo" && entries[0][1] === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}