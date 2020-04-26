load("test-common.js");

try {
    assert(Number.isNaN.length === 1);

    assert(Number.isNaN(0) === false);
    assert(Number.isNaN(42) === false);
    assert(Number.isNaN("") === false);
    assert(Number.isNaN("0") === false);
    assert(Number.isNaN("42") === false);
    assert(Number.isNaN(true) === false);
    assert(Number.isNaN(false) === false);
    assert(Number.isNaN(null) === false);
    assert(Number.isNaN([]) === false);
    assert(Number.isNaN(Infinity) === false);
    assert(Number.isNaN(-Infinity) === false);
    assert(Number.isNaN() === false);
    assert(Number.isNaN(undefined) === false);
    assert(Number.isNaN("foo") === false);
    assert(Number.isNaN({}) === false);
    assert(Number.isNaN([1, 2, 3]) === false);

    assert(Number.isNaN(NaN) === true);
    assert(Number.isNaN(Number.NaN) === true);
    assert(Number.isNaN(0 / 0) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
