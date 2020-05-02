load("test-common.js");

try {
    assert(Function.length === 1);
    assert(Function.name === "Function");
    assert(Function.prototype.length === 0);
    assert(Function.prototype.name === "");

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
    assert(new Function("x", "return function (y) { return x + y };")(1)(2) === 3);

    assert(new Function().name === "anonymous");
    assert(new Function().toString() === "function anonymous() {\n  ???\n}");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
