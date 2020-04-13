load("test-common.js");

try {
    assert(Array.prototype.unshift.length === 1);

    var a = ["hello"];
    var length = a.unshift();
    assert(length === 1);
    assert(a.length === 1);
    assert(a[0] === "hello");

    length = a.unshift("friends");
    assert(length === 2);
    assert(a.length === 2);
    assert(a[0] === "friends");
    assert(a[1] === "hello");

    length = a.unshift(1, 2, 3);
    assert(length === 5);
    assert(a.length === 5);
    assert(a[0] === 1);
    assert(a[1] === 2);
    assert(a[2] === 3);
    assert(a[3] === "friends");
    assert(a[4] === "hello");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
