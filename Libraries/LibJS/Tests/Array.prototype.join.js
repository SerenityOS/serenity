load("test-common.js");

try {
    assert(Array.prototype.join.length === 1);

    assert(["hello", "friends"].join() === "hello,friends");
    assert(["hello", "friends"].join(" ") === "hello friends");
    assert(Array(3).join() === ",,");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
