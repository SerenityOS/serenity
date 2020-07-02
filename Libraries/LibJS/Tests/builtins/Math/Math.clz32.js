load("test-common.js");

try {
    assert(Math.clz32.length === 1);

    assert(Math.clz32(0) === 32);
    assert(Math.clz32(1) === 31);
    assert(Math.clz32(2) === 30);
    assert(Math.clz32(3) === 30);
    assert(Math.clz32(4) === 29);
    assert(Math.clz32(5) === 29);
    assert(Math.clz32(-1) === 0);
    assert(Math.clz32(-10) === 0);
    assert(Math.clz32(-100) === 0);
    assert(Math.clz32(-1000) === 0);
    assert(Math.clz32(-0.123) === 32);
    assert(Math.clz32(0.123) === 32);
    assert(Math.clz32(1.23) === 31);
    assert(Math.clz32(12) === 28);
    assert(Math.clz32(123) === 25);
    assert(Math.clz32(1234) === 21);
    assert(Math.clz32(12345) === 18);
    assert(Math.clz32(123456) === 15);
    assert(Math.clz32(1234567) === 11);
    assert(Math.clz32(12345678) === 8);
    assert(Math.clz32(123456789) === 5);
    assert(Math.clz32(999999999) === 2);
    assert(Math.clz32(9999999999) === 1);
    assert(Math.clz32(99999999999) === 1);
    assert(Math.clz32(999999999999) === 0);
    assert(Math.clz32(9999999999999) === 1);
    assert(Math.clz32(99999999999999) === 3);
    assert(Math.clz32(999999999999999) === 0);

    assert(Math.clz32() === 32);
    assert(Math.clz32(NaN) === 32);
    assert(Math.clz32(Infinity) === 32);
    assert(Math.clz32(-Infinity) === 32);
    assert(Math.clz32(false) === 32);
    assert(Math.clz32(true) === 31);
    assert(Math.clz32(null) === 32);
    assert(Math.clz32(undefined) === 32);
    assert(Math.clz32([]) === 32);
    assert(Math.clz32({}) === 32);
    assert(Math.clz32("foo") === 32);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
