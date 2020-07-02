load("test-common.js");

try {
    assert(Math.floor(0.95) === 0);
    assert(Math.floor(4) === 4);
    assert(Math.floor(7.004) == 7);
    assert(Math.floor(-0.95) === -1);
    assert(Math.floor(-4)    === -4);
    assert(Math.floor(-7.004) === -8);

    assert(isNaN(Math.floor()));
    assert(isNaN(Math.floor(NaN)));

    assert(Math.floor.length === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
