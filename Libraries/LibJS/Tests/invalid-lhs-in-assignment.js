load("test-common.js");

try {
    assertThrowsError(() => {
        512 = 256;
    }, {
        error: ReferenceError,
        message: "Invalid left-hand side in assignment"
    });

    assertThrowsError(() => {
        512 = 256;
    }, {
        error: ReferenceError,
        message: "Invalid left-hand side in assignment"
    });

    assertThrowsError(() => {
        "hello world" = "another thing?";
    }, {
        error: ReferenceError,
        message: "Invalid left-hand side in assignment"
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
