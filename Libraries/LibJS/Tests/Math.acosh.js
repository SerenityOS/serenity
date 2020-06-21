load("test-common.js");

try {
    assert(isNaN(Math.acosh(-1)));
    assert(isNaN(Math.acosh(0)));
    assert(isNaN(Math.acosh(0.5)));
    assert(isClose(Math.acosh(1), 0));
    assert(isClose(Math.acosh(2), 1.316957));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
