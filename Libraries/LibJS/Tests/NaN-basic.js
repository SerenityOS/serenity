function assert(x) { if (!x) throw 1; }

try {
    var nan = undefined + 1;
    assert(nan + "" == "NaN");
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
