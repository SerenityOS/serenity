load("test-common.js");

try {
    // Ensuring it's the same function as the global
    // parseFloat() is enough as that already has tests :^)
    assert(Number.parseFloat === parseFloat);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
