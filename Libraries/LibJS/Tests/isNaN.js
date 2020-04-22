load("test-common.js");

try {
    assert(isNaN.length === 1);

    assert(isNaN(0) === false);
    assert(isNaN(42) === false);
    assert(isNaN("") === false);
    assert(isNaN("0") === false);
    assert(isNaN("42") === false);
    assert(isNaN(true) === false);
    assert(isNaN(false) === false);
    assert(isNaN(null) === false);
    assert(isNaN([]) === false);
    assert(isNaN(Infinity) === false);
    assert(isNaN(-Infinity) === false);

    assert(isNaN() === true);
    assert(isNaN(NaN) === true);
    assert(isNaN(undefined) === true);
    assert(isNaN("foo") === true);
    assert(isNaN({}) === true);
    assert(isNaN([1, 2, 3]) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
