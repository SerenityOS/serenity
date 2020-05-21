load("test-common.js");

try {
    assert(Array.prototype.includes.length === 1);

    var array = ['hello', 'friends', 1, 2, false];

    assert([].includes() === false);
    assert([undefined].includes() === true);
    assert(array.includes('hello') === true);
    assert(array.includes(1) === true);
    assert(array.includes(1, -3) === true);
    assert(array.includes('serenity') === false);
    assert(array.includes(false, -1) === true);
    assert(array.includes(2, -1) === false);
    assert(array.includes(2, -100) === true);
    assert(array.includes('friends', 100) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
