load("test-common.js");

try {
    assert(Error.length === 1);
    assert(Error.name === "Error");
    assert(Error.prototype.length === undefined);

    var e;

    e = Error();
    assert(e.name === "Error");
    assert(e.message === "");

    e = Error(undefined);
    assert(e.name === "Error");
    assert(e.message === "");

    e = Error("test");
    assert(e.name === "Error");
    assert(e.message === "test");

    e = Error(42);
    assert(e.name === "Error");
    assert(e.message === "42");

    e = Error(null);
    assert(e.name === "Error");
    assert(e.message === "null");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
