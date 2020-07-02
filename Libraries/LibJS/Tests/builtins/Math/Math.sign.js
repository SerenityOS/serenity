load("test-common.js");

function isPositiveZero(value) {
    return value === 0 && 1 / value === Infinity;
}

function isNegativeZero(value) {
    return value === 0 && 1 / value === -Infinity;
}

try {
    assert(Math.sign.length === 1);

    assert(Math.sign(0.0001) === 1);
    assert(Math.sign(1) === 1);
    assert(Math.sign(42) === 1);
    assert(Math.sign(Infinity) === 1);
    assert(isPositiveZero(Math.sign(0)));
    assert(isPositiveZero(Math.sign(null)));
    assert(isPositiveZero(Math.sign('')));
    assert(isPositiveZero(Math.sign([])));

    assert(Math.sign(-0.0001) === -1);
    assert(Math.sign(-1) === -1);
    assert(Math.sign(-42) === -1);
    assert(Math.sign(-Infinity) === -1);
    assert(isNegativeZero(Math.sign(-0)));
    assert(isNegativeZero(Math.sign(-null)));
    assert(isNegativeZero(Math.sign(-'')));
    assert(isNegativeZero(Math.sign(-[])));

    assert(isNaN(Math.sign()));
    assert(isNaN(Math.sign(undefined)));
    assert(isNaN(Math.sign([1, 2, 3])));
    assert(isNaN(Math.sign({})));
    assert(isNaN(Math.sign(NaN)));
    assert(isNaN(Math.sign("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
