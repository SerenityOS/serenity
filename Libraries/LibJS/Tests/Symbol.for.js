load("test-common.js")

try {
    const localSym = Symbol("foo");
    const globalSym = Symbol.for("foo");

    assert(localSym !== globalSym);
    assert(localSym !== Symbol("foo"));
    assert(globalSym !== Symbol("foo"));
    assert(globalSym === Symbol.for("foo"));
    assert(localSym.toString() === "Symbol(foo)");
    assert(globalSym.toString() === "Symbol(foo)");

    assert(Symbol.for(1).description === "1");
    assert(Symbol.for(true).description === "true");
    assert(Symbol.for({}).description === "[object Object]");
    assert(Symbol.for().description === "undefined");
    assert(Symbol.for(null).description === "null");

    assertThrowsError(() => {
        Symbol.for(Symbol());
    }, {
        error: TypeError,
        message: "Can't convert symbol to string",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
