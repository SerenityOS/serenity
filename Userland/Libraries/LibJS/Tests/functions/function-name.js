test("basic functionality", () => {
    expect(function () {}.name).toBe("");

    function bar() {}
    expect(bar.name).toBe("bar");
    expect((bar.name = "baz")).toBe("baz");
    expect(bar.name).toBe("bar");
});

test("function assigned to variable", () => {
    let foo = function () {};
    expect(foo.name).toBe("foo");
    expect((foo.name = "bar")).toBe("bar");
    expect(foo.name).toBe("foo");

    let a, b;
    a = b = function () {};
    expect(a.name).toBe("b");
    expect(b.name).toBe("b");
});

test("functions in array assigned to variable", () => {
    const arr = [function () {}, function () {}, function () {}];
    expect(arr[0].name).toBe("arr");
    expect(arr[1].name).toBe("arr");
    expect(arr[2].name).toBe("arr");
});

test("functions in objects", () => {
    let f;
    let o = { a: function () {} };

    expect(o.a.name).toBe("a");
    f = o.a;
    expect(f.name).toBe("a");
    expect(o.a.name).toBe("a");

    o = { ...o, b: f };
    expect(o.a.name).toBe("a");
    expect(o.b.name).toBe("a");

    o.c = function () {};
    expect(o.c.name).toBe("c");
});

test("names of native functions", () => {
    expect(console.debug.name).toBe("debug");
    expect((console.debug.name = "warn")).toBe("warn");
    expect(console.debug.name).toBe("debug");
});

test("cyclic members should not cause infinite recursion (#3471)", () => {
    let a = [() => 4];
    a[1] = a;
    a = a;
    expect(a[0].name).toBe("a");
});
