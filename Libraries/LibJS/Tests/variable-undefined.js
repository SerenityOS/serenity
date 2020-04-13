load("test-common.js");

function foo(a) {
    return a;
}

try {
    var x = undefined;
    assert(x === undefined);
    assert(foo(x) === undefined);

    var o = {};
    o.x = x;
    assert(o.x === undefined);
    assert(o.x === x);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
