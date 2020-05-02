load("test-common.js");

try {
    assert(Array.length === 1);
    assert(Array.name === "Array");
    assert(Array.prototype.length === 0);

    assert(typeof Array() === "object");
    assert(typeof new Array() === "object");

    var a = new Array(5);
    assert(a.length === 5);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
