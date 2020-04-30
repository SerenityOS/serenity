load("test-common.js")

try {
    const s1 = Symbol("foo");
    const s2 = Symbol.for("bar");

    assert(s1.toString() === "Symbol(foo)");
    assert(s2.toString() === "Symbol(bar)");
    
    assertThrowsError(() => {
        s1 + "";
    }, {
        error: TypeError,
        message: "Can't convert symbol to string",
    });
    
    // FIXME: Uncomment when this doesn't assert
    // assertThrowsError(() => {
    //     s1 + 1;
    // }, {
    //     error: TypeError,
    //     message: "Can't convert symbol to number",
    // });
    
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
} 
