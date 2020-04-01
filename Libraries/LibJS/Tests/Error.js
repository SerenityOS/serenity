function assert(x) { if (!x) throw 1; }

try {
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
