load("test-common.js");

try {
    assert(typeof Object.getPrototypeOf("") === "object");
    assert(Object.getPrototypeOf("").valueOf() === '');

    assert(typeof String.prototype === "object");
    assert(String.prototype.valueOf() === '');

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
