load("test-common.js");

try {
    assert(Array.of.length === 0);

    assert(typeof Array.of() === "object");

    var a;

    a = Array.of(5);
    assert(a.length === 1);
    assert(a[0] === 5);

    a = Array.of("5");
    assert(a.length === 1);
    assert(a[0] === "5");

    a = Array.of(Infinity);
    assert(a.length === 1);
    assert(a[0] === Infinity);

    a = Array.of(1, 2, 3);
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);

    a = Array.of([1, 2, 3]);
    assert(a.length === 1);
    assert(a[0][0] === 1);
    assert(a[0][1] === 2);
    assert(a[0][2] === 3);

    let t = [1, 2, 3];
    Object.defineProperty(t, 3, {
        get() {
            return 4;
        },
    });
    a = Array.of(...t);
    assert(a.length === 4);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(a[3] === 4);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
