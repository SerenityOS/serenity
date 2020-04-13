load("test-common.js");

try {
    var a = [1, 2, 3];
    var value = a.pop();
    assert(value === 3);
    assert(a.length === 2);
    assert(a[0] === 1);
    assert(a[1] === 2);

    var a = [];
    var value = a.pop();
    assert(value === undefined);
    assert(a.length === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
