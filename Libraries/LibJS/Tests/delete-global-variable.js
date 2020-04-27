load("test-common.js");

try {
    a = 1;
    assert(delete a === true);

    assertThrowsError(() => {
        a;
    }, {
        error: ReferenceError
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
