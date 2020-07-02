load("test-common.js");

try {
    assert(BigInt.prototype.toString.length === 0);

    assertThrowsError(() => {
        BigInt.prototype.toString.call("foo");
    }, {
        error: TypeError,
        message: "Not a BigInt object"
    });
    
    assert(BigInt(123).toString() === "123");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
