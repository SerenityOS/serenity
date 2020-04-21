load("test-common.js");

try {
    assertThrowsError(() => {
        ++x;
    }, {
        error: ReferenceError,
        message: "'x' not known"
    });

    var n = 0;
    assert(++n === 1);
    assert(n === 1);

    var n = 0;
    assert(n++ === 0);
    assert(n === 1);

    var n = 0;
    assert(--n === -1);
    assert(n === -1);

    var n = 0;
    assert(n-- === 0);
    assert(n === -1);

    var a = [];
    assert(a++ === 0);
    assert(a === 1);

    var b = true;
    assert(b-- === 1);
    assert(b === 0);

    var s = "foo";
    assert(isNaN(++s));
    assert(isNaN(s));

    var s = "foo";
    assert(isNaN(s++));
    assert(isNaN(s));

    var s = "foo";
    assert(isNaN(--s));
    assert(isNaN(s));

    var s = "foo";
    assert(isNaN(s--));
    assert(isNaN(s));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
