load("test-common.js");

try {
    assert(Reflect.apply.length === 3);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.apply(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.apply() must be a function"
        });

        assertThrowsError(() => {
            Reflect.apply(() => {}, undefined, value);
        }, {
            error: TypeError,
            message: "Arguments list must be an object"
        });
    });

    assert(Reflect.apply(String.prototype.charAt, "foo", [0]) === "f");
    assert(Reflect.apply(Array.prototype.indexOf, ["hello", 123, "foo", "bar"], ["foo"]) === 2);

    function Foo(foo) {
        this.foo = foo;
    }

    var o = {};
    assert(o.foo === undefined);
    assert(Reflect.apply(Foo, o, ["bar"]) === undefined);
    assert(o.foo === "bar");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
