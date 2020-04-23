load("test-common.js");

try {
    assert((0 << 0) === 0);
    assert((0 << 1) === 0);
    assert((0 << 2) === 0);
    assert((0 << 3) === 0);
    assert((0 << 4) === 0);
    assert((0 << 5) === 0);

    assert((1 << 0) === 1);
    assert((1 << 1) === 2);
    assert((1 << 2) === 4);
    assert((1 << 3) === 8);
    assert((1 << 4) === 16);
    assert((1 << 5) === 32);

    assert((2 << 0) === 2);
    assert((2 << 1) === 4);
    assert((2 << 2) === 8);
    assert((2 << 3) === 16);
    assert((2 << 4) === 32);
    assert((2 << 5) === 64);

    assert((3 << 0) === 3);
    assert((3 << 1) === 6);
    assert((3 << 2) === 12);
    assert((3 << 3) === 24);
    assert((3 << 4) === 48);
    assert((3 << 5) === 96);

    assert((4 << 0) === 4);
    assert((4 << 1) === 8);
    assert((4 << 2) === 16);
    assert((4 << 3) === 32);
    assert((4 << 4) === 64);
    assert((4 << 5) === 128);

    assert((5 << 0) === 5);
    assert((5 << 1) === 10);
    assert((5 << 2) === 20);
    assert((5 << 3) === 40);
    assert((5 << 4) === 80);
    assert((5 << 5) === 160);

    var x = 3;
    var y = 7;
    assert(("42" << 6) === 2688);
    assert((x << y) === 384);
    assert((x << [[[[12]]]]) === 12288);
    assert((undefined << y) === 0);
    assert(("a" << "b") === 0);
    assert((null << null) === 0);
    assert((undefined << undefined) === 0);
    assert((NaN << NaN) === 0);
    assert((NaN << 6) === 0);
    assert((Infinity << Infinity) === 0);
    assert((-Infinity << Infinity) === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
