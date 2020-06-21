load("test-common.js");

try {
    assert(isNaN(Math.atanh(-2)));
    assert(Math.atanh(-1) === -Infinity);
    assert(Math.atanh(0) === 0);
    assert(isClose(Math.atanh(0.5), 0.549306));
    assert(Math.atanh(1) === Infinity);
    assert(isNaN(Math.atanh(2)));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
