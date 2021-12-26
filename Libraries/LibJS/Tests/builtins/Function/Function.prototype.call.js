test("length", () => {
    expect(Function.prototype.call).toHaveLength(1);
});

test("basic functionality", () => {
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

    var foo = new Foo("test");
    expect(foo.foo).toBe("test");
    expect(foo.bar).toBeUndefined();
    expect(foo.baz).toBeUndefined();

    var bar = new Bar("test");
    expect(bar.foo).toBeUndefined();
    expect(bar.bar).toBe("test");
    expect(bar.baz).toBeUndefined();

    var foobar = new FooBar("test");
    expect(foobar.foo).toBe("test");
    expect(foobar.bar).toBe("test");
    expect(foobar.baz).toBeUndefined();

    var foobarbaz = new FooBarBaz("test");
    expect(foobarbaz.foo).toBe("test");
    expect(foobarbaz.bar).toBe("test");
    expect(foobarbaz.baz).toBe("test");

    expect(Math.abs.call(null, -1)).toBe(1);

    var add = (x, y) => x + y;
    expect(add.call(null, 1, 2)).toBe(3);

    var multiply = function (x, y) {
        return x * y;
    };
    expect(multiply.call(null, 3, 4)).toBe(12);

    expect((() => this).call("foo")).toBe(globalThis);
});
