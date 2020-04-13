try {
    var foo = true;
    assert(foo.toString() === "true");
    assert(true.toString() === "true");

    assert(Boolean.prototype.toString.call(true) === "true");
    assert(Boolean.prototype.toString.call(false) === "false");

    try {
        Boolean.prototype.toString.call("foo");
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
