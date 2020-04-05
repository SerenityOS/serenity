function assert(x) { if (!x) throw 1; }

try {
    assert(Math.trunc(13.37) === 13);
    assert(Math.trunc(42.84) === 42);
    assert(Math.trunc(0.123) ===  0);
    assert(Math.trunc(-0.123) === -0);

    assert(isNaN(Math.trunc(NaN)));
    assert(isNaN(Math.trunc('foo')));
    assert(isNaN(Math.trunc()));

    assert(Math.trunc.length === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
