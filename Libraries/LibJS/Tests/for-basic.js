load("test-common.js");

try {
    var a = [];
    for (var i = 0; i < 3; ++i) {
        a.push(i);
    }
    assert(a.length === 3);
    assert(a[0] === 0);
    assert(a[1] === 1);
    assert(a[2] === 2);

    for (; a.length < 6;) {
        a.push('x');
    }
    assert(a.length === 6);
    assert(a[3] === 'x');
    assert(a[4] === 'x');
    assert(a[5] === 'x');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
