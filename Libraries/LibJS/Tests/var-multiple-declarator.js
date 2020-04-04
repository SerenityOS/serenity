function assert(x) { if (!x) throw 1; }

try {
    var a = 1, b = 2, c = a + b;
    assert(a === 1);
    assert(b === 2);
    assert(c === 3);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
