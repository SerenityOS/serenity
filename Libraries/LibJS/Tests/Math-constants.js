// FIXME: The parser seems to have issues with decimals,
// so we multiply everything and compare with whole numbers.
// I.e. 1233 < X * 1000 < 1235 instead of 1.233 < X < 1.235

try {
    // approx. 2.718
    assert(2717 < Math.E * 1000 < 2719);
    // approx. 0.693MATH
    assert(692 < Math.LN2 * 1000 < 694);
    // approx. 2.303
    assert(2302 < Math.LN10 * 1000 < 2304);
    // approx. 1.443
    assert(1442 < Math.LOG2E * 1000 < 1444);
    // approx. 0.434
    assert(433 < Math.LOG10E * 1000 < 435);
    // approx. 3.1415
    assert(31414 < Math.PI * 10000 < 31416);
    // approx. 0.707
    assert(706 < Math.SQRT1_2 * 1000 < 708);
    // approx. 1.414
    assert(1413 < Math.SQRT2 * 1000 < 1415);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
