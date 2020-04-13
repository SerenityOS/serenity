load("test-common.js");

try {
    assert((function() {}).toString() === "function () {\n  ???\n}");
    assert((function(foo) {}).toString() === "function (foo) {\n  ???\n}");
    assert((function(foo, bar, baz) {}).toString() === "function (foo, bar, baz) {\n  ???\n}");
    assert((function(foo, bar, baz) {
        if (foo) {
            return baz;
        } else if (bar) {
            return foo;
        }
        return bar + 42;
    }).toString() === "function (foo, bar, baz) {\n  ???\n}");
    assert(console.log.toString() === "function log() {\n  [NativeFunction]\n}");
    assert(Function.toString() === "function Function() {\n  [FunctionConstructor]\n}");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
