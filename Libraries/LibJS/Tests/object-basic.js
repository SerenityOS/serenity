load("test-common.js");

try {
    var foo = "bar";
    var computed = "computed"
    var o = {
        1: 23,
        foo,
        bar: "baz",
        "hello": "friends",
        [1 + 2]: 42,
        ["I am a " + computed + " key"]: foo,
        duplicate: "hello",
        duplicate: "world"
    };
    assert(o[1] === 23);
    assert(o["1"] === 23);
    assert(o.foo === "bar");
    assert(o["foo"] === "bar");
    assert(o.hello === "friends");
    assert(o["hello"] === "friends");
    assert(o[3] === 42);
    assert(o["I am a computed key"] === "bar");
    assert(o.duplicate === "world");
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

    // This is also allowed! Watch out for syntax errors.
    var o2 = { return: 1, yield: 1, for: 1, catch: 1, break: 1 };
    assert(o2.return === 1);
    assert(o2.yield === 1);
    assert(o2.for === 1);
    assert(o2.catch === 1);
    assert(o2.break === 1);

    var a;
    var append = x => { a.push(x); };

    a = [];
    var o3 = {[append(1)]: 1, [append(2)]: 2, [append(3)]: 3}
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(o3.undefined === 3);

    a = [];
    var o4 = {"test": append(1), "test": append(2), "test": append(3)}
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(o4.test === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
