load("test-common.js");

try {
    var foo = true;
    assert(foo.valueOf() === true);
    assert(true.valueOf() === true);

    assert(Boolean.prototype.valueOf.call(true) === true);
    assert(Boolean.prototype.valueOf.call(false) === false);

    try {
        Boolean.prototype.valueOf.call("foo");
        assertNotReached();
    } catch (e) {
        assert(e instanceof Error);
        assert(e.name === "TypeError");
        assert(e.message === "Not a Boolean");
    }

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
