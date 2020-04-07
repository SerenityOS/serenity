try {
    assert(Number.isSafeInteger.length === 1);
    assert(Number.isSafeInteger(0) === true);
    assert(Number.isSafeInteger(1) === true);
    assert(Number.isSafeInteger(2.0) === true);
    assert(Number.isSafeInteger(42) === true);
    assert(Number.isSafeInteger(Number.MAX_SAFE_INTEGER) === true);
    assert(Number.isSafeInteger(Number.MIN_SAFE_INTEGER) === true);
    assert(Number.isSafeInteger() === false);
    assert(Number.isSafeInteger("1") === false);
    assert(Number.isSafeInteger(2.1) === false);
    assert(Number.isSafeInteger(42.42) === false);
    assert(Number.isSafeInteger("") === false);
    assert(Number.isSafeInteger([]) === false);
    assert(Number.isSafeInteger(null) === false);
    assert(Number.isSafeInteger(undefined) === false);
    assert(Number.isSafeInteger(NaN) === false);
    assert(Number.isSafeInteger(Infinity) === false);
    assert(Number.isSafeInteger(-Infinity) === false);
    assert(Number.isSafeInteger(Number.MAX_SAFE_INTEGER + 1) === false);
    assert(Number.isSafeInteger(Number.MIN_SAFE_INTEGER - 1) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
