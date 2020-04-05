try {
    function foo() { }
    assert(foo.length === 0);
    assert((foo.length = 5) === 5);
    assert(foo.length === 0);

    function bar(a, b, c) {}
    assert(bar.length === 3);
    assert((bar.length = 5) === 5);
    assert(bar.length === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
