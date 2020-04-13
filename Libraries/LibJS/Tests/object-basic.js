load("test-common.js");

try {
    var o = { 1: 23, foo: "bar", "hello": "friends" };
    assert(o[1] === 23);
    assert(o["1"] === 23);
    assert(o.foo === "bar");
    assert(o["foo"] === "bar");
    assert(o.hello === "friends");
    assert(o["hello"] === "friends");
    o.baz = "test";
    assert(o.baz === "test");
    assert(o["baz"] === "test");
    o[10] = "123";
    assert(o[10] === "123");
    assert(o["10"] === "123");
    o[-1] = "hello friends";
    assert(o[-1] === "hello friends");
    assert(o["-1"] === "hello friends");

    var math = { 3.14: "pi" };
    assert(math["3.14"] === "pi");
    // Note : this test doesn't pass yet due to floating-point literals being coerced to i32 on access
    // assert(math[3.14] === "pi");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
