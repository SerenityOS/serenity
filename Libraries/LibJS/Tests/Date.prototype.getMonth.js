load("test-common.js");

try {
    var d = new Date();
    assert(!isNaN(d.getMonth()));
    assert(0 <= d.getMonth() <= 11);
    assert(d.getMonth() === d.getMonth());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
