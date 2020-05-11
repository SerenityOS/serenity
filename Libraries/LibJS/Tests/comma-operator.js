load("test-common.js");

try {
    assert((1, 2, 3) === 3);
    assert((1, 2 + 3, 4) === 4);

    var foo = 0;
    foo = (foo++, foo);
    assert(foo === 1);

    var a, b, c;
    assert((a = b = 3, c = 4) === 4);
    assert(a === 3);
    assert(b === 3);
    assert(c === 4);

    var x, y, z;
    assert((x = (y = 5, z = 6)) === 6);
    assert(x === 6)
    assert(y === 5)
    assert(z === 6)

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
