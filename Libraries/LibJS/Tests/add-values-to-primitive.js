load("test-common.js");

try {
    // Note that these will give different results in the REPL due to parsing behavior.
    assert([] + [] === "");
    assert([] + {} === "[object Object]");
    assert({} + {} === "[object Object][object Object]");
    assert({} + [] === "[object Object]");
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
