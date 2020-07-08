test("plain property", () => {
    let o = { foo: "bar" };

    expect(o).toHaveConfigurableProperty("foo");
    expect(o).toHaveEnumerableProperty("foo");
    expect(o).toHaveWritableProperty("foo");
    expect(o).toHaveValueProperty("foo", "bar");
    expect(o).not.toHaveGetterProperty("foo");
    expect(o).not.toHaveSetterProperty("foo");
});

test("symbol property", () => {
    let s = Symbol("foo");
    let o = { [s]: "bar" };

    expect(o).toHaveConfigurableProperty(s);
    expect(o).toHaveEnumerableProperty(s);
    expect(o).toHaveWritableProperty(s);
    expect(o).toHaveValueProperty(s, "bar");
    expect(o).not.toHaveGetterProperty(s);
    expect(o).not.toHaveSetterProperty(s);
});

test("getter property", () => {
    let o = { get foo() {} };

    expect(o).toHaveConfigurableProperty("foo");
    expect(o).toHaveEnumerableProperty("foo");
    expect(o).not.toHaveWritableProperty("foo");
    expect(o).not.toHaveValueProperty("foo");
    expect(o).toHaveGetterProperty("foo");
    expect(o).not.toHaveSetterProperty("foo");
});

test("setter property", () => {
    let o = { set foo(_) {} };

    expect(o).toHaveConfigurableProperty("foo");
    expect(o).toHaveEnumerableProperty("foo");
    expect(o).not.toHaveWritableProperty("foo");
    expect(o).not.toHaveValueProperty("foo");
    expect(o).not.toHaveGetterProperty("foo");
    expect(o).toHaveSetterProperty("foo");
});

test("defined property", () => {
    let o = {};

    Object.defineProperty(o, "foo", {
        enumerable: false,
        writable: true,
        value: 10,
    });

    expect(o).not.toHaveConfigurableProperty("foo");
    expect(o).not.toHaveEnumerableProperty("foo");
    expect(o).toHaveWritableProperty("foo");
    expect(o).toHaveValueProperty("foo", 10);
    expect(o).not.toHaveGetterProperty("foo");
    expect(o).not.toHaveSetterProperty("foo");
});
