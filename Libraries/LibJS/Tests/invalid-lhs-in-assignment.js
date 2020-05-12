load("test-common.js");

try {
    function foo() { }

    assertThrowsError(() => {
        foo() = "foo";
    }, {
        error: ReferenceError,
        message: "Invalid left-hand side in assignment"
    });

    assertThrowsError(() => {
        (function () { })() = "foo";
    }, {
        error: ReferenceError,
        message: "Invalid left-hand side in assignment"
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
