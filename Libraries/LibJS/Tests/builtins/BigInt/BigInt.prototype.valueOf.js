load("test-common.js");

try {
    assert(BigInt.prototype.valueOf.length === 0);

    assertThrowsError(() => {
        BigInt.prototype.valueOf.call("foo");
    }, {
        error: TypeError,
        message: "Not a BigInt object"
    });

    assert(typeof BigInt(123).valueOf() === "bigint");
    // FIXME: Uncomment once we support Object() with argument
    // assert(typeof Object(123n).valueOf() === "bigint");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
