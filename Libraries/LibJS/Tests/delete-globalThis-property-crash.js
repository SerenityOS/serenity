load("test-common.js");

try {
    a = 1;
    assert(delete globalThis.a === true);
    a = 2;
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
