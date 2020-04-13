load("test-common.js");

try {
    assert(2 ** 0 === 1);
    assert(2 ** 1 === 2);
    assert(2 ** 2 === 4);
    assert(2 ** 3 === 8);
    assert(2 ** -3 === 0.125);
    assert(3 ** 2 === 9);
    assert(0 ** 0 === 1);
    assert(2 ** 3 ** 2 === 512);
    assert(2 ** (3 ** 2) === 512);
    assert((2 ** 3) ** 2 === 64);
    assert("2" ** "3" === 8);
    assert("" ** [] === 1);
    assert([] ** null === 1);
    assert(null ** null === 1);
    assert(undefined ** null === 1);
    assert(isNaN(NaN ** 2));
    assert(isNaN(2 ** NaN));
    assert(isNaN(undefined ** 2));
    assert(isNaN(2 ** undefined));
    assert(isNaN(null ** undefined));
    assert(isNaN(2 ** "foo"));
    assert(isNaN("foo" ** 2));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
