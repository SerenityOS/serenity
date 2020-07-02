load("test-common.js");

try {
    assert(Array.prototype.join.length === 1);

    assert(["hello", "friends"].join() === "hello,friends");
    assert(["hello", "friends"].join(" ") === "hello friends");
    assert(["hello", "friends", "foo"].join("~", "#") === "hello~friends~foo");
    assert([].join() === "");
    assert([null].join() === "");
    assert([undefined].join() === "");
    assert([undefined, null, ""].join() === ",,");
    assert([1, null, 2, undefined, 3].join() === "1,,2,,3");
    assert(Array(3).join() === ",,");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
