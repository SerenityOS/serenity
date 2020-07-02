load("test-common.js");

try {
    assert(Math.ceil(0.95) === 1);
    assert(Math.ceil(4) === 4);
    assert(Math.ceil(7.004) == 8);
    assert(Math.ceil(-0.95) === -0);
    assert(Math.ceil(-4)    === -4);
    assert(Math.ceil(-7.004) === -7);

    assert(isNaN(Math.ceil()));
    assert(isNaN(Math.ceil(NaN)));

    assert(Math.ceil.length === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
