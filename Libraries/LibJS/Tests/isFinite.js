load("test-common.js");

try {
    assert(isFinite.length === 1);

    assert(isFinite(0) === true);
    assert(isFinite(1.23) === true);
    assert(isFinite(42) === true);
    assert(isFinite("") === true);
    assert(isFinite("0") === true);
    assert(isFinite("42") === true);
    assert(isFinite(true) === true);
    assert(isFinite(false) === true);
    assert(isFinite(null) === true);
    assert(isFinite([]) === true);

    assert(isFinite() === false);
    assert(isFinite(NaN) === false);
    assert(isFinite(undefined) === false);
    assert(isFinite(Infinity) === false);
    assert(isFinite(-Infinity) === false);
    assert(isFinite("foo") === false);
    assert(isFinite({}) === false);
    assert(isFinite([1, 2, 3]) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
