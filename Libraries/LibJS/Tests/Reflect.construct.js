load("test-common.js");

try {
    assert(Reflect.construct.length === 2);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.construct(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.construct() must be a function"
        });

        assertThrowsError(() => {
            Reflect.construct(() => {}, value);
        }, {
            error: TypeError,
            message: "Arguments list must be an object"
        });

        assertThrowsError(() => {
            Reflect.construct(() => {}, [], value);
        }, {
            error: TypeError,
            message: "Optional third argument of Reflect.construct() must be a constructor"
        });
    });

    var a = Reflect.construct(Array, [5]);
    assert(a instanceof Array);
    assert(a.length === 5);

    var s = Reflect.construct(String, [123]);
    assert(s instanceof String);
    assert(s.length === 3);
    assert(s.toString() === "123");

    function Foo() {
        this.name = "foo";
    }
    
    function Bar() {
        this.name = "bar";
    }

    var o = Reflect.construct(Foo, [], Bar);
    assert(o.name === "foo");
    assert(o instanceof Foo === false);
    assert(o instanceof Bar === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
