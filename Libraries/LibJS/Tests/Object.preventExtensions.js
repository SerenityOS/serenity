load("test-common.js");

try {
    assert(Object.preventExtensions() === undefined);
    assert(Object.preventExtensions(undefined) === undefined);
    assert(Object.preventExtensions(null) === null);
    assert(Object.preventExtensions(true) === true);
    assert(Object.preventExtensions(6) === 6);
    assert(Object.preventExtensions("test") === "test");

    let s = Symbol();
    assert(Object.preventExtensions(s) === s);

    let o = { foo: "foo" };
    assert(o.foo === "foo");
    o.bar = "bar";
    assert(o.bar === "bar");

    assert(Object.preventExtensions(o) === o);
    assert(o.foo === "foo");
    assert(o.bar === "bar");

    o.baz = "baz";
    assert(o.baz === undefined);

    assertThrowsError(() => {
        Object.defineProperty(o, "baz", { value: "baz" });
    }, {
        error: TypeError,
        message: "Cannot define property baz on non-extensible object",
    });

    assert(o.baz === undefined);

    assertThrowsError(() => {
        "use strict";
        o.baz = "baz";
    }, {
        error: TypeError,
        message: "Cannot define property baz on non-extensible object",
    });

    assertThrowsError(() => {
        "use strict";
        Object.defineProperty(o, "baz", { value: "baz" });
    }, {
        error: TypeError,
        message: "Cannot define property baz on non-extensible object",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
