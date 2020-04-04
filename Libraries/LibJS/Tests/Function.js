function assert(x) { if (!x) throw 1; }

try {
    assert(Function.length === 1);
    assert(Function.prototype.length === 0);
    assert(typeof Function() === "function");
    assert(typeof new Function() === "function");
    assert(Function()() === undefined);
    assert(new Function()() === undefined);
    assert(Function("return 42")() === 42);
    assert(new Function("return 42")() === 42);
    assert(new Function("foo", "return foo")(42) === 42);
    assert(new Function("foo,bar", "return foo + bar")(1, 2) === 3);
    assert(new Function("foo", "bar", "return foo + bar")(1, 2) === 3);
    assert(new Function("foo", "bar,baz", "return foo + bar + baz")(1, 2, 3) === 6);
    assert(new Function("foo", "bar", "baz", "return foo + bar + baz")(1, 2, 3) === 6);
    assert(new Function("foo", "if (foo) { return 42; } else { return 'bar'; }")(true) === 42);
    assert(new Function("foo", "if (foo) { return 42; } else { return 'bar'; }")(false) === "bar");
    assert(new Function("return typeof Function()")() === "function");
    // FIXME: This is equivalent to
    //   (function (x) { return function (y) { return x + y;} })(1)(2)
    // and should totally work, but both currently fail with
    //   Uncaught exception: [ReferenceError]: 'x' not known
    // assert(new Function("x", "return function (y) { return x + y };")(1)(2) === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
