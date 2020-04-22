load("test-common.js");

try {
    var a = [1, 2, 3];

    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);

    a.length = 5;
    assert(a.length === 5);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(a[3] === undefined);
    assert(a[4] === undefined);

    a.length = 1;
    assert(a.length === 1);
    assert(a[0] === 1);

    a.length = 0;
    assert(a.length === 0);

    a.length = "42";
    assert(a.length === 42);

    a.length = [];
    assert(a.length === 0);

    a.length = true;
    assert(a.length === 1);

    [undefined, "foo", -1, Infinity, -Infinity, NaN].forEach(value => {
        assertThrowsError(() => {
            a.length = value;
        }, {
            error: RangeError,
            message: "Invalid array length"
        });
        assert(a.length === 1);
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
