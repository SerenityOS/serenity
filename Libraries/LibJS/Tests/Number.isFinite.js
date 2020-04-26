load("test-common.js");

try {
    assert(Number.isFinite.length === 1);

    assert(Number.isFinite(0) === true);
    assert(Number.isFinite(1.23) === true);
    assert(Number.isFinite(42) === true);

    assert(Number.isFinite("") === false);
    assert(Number.isFinite("0") === false);
    assert(Number.isFinite("42") === false);
    assert(Number.isFinite(true) === false);
    assert(Number.isFinite(false) === false);
    assert(Number.isFinite(null) === false);
    assert(Number.isFinite([]) === false);
    assert(Number.isFinite() === false);
    assert(Number.isFinite(NaN) === false);
    assert(Number.isFinite(undefined) === false);
    assert(Number.isFinite(Infinity) === false);
    assert(Number.isFinite(-Infinity) === false);
    assert(Number.isFinite("foo") === false);
    assert(Number.isFinite({}) === false);
    assert(Number.isFinite([1, 2, 3]) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
