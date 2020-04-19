load("test-common.js");

try {
    var foo = true;
    assert(foo.toString() === "true");
    assert(true.toString() === "true");

    assert(Boolean.prototype.toString.call(true) === "true");
    assert(Boolean.prototype.toString.call(false) === "false");

    assertThrowsError(() => {
        Boolean.prototype.toString.call("foo");
    }, {
        error: TypeError,
        message: "Not a Boolean"
    });

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
