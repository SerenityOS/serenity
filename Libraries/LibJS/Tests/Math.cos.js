load("test-common.js");

try {
    assert(Math.cos(0) === 1);
    assert(Math.cos(null) === 1);
    assert(Math.cos('') === 1);
    assert(Math.cos([]) === 1);
    assert(Math.cos(Math.PI) === -1);
    assert(isNaN(Math.cos()));
    assert(isNaN(Math.cos(undefined)));
    assert(isNaN(Math.cos([1, 2, 3])));
    assert(isNaN(Math.cos({})));
    assert(isNaN(Math.cos("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
