load("test-common.js");

try {
    assert(Array.length === 1);
    assert(Array.name === "Array");
    assert(Array.prototype.length === 0);

    assert(typeof Array() === "object");
    assert(typeof new Array() === "object");

    var a;

    a = new Array(5);
    assert(a.length === 5);

    a = new Array("5");
    assert(a.length === 1);
    assert(a[0] === "5");

    a = new Array(1, 2, 3);
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);

    a = new Array([1, 2, 3]);
    assert(a.length === 1);
    assert(a[0][0] === 1);
    assert(a[0][1] === 2);
    assert(a[0][2] === 3);

    [-1, -100, -0.1, 0.1, 1.23, Infinity, -Infinity, NaN].forEach(value => {
        assertThrowsError(() => {
            new Array(value);
        }, {
            error: TypeError,
            message: "Invalid array length"
        });
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
