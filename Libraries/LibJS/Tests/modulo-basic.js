function assert(x) { if (!x) throw 1; }

try {
    assert(10 % 3 === 1);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
