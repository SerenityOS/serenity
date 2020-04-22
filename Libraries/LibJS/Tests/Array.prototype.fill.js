load("test-common.js");

try {
    assert(Array.prototype.fill.length === 3);

    var array = [1, 2, 3];

    assert(array.fill(4) === [4, 4, 4]);
    assert(array.fill(4, 1) === [1, 4, 4]);
    assert(array.fill(4, 1, 2) === [1, 4, 3]);
    assert(array.fill(4, 3, 3) == [1, 2, 3]);
    assert(array.fill(4, -3, -2) === [4, 2, 3]);
    assert(array.fill(4, NaN, NaN) === [1, 2, 3]);
    assert(array.fill(4, 3, 5) === [1, 2, 3]);
    assert(Array(3).fill(4) === [4, 4, 4]);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
