load("test-common.js");

try {
    debugger;

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
