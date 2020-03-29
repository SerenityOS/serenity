function assert(x) { if (!x) throw 1; }

try {
    var d = new Date();
    assert(!isNaN(d.getFullYear()));
    assert(d.getFullYear() >= 2020);
    assert(d.getFullYear() === d.getFullYear());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
