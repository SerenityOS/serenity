load("test-common.js");

try {
    assert(Array.prototype.indexOf.length === 1);

    var array = ['hello', 'friends', 1, 2, false];

    assert(array.indexOf('hello') === 0);
    assert(array.indexOf('friends') === 1);
    assert(array.indexOf(false) === 4);
    assert(array.indexOf(false, 2) === 4);
    assert(array.indexOf(false, -2) === 4);
    assert(array.indexOf(1) === 2);
    assert(array.indexOf(1, 1000) === -1);
    assert(array.indexOf(1, -1000) === 2);
    assert(array.indexOf('serenity') === -1);
    assert(array.indexOf(false, -1) === 4);
    assert(array.indexOf(2, -1) === -1);
    assert(array.indexOf(2, -2) === 3);
    assert([].indexOf('serenity') === -1);
    assert([].indexOf('serenity', 10) === -1);
    assert([].indexOf('serenity', -10) === -1);
    assert([].indexOf() === -1);
    assert([undefined].indexOf() === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
