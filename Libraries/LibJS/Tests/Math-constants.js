load("test-common.js");

try {
    assert(isClose(Math.E, 2.718281));
    assert(isClose(Math.LN2, 0.693147));
    assert(isClose(Math.LN10, 2.302585));
    assert(isClose(Math.LOG2E, 1.442695));
    assert(isClose(Math.LOG10E, 0.434294));
    assert(isClose(Math.PI, 3.1415926));
    assert(isClose(Math.SQRT1_2, 0.707106));
    assert(isClose(Math.SQRT2, 1.414213));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
