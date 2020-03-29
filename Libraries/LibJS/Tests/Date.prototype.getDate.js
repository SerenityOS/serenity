function assert(x) { if (!x) throw 1; }

try {
    var d = new Date();
    assert(!isNaN(d.getDate()));
    assert(1 <= d.getDate() <= 31);
    assert(d.getDate() === d.getDate());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
