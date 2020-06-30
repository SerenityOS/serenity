load("test-common.js");

try {
    assert(isNaN(Math.cbrt(NaN)));
    // FIXME: assert(Math.cbrt(-1) === -1);
    assert(Math.cbrt(-0) === -0);
    // FIXME: assert(Math.cbrt(-Infinity) === -Infinity);
    // FIXME: assert(Math.cbrt(1) === 1);
    // FIXME: assert(Math.cbrt(Infinity) === Infinity);
    assert(Math.cbrt(null) === 0);
    // FIXME: assert(isClose(Math.cbrt(2), 1.259921));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
