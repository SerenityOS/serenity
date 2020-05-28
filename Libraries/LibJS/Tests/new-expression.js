load("test-common.js");

try {
    function Foo() { this.x = 1; }

    let foo = new Foo();
    assert(foo.x === 1);

    foo = new Foo
    assert(foo.x === 1);

    foo = new
    Foo
    ()
    assert(foo.x === 1);

    foo = new Foo + 2
    assert(foo === "[object Object]2");

    let a = {
        b: function() {
            this.x = 2;
        },
    };

    foo = new a.b();
    assert(foo.x === 2);

    foo = new a.b;
    assert(foo.x === 2);

    foo = new
    a.b();
    assert(foo.x === 2);

    function funcGetter() {
        return function(a, b) {
            this.x = a + b;
        };
    };

    foo = new funcGetter()(1, 5);
    assert(foo === undefined);

    foo = new (funcGetter())(1, 5);
    assert(foo.x === 6);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
