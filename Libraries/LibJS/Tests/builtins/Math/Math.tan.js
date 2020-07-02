load("test-common.js");

try {
    assert(Math.tan(0) === 0);
    assert(Math.tan(null) === 0);
    assert(Math.tan('') === 0);
    assert(Math.tan([]) === 0);
    assert(Math.ceil(Math.tan(Math.PI / 4)) === 1);
    assert(isNaN(Math.tan()));
    assert(isNaN(Math.tan(undefined)));
    assert(isNaN(Math.tan([1, 2, 3])));
    assert(isNaN(Math.tan({})));
    assert(isNaN(Math.tan("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
