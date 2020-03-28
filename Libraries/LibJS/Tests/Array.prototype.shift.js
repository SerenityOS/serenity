function assert(x) { if (!x) throw 1; }

try {
    var a = [1, 2, 3];
    var value = a.shift();
    assert(value === 1);
    assert(a.length === 2);
    assert(a[0] === 2);
    assert(a[1] === 3);

    var a = [];
    var value = a.shift();
    assert(value === undefined);
    assert(a.length === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
