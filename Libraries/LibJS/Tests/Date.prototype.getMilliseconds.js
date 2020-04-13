load("test-common.js");

try {
    var d = new Date();
    assert(!isNaN(d.getMilliseconds()));
    assert(0 <= d.getMilliseconds() <= 999);
    assert(d.getMilliseconds() === d.getMilliseconds());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
