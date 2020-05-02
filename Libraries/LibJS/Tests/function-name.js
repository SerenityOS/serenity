load("test-common.js");

try {
    var f = function () { }
    assert(f.name === "");
    assert((f.name = "f") === "f");
    assert(f.name === "");

    function foo() { }
    assert(foo.name === "foo");
    assert((foo.name = "bar") === "bar");
    assert(foo.name === "foo");

    assert(console.log.name === "log");
    assert((console.log.name = "warn") === "warn");
    assert(console.log.name === "log");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
