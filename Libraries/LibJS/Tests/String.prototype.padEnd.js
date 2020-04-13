load("test-common.js");

try {
    assert(String.prototype.padEnd.length === 1);

    var s = "foo";
    assert(s.padEnd(-1) === "foo");
    assert(s.padEnd(0) === "foo");
    assert(s.padEnd(3) === "foo");
    assert(s.padEnd(5) === "foo  ");
    assert(s.padEnd(10) === "foo       ");
    assert(s.padEnd("5") === "foo  ");
    assert(s.padEnd([[["5"]]]) === "foo  ");
    assert(s.padEnd(2, "+") === "foo");
    assert(s.padEnd(5, "+") === "foo++");
    assert(s.padEnd(5, 1) === "foo11");
    assert(s.padEnd(10, null) === "foonullnul");
    assert(s.padEnd(10, "bar") === "foobarbarb");
    assert(s.padEnd(10, "123456789") === "foo1234567");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
