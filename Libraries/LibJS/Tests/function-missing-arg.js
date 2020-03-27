function assert(x) { if (!x) throw 1; }

function foo(a, b) { return a + b; }

try {
    assert(isNaN(foo()) === true);
    assert(isNaN(foo(1)) === true);
    assert(foo(2, 3) === 5);
    assert(foo(2, 3, 4) === 5);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
