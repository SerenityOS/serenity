try {
    var o = { foo: "bar" };
    assert(o.foo === "bar");
    assert(o["foo"] === "bar");
    o.baz = "test";
    assert(o.baz === "test");
    assert(o["baz"] === "test");
    o[10] = "123";
    assert(o[10] === "123");
    assert(o["10"] === "123");
    o[-1] = "hello friends";
    assert(o[-1] === "hello friends");
    assert(o["-1"] === "hello friends");
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}