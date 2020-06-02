load("test-common.js");

try {
    assert(Object.setPrototypeOf.length === 2);

    o = {};
    assert(Object.setPrototypeOf(o, {}) === o);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
