load("test-common.js");

try {
    assert(Math.exp.length === 1);

    assert(Math.exp(0) === 1);
    assert(isClose(Math.exp(-2), 0.135335));
    assert(isClose(Math.exp(-1), 0.367879));
    assert(isClose(Math.exp(1), 2.718281));
    assert(isClose(Math.exp(2), 7.389056));

    assert(isNaN(Math.exp()));
    assert(isNaN(Math.exp(undefined)));
    assert(isNaN(Math.exp("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
