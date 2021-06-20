test("length is 1", () => {
    expect(Number).toHaveLength(1);
});

test("constructor without new", () => {
    expect(typeof Number()).toBe("number");
    expect(typeof new Number()).toBe("object");

    expect(Number()).toBe(0);
    expect(Number(123)).toBe(123);
    expect(Number(-123)).toBe(-123);
    expect(Number(123n)).toBe(123);
    expect(Number(-123n)).toBe(-123);
    expect(Number("42")).toBe(42);
    expect(Number(null)).toBe(0);
    expect(Number(true)).toBe(1);
    expect(Number("Infinity")).toBe(Infinity);
    expect(Number("+Infinity")).toBe(Infinity);
    expect(Number("-Infinity")).toBe(-Infinity);
    expect(Number(undefined)).toBeNaN();
    expect(Number({})).toBeNaN();
    expect(Number({ a: 1 })).toBeNaN();
    expect(Number([1, 2, 3])).toBeNaN();
    expect(Number("foo")).toBeNaN();
});

test("constructor with new", () => {
    expect(typeof new Number()).toBe("object");

    expect(new Number().valueOf()).toBe(0);
    expect(new Number(123).valueOf()).toBe(123);
    expect(new Number(-123).valueOf()).toBe(-123);
    expect(new Number(123n).valueOf()).toBe(123);
    expect(new Number(-123n).valueOf()).toBe(-123);
    expect(new Number("42").valueOf()).toBe(42);
    expect(new Number(null).valueOf()).toBe(0);
    expect(new Number(true).valueOf()).toBe(1);
    expect(new Number("Infinity").valueOf()).toBe(Infinity);
    expect(new Number("+Infinity").valueOf()).toBe(Infinity);
    expect(new Number("-Infinity").valueOf()).toBe(-Infinity);
    expect(new Number(undefined).valueOf()).toBeNaN();
    expect(new Number({}).valueOf()).toBeNaN();
    expect(new Number({ a: 1 }).valueOf()).toBeNaN();
    expect(new Number([1, 2, 3]).valueOf()).toBeNaN();
    expect(new Number("foo").valueOf()).toBeNaN();
});
