load("test-common.js")

try {
    const s1 = Symbol("foo");
    const s2 = Symbol("foo");

    assert(s1 !== s2);
    assert(s1.description === "foo");
    assert(s2.description === "foo");

    s1.description = "bar";
    assert(s1.description === "foo");

    assert(typeof s1 === "symbol");

    assertThrowsError(() => {
        Symbol(Symbol('foo'));
    }, {
        error: TypeError,
        message: "Can't convert symbol to string"
    })
    
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
