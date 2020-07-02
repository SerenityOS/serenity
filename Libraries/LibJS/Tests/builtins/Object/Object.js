load("test-common.js");

try {
    assert(Object.length === 1);
    assert(Object.name === "Object");
    assert(Object.prototype.length === undefined);

    assert(typeof Object() === "object");
    assert(typeof new Object() === "object");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
