// Borrowed from LibM/TestMath.cpp :^)
function expectClose(a, b) { assert(Math.abs(a - b) < 0.000001); }

try {
    expectClose(Math.E, 2.718281);
    expectClose(Math.LN2, 0.693147);
    expectClose(Math.LN10, 2.302585);
    expectClose(Math.LOG2E, 1.442695);
    expectClose(Math.LOG10E, 0.434294);
    expectClose(Math.PI, 3.1415926);
    expectClose(Math.SQRT1_2, 0.707106);
    expectClose(Math.SQRT2, 1.414213);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
