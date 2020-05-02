load("test-common.js");

try {
    assert(Date.length === 7);
    assert(Date.name === "Date");
    assert(Date.prototype.length === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
