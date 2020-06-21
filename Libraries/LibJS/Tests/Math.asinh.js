load("test-common.js");

try {
    assert(isClose(Math.asinh(0), 0));
    assert(isClose(Math.asinh(1), 0.881373));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
