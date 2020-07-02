load("test-common.js");

try {
    assert(Array.isArray.length === 1);

    assert(Array.isArray() === false);
    assert(Array.isArray("1") === false);
    assert(Array.isArray("foo") === false);
    assert(Array.isArray(1) === false);
    assert(Array.isArray(1, 2, 3) === false);
    assert(Array.isArray(undefined) === false);
    assert(Array.isArray(null) === false);
    assert(Array.isArray(Infinity) === false);
    assert(Array.isArray({}) === false);

    assert(Array.isArray([]) === true);
    assert(Array.isArray([1]) === true);
    assert(Array.isArray([1, 2, 3]) === true);
    assert(Array.isArray(new Array()) === true);
    assert(Array.isArray(new Array(10)) === true);
    assert(Array.isArray(new Array("a", "b", "c")) === true);
    // FIXME: Array.prototype is supposed to be an array!
    // assert(Array.isArray(Array.prototype) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
