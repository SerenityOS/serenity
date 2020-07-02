load("test-common.js");

try {
    assert(Number.EPSILON === 2 ** -52);
    assert(Number.EPSILON > 0);
    assert(Number.MAX_SAFE_INTEGER === 2 ** 53 - 1);
    assert(Number.MAX_SAFE_INTEGER + 1 === Number.MAX_SAFE_INTEGER + 2);
    assert(Number.MIN_SAFE_INTEGER === -(2 ** 53 - 1));
    assert(Number.MIN_SAFE_INTEGER - 1 === Number.MIN_SAFE_INTEGER - 2);
    assert(Number.POSITIVE_INFINITY === Infinity);
    assert(Number.NEGATIVE_INFINITY === -Infinity);
    assert(isNaN(Number.NaN));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
