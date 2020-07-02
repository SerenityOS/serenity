load("test-common.js");

try {
    assert(Number.isInteger.length === 1);

    assert(Number.isInteger(0) === true);
    assert(Number.isInteger(42) === true);
    assert(Number.isInteger(-10000) === true);
    assert(Number.isInteger(5) === true);
    assert(Number.isInteger(5.0) === true);
    assert(Number.isInteger(5 + 1/10000000000000000) === true);
    // FIXME: values outside of i32's range should still return true
    // assert(Number.isInteger(+2147483647 + 1) === true);
    // assert(Number.isInteger(-2147483648 - 1) === true);
    // assert(Number.isInteger(99999999999999999999999999999999999) === true);

    assert(Number.isInteger(5 + 1/1000000000000000) === false);
    assert(Number.isInteger(1.23) === false);
    assert(Number.isInteger("") === false);
    assert(Number.isInteger("0") === false);
    assert(Number.isInteger("42") === false);
    assert(Number.isInteger(true) === false);
    assert(Number.isInteger(false) === false);
    assert(Number.isInteger(null) === false);
    assert(Number.isInteger([]) === false);
    assert(Number.isInteger(Infinity) === false);
    assert(Number.isInteger(-Infinity) === false);
    assert(Number.isInteger(NaN) === false);
    assert(Number.isInteger() === false);
    assert(Number.isInteger(undefined) === false);
    assert(Number.isInteger("foo") === false);
    assert(Number.isInteger({}) === false);
    assert(Number.isInteger([1, 2, 3]) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
