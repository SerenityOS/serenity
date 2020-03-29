function assert(x) { if (!x) throw 1; }

try {
    var d = new Date();
    assert(!isNaN(d.getDay()));
    assert(0 <= d.getDay() <= 6);
    assert(d.getDay() === d.getDay());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
