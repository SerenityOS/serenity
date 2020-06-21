load("test-common.js");

try {
    assert(isNaN(Math.cbrt(NaN)));
    assert(Math.cbrt(-1) === -1);
    assert(Math.cbrt(-0) === -0);
    assert(Math.cbrt(-Infinity) === -Infinity);
    assert(Math.cbrt(1) === 1);
    assert(Math.cbrt(Infinity) === Infinity);
    assert(Math.cbrt(null) === 0);
    assert(isClose(Math.cbrt(2), 1.259921));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
