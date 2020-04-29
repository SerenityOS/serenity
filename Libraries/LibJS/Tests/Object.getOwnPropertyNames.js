load("test-common.js");

try {
    let names = Object.getOwnPropertyNames([1, 2, 3]);

    assert(names.length === 4);
    assert(names[0] === "0");
    assert(names[1] === "1");
    assert(names[2] === "2");
    assert(names[3] === "length");

    names = Object.getOwnPropertyNames({ foo: 1, bar: 2, baz: 3 });
    assert(names.length === 3);
    assert(names[0] === "foo");
    assert(names[1] === "bar");
    assert(names[2] === "baz");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
