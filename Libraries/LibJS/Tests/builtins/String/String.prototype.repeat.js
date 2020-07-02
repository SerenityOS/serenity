load("test-common.js");

try {
    assert(String.prototype.repeat.length === 1);

    try {
        "foo".repeat(-1);
        assertNotReached();
    } catch (e) {
        assert(e.name === "RangeError");
        assert(e.message === "repeat count must be a positive number");
    }

    try {
        "foo".repeat(Infinity);
        assertNotReached();
    } catch (e) {
        assert(e.name === "RangeError");
        assert(e.message === "repeat count must be a finite number");
    }

    assert("foo".repeat(0) === "");
    assert("foo".repeat(1) === "foo");
    assert("foo".repeat(2) === "foofoo");
    assert("foo".repeat(3) === "foofoofoo");
    assert("foo".repeat(3.1) === "foofoofoo");
    assert("foo".repeat(3.5) === "foofoofoo");
    assert("foo".repeat(3.9) === "foofoofoo");
    assert("foo".repeat(null) === "");
    assert("foo".repeat(undefined) === "");
    assert("foo".repeat([]) === "");
    assert("foo".repeat("") === "");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
