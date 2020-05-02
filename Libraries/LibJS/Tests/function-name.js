load("test-common.js");

try {
    assert((function () { }).name === "");

    var foo = function () { }
    assert(foo.name === "foo");
    assert((foo.name = "bar") === "bar");
    assert(foo.name === "foo");

    var a, b;
    a = b = function () { }
    assert(a.name === "b");
    assert(b.name === "b");

    var arr = [
        function () { },
        function () { },
        function () { }
    ];
    assert(arr[0].name === "arr");
    assert(arr[1].name === "arr");
    assert(arr[2].name === "arr");

    var f;
    var o = { a: function () { } };
    assert(o.a.name === "a");
    f = o.a;
    assert(f.name === "a");
    assert(o.a.name === "a");
    o = { ...o, b: f };
    assert(o.a.name === "a");
    assert(o.b.name === "a");
    o.c = function () { };
    assert(o.c.name === "c");

    function bar() { }
    assert(bar.name === "bar");
    assert((bar.name = "baz") === "baz");
    assert(bar.name === "bar");

    assert(console.log.name === "log");
    assert((console.log.name = "warn") === "warn");
    assert(console.log.name === "log");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
