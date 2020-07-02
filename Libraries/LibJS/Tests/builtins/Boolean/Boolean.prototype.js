load("test-common.js");

try {
    assert(typeof Boolean.prototype === "object");
    assert(Boolean.prototype.valueOf() === false);

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
