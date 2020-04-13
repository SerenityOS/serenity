load("test-common.js");

try {
    var names = Object.getOwnPropertyNames([1, 2, 3]);

    assert(names.length === 4);
    assert(names[0] === '0');
    assert(names[1] === '1');
    assert(names[2] === '2');
    assert(names[3] === 'length');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
