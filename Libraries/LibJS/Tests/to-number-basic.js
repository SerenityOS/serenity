function assert(x) { if (!x) throw 1; }

// FIXME: Just "+x" doesn't parse currently,
// so we use "x - 0", which is effectively the same.
// "0 + x" would _not_ work in all cases.
function n(x) { return x - 0; }

try {
    assert(n(false) === 0);
    assert(n(true) === 1);
    assert(n(null) === 0);
    assert(n([]) === 0);
    assert(n([[[[[]]]]]) === 0);
    assert(n([[[[[42]]]]]) === 42);
    assert(n("") === 0);
    assert(n("42") === 42);
    assert(n(42) === 42);
    // FIXME: returns NaN
    // assert(n("1.23") === 1.23)
    // FIXME: chokes on ASSERT
    // assert(n(1.23) === 1.23);

    assert(isNaN(n(undefined)));
    assert(isNaN(n({})));
    assert(isNaN(n({a: 1})));
    assert(isNaN(n([1, 2, 3])));
    assert(isNaN(n([[[["foo"]]]])));
    assert(isNaN(n("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
