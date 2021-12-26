load("test-common.js");

try {
    function Foo(arg) {
        this.foo = arg;
    }
    function Bar(arg) {
        this.bar = arg;
    }
    function FooBar(arg) {
        Foo.call(this, arg);
        Bar.call(this, arg);
    }
    function FooBarBaz(arg) {
        Foo.call(this, arg);
        Bar.call(this, arg);
        this.baz = arg;
    }

    assert(Function.prototype.call.length === 1);

    var foo = new Foo("test");
    assert(foo.foo === "test");
    assert(foo.bar === undefined);
    assert(foo.baz === undefined);

    var bar = new Bar("test");
    assert(bar.foo === undefined);
    assert(bar.bar === "test");
    assert(bar.baz === undefined);

    var foobar = new FooBar("test");
    assert(foobar.foo === "test");
    assert(foobar.bar === "test");
    assert(foobar.baz === undefined);

    var foobarbaz = new FooBarBaz("test");
    assert(foobarbaz.foo === "test");
    assert(foobarbaz.bar === "test");
    assert(foobarbaz.baz === "test");

    assert(Math.abs.call(null, -1) === 1);

    var add = (x, y) => x + y;
    assert(add.call(null, 1, 2) === 3);

    var multiply = function (x, y) { return x * y; };
    assert(multiply.call(null, 3, 4) === 12);

    assert((() => this).call("foo") === globalThis);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
