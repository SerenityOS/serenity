function assert(x) { if (!x) throw 1; }

try {
    var d = new Date();
    assert(!isNaN(d.getMinutes()));
    assert(0 <= d.getMinutes() <= 59);
    assert(d.getMinutes() === d.getMinutes());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
