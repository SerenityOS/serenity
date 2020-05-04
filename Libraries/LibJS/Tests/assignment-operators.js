load("test-common.js");

try {
    var x;

    x = 1;
    assert((x = 2) === 2);
    assert(x === 2);

    x = 1;
    assert((x += 2) === 3);
    assert(x === 3);

    x = 3;
    assert((x -= 2) === 1);
    assert(x === 1);

    x = 3;
    assert((x *= 2) === 6);
    assert(x === 6);

    x = 6;
    assert((x /= 2) === 3);
    assert(x === 3);

    x = 2;
    assert((x <<= 2) === 8);
    assert(x === 8);

    x = 8;
    assert((x >>= 2) === 2);
    assert(x === 2);

    x = -(2 ** 32 - 10);
    assert((x >>>= 2) === 2);
    assert(x === 2);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
