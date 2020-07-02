load("test-common.js");

try {
    assert(Array.prototype.push.length === 1);

    var a = ["hello"];
    var length = a.push();
    assert(length === 1);
    assert(a.length === 1);
    assert(a[0] === "hello");

    length = a.push("friends");
    assert(length === 2);
    assert(a.length === 2);
    assert(a[0] === "hello");
    assert(a[1] === "friends");

    length = a.push(1, 2, 3);
    assert(length === 5);
    assert(a.length === 5);
    assert(a[0] === "hello");
    assert(a[1] === "friends");
    assert(a[2] === 1);
    assert(a[3] === 2);
    assert(a[4] === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
