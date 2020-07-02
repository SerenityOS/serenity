load("test-common.js");

try {
    assert(typeof Object.prototype.toString() === "string");
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
