test("basic functionality", () => {
    expect(Number).toHaveLength(1);
    expect(Number.name).toBe("Number");
    expect(Number.prototype).not.toHaveProperty("length");

    expect(typeof Number()).toBe("number");
    expect(typeof new Number()).toBe("object");

    expect(Number()).toBe(0);
    expect(new Number().valueOf()).toBe(0);
    expect(Number("42")).toBe(42);
    expect(new Number("42").valueOf()).toBe(42);
    expect(Number(null)).toBe(0);
    expect(new Number(null).valueOf()).toBe(0);
    expect(Number(true)).toBe(1);
    expect(new Number(true).valueOf()).toBe(1);
    expect(Number("Infinity")).toBe(Infinity);
    expect(new Number("Infinity").valueOf()).toBe(Infinity);
    expect(Number("+Infinity")).toBe(Infinity);
    expect(new Number("+Infinity").valueOf()).toBe(Infinity);
    expect(Number("-Infinity")).toBe(-Infinity);
    expect(new Number("-Infinity").valueOf()).toBe(-Infinity);
    expect(Number(undefined)).toBeNaN();
    expect(new Number(undefined).valueOf()).toBeNaN();
    expect(Number({})).toBeNaN();
    expect(new Number({}).valueOf()).toBeNaN();
    expect(Number({ a: 1 })).toBeNaN();
    expect(new Number({ a: 1 }).valueOf()).toBeNaN();
    expect(Number([1, 2, 3])).toBeNaN();
    expect(new Number([1, 2, 3]).valueOf()).toBeNaN();
    expect(Number("foo")).toBeNaN();
    expect(new Number("foo").valueOf()).toBeNaN();
});
