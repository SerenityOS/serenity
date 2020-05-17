load("test-common.js");

try {
    const message = "oops, Value::to_number() failed";
    const o = { toString() { throw new Error(message); } };

    assertThrowsError(() => {
        +o;
    }, {
        error: Error,
        message
    });

    assertThrowsError(() => {
        o - 1;
    }, {
        error: Error,
        message
    });

    assertThrowsError(() => {
        "foo".charAt(o);
    }, {
        error: Error,
        message
    });

    assertThrowsError(() => {
        "bar".repeat(o);
    }, {
        error: Error,
        message
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
