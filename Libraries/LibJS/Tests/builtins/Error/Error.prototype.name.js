load("test-common.js");

try {
    var changedInstance = new Error("");
    changedInstance.name = 'NewCustomError';
    assert(changedInstance.name === "NewCustomError");

    var normalInstance = new Error("");
    assert(normalInstance.name === "Error");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
