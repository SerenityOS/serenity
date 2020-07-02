load("test-common.js");

try {
    assert(Error().toString() === "Error");
    assert(Error(undefined).toString() === "Error");
    assert(Error(null).toString() === "Error: null");
    assert(Error("test").toString() === "Error: test");
    assert(Error(42).toString() === "Error: 42");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
