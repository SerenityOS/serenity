load("test-common.js");

try {
    assert(Array.prototype.toLocaleString.length === 0);

    assert([].toLocaleString() === "");
    assert(["foo"].toLocaleString() === "foo");
    assert(["foo", "bar"].toLocaleString() === "foo,bar");
    assert(["foo", undefined, "bar", null, "baz"].toLocaleString() === "foo,,bar,,baz");

    var toStringCalled = 0;
    var o = {
        toString: () => {
            toStringCalled++;
            return "o";
        }
    };
    assert([o, undefined, o, null, o].toLocaleString() === "o,,o,,o");
    assert(toStringCalled === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
