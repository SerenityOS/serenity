load("test-common.js");

try {
    function foo() { }
    assert(foo.length === 0);
    assert((foo.length = 5) === 5);
    assert(foo.length === 0);

    function bar(a, b, c) {}
    assert(bar.length === 3);
    assert((bar.length = 5) === 5);
    assert(bar.length === 3);

    function baz(a, b = 1, c) {}
    assert(baz.length === 1);
    assert((baz.length = 5) === 5);
    assert(baz.length === 1);

    function qux(a, b, ...c) {}
    assert(qux.length === 2);
    assert((qux.length = 5) === 5);
    assert(qux.length === 2);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
