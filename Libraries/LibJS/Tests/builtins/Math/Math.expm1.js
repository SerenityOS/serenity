load("test-common.js");

try {
    assert(Math.expm1.length === 1);

    assert(Math.expm1(0) === 0);
    assert(isClose(Math.expm1(-2), -0.864664));
    assert(isClose(Math.expm1(-1), -0.632120));
    assert(isClose(Math.expm1(1), 1.718281));
    assert(isClose(Math.expm1(2), 6.389056));

    assert(isNaN(Math.expm1()));
    assert(isNaN(Math.expm1(undefined)));
    assert(isNaN(Math.expm1("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
