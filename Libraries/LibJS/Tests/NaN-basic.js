function assert(x) { if (!x) throw 1; }

try {
    var nan = undefined + 1;
    assert(nan + "" == "NaN");
    assert(isNaN(nan) === true);
    assert(isNaN(0) === false);
    assert(isNaN(undefined) === false);
    assert(isNaN(null) === false);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
