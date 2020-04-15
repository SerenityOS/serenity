load("test-common.js");

try {
    var a = [1, 2, 3];

    assert(typeof a === "object");
    assert(a.length === 3);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);

    a[1] = 5;
    assert(a[1] === 5);
    assert(a.length === 3);

    a.push(7);
    assert(a[3] === 7);
    assert(a.length === 4);

    a = [,];
    assert(a.length === 1);
    assert(a.toString() === "");
    assert(a[0] === undefined);

    a = [,,,,];
    assert(a.length === 4);
    assert(a.toString() === ",,,");
    assert(a[0] === undefined);
    assert(a[1] === undefined);
    assert(a[2] === undefined);
    assert(a[3] === undefined);

    a = [1,,2,,,3,];
    assert(a.length === 6);
    assert(a.toString() === "1,,2,,,3");
    assert(a[0] === 1);
    assert(a[1] === undefined);
    assert(a[2] === 2);
    assert(a[3] === undefined);
    assert(a[4] === undefined);
    assert(a[5] === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
