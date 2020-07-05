describe("normal functionality", () => {
  test("non-configurable property", () => {
    let o = {};
    Object.defineProperty(o, "foo", { value: 1, writable: false, enumerable: false });

    expect(o.foo).toBe(1);
    o.foo = 2;
    expect(o.foo).toBe(1);

    expect(o).not.toHaveConfigurableProperty("foo");
    expect(o).not.toHaveEnumerableProperty("foo");
    expect(o).not.toHaveWritableProperty("foo");
    expect(o).toHaveValueProperty("foo", 1);
  });

  test("array index getter", () => {
    let o = {};
    Object.defineProperty(o, 2, {
      get() {
        return 10;
      },
    });
    expect(o[2]).toBe(10);
  });

  test("configurable property", () => {
    let o = {};
    Object.defineProperty(o, "foo", { value: "hi", writable: true, enumerable: true });

    expect(o.foo).toBe("hi");
    o.foo = "ho";
    expect(o.foo).toBe("ho");

    expect(o).not.toHaveConfigurableProperty("foo");
    expect(o).toHaveEnumerableProperty("foo");
    expect(o).toHaveWritableProperty("foo");
    expect(o).toHaveValueProperty("foo", "ho");
  });

  test("reconfigure configurable property", () => {
    let o = {};
    Object.defineProperty(o, "foo", { value: 9, configurable: true, writable: false });
    Object.defineProperty(o, "foo", { configurable: true, writable: true });

    expect(o).toHaveConfigurableProperty("foo");
    expect(o).toHaveWritableProperty("foo");
    expect(o).not.toHaveEnumerableProperty("foo");
    expect(o).toHaveValueProperty("foo", 9);
  });

  test("define accessor", () => {
    let o = {};

    Object.defineProperty(o, "foo", {
      configurable: true,
      get() {
        return o.secret_foo + 1;
      },
      set(value) {
        this.secret_foo = value + 1;
      },
    });

    o.foo = 10;
    expect(o.foo).toBe(12);
    o.foo = 20;
    expect(o.foo).toBe(22);

    Object.defineProperty(o, "foo", { configurable: true, value: 4 });

    expect(o.foo).toBe(4);
    expect((o.foo = 5)).toBe(5);
    expect((o.foo = 4)).toBe(4);
  });
});

describe("errors", () => {
  test("redefine non-configurable property", () => {
    let o = {};
    Object.defineProperty(o, "foo", { value: 1, writable: true, enumerable: true });

    expect(() => {
      Object.defineProperty(o, "foo", { value: 2, writable: false, enumerable: true });
    }).toThrowWithMessage(TypeError, "Cannot change attributes of non-configurable property 'foo'");
  });

  test("cannot define 'value' and 'get' in the same descriptor", () => {
    let o = {};

    expect(() => {
      Object.defineProperty(o, "a", {
        get() {},
        value: 9,
      });
    }).toThrowWithMessage(
      TypeError,
      "Accessor property descriptor cannot specify a value or writable key"
    );
  });

  test("cannot define 'value' and 'set' in the same descriptor", () => {
    let o = {};

    expect(() => {
      Object.defineProperty(o, "a", {
        set() {},
        writable: true,
      });
    }).toThrowWithMessage(
      TypeError,
      "Accessor property descriptor cannot specify a value or writable key"
    );
  });

  test("redefine non-configurable accessor", () => {
    let o = {};

    Object.defineProperty(o, "foo", {
      configurable: false,
      get() {
        return this.secret_foo + 2;
      },
      set(value) {
        o.secret_foo = value + 2;
      },
    });

    expect(() => {
      Object.defineProperty(o, "foo", {
        configurable: false,
        get() {
          return this.secret_foo + 2;
        },
      });
    }).toThrowWithMessage(TypeError, "Cannot change attributes of non-configurable property 'foo'");
  });
});
