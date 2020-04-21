load("test-common.js");

try {
    assert(Array.prototype.lastIndexOf.length === 1);

    var array = [1, 2, 3, 1, "hello"];

    assert(array.lastIndexOf("hello") === 4);
    assert(array.lastIndexOf(1) === 3);
    assert(array.lastIndexOf(1, -1) === -1);
    assert(array.lastIndexOf(1, -2) === 3);
    assert(array.lastIndexOf(2) === 1);
    assert(array.lastIndexOf(2, -3) === -1);
    assert(array.lastIndexOf(2, -4) === 1);
    assert([].lastIndexOf('hello') === -1);
    assert([].lastIndexOf('hello', 10) === -1);
    assert([].lastIndexOf('hello', -10) === -1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
