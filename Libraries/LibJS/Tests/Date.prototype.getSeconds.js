load("test-common.js");

try {
    var d = new Date();
    assert(!isNaN(d.getSeconds()));
    assert(0 <= d.getSeconds() <= 59);
    assert(d.getSeconds() === d.getSeconds());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
