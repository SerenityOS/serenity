load("test-common.js");

try {
    assertThrowsError(() => {
        for (var i = foo; i < 100; ++i) {
            assertNotReached();
        }
    }, {
        error: ReferenceError,
        message: "'foo' is not defined"
    });

    assertThrowsError(() => {
        for (var i = 0; i < foo; ++i) {
            assertNotReached();
        }
    }, {
        error: ReferenceError,
        message: "'foo' is not defined"
    });

    var loopCount = 0;
    assertThrowsError(() => {
        for (var i = 0; i < 100; ++foo) {
            loopCount++;
        }
    }, {
        error: ReferenceError,
        message: "'foo' is not defined"
    });
    assert(loopCount === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
