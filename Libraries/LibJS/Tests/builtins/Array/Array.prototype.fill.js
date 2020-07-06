load("test-common.js");

try {
    assert(Array.prototype.fill.length === 1);

    var array = [1, 2, 3, 4];
    assertArrayEquals(array.fill(0, 2, 4), [1, 2, 0, 0]);
    assertArrayEquals(array.fill(5, 1), [1, 5, 5, 5]);
    assertArrayEquals(array.fill(6), [6, 6, 6, 6]);

    assertArrayEquals([1, 2, 3].fill(4), [4, 4, 4]);
    assertArrayEquals([1, 2, 3].fill(4, 1), [1, 4, 4]);
    assertArrayEquals([1, 2, 3].fill(4, 1, 2), [1, 4, 3]);
    assertArrayEquals([1, 2, 3].fill(4, 3, 3), [1, 2, 3]);
    assertArrayEquals([1, 2, 3].fill(4, -3, -2), [4, 2, 3]);
    assertArrayEquals([1, 2, 3].fill(4, NaN, NaN), [1, 2, 3]);
    assertArrayEquals([1, 2, 3].fill(4, 3, 5), [1, 2, 3]);
    assertArrayEquals(Array(3).fill(4), [4, 4, 4]);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
