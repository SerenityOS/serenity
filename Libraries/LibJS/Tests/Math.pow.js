load("test-common.js");

try {
    assert(Math.pow(2, 0) === 1);
    assert(Math.pow(2, 1) === 2);
    assert(Math.pow(2, 2) === 4);
    assert(Math.pow(2, 3) === 8);
    assert(Math.pow(2, -3) === 0.125);
    assert(Math.pow(3, 2) === 9);
    assert(Math.pow(0, 0) === 1);
    assert(Math.pow(2, Math.pow(3, 2)) === 512);
    assert(Math.pow(Math.pow(2, 3), 2) === 64);
    assert(Math.pow("2", "3") === 8);
    assert(Math.pow("", []) === 1);
    assert(Math.pow([], null) === 1);
    assert(Math.pow(null, null) === 1);
    assert(Math.pow(undefined, null) === 1);
    assert(isNaN(Math.pow(NaN, 2)));
    assert(isNaN(Math.pow(2, NaN)));
    assert(isNaN(Math.pow(undefined, 2)));
    assert(isNaN(Math.pow(2, undefined)));
    assert(isNaN(Math.pow(null, undefined)));
    assert(isNaN(Math.pow(2, "foo")));
    assert(isNaN(Math.pow("foo", 2)));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
