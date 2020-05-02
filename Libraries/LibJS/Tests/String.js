load("test-common.js");

try {
    assert(String.length === 1);
    assert(String.name === "String");
    assert(String.prototype.length === 0);

    assert(typeof String() === "string");
    assert(typeof new String() === "object");

    assert(String() === "");
    assert(new String().valueOf() === "");
    assert(String("foo") === "foo");
    assert(new String("foo").valueOf() === "foo");
    assert(String(123) === "123");
    assert(new String(123).valueOf() === "123");
    assert(String(123) === "123");
    assert(new String(123).valueOf() === "123");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
