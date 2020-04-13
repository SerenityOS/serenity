load("test-common.js");

try {
    var d = new Date();
    assert(!isNaN(d.getFullYear()));
    assert(d.getFullYear() >= 2020);
    assert(d.getFullYear() === d.getFullYear());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
