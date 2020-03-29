function assert(x) { if (!x) throw 1; }

try {
    var last = 0;
    for (var i = 0; i < 100; ++i) {
        var now = Date.now();
        assert(!isNaN(now))
        assert(now > 1580000000000);
        assert(now >= last);
        last = now;
    }
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
