test("basic functionality", () => {
    const s1 = Symbol("foo");
    const s2 = Symbol("foo");

    expect(s1).not.toBe(s2);
    expect(s1.description).toBe("foo");
    expect(s2.description).toBe("foo");

    s1.description = "bar";
    expect(s1.description).toBe("foo");

    expect(typeof s1).toBe("symbol");
});

test("constructing symbol from symbol is an error", () => {
    expect(() => {
        Symbol(Symbol("foo"));
    }).toThrowWithMessage(TypeError, "Cannot convert symbol to string");
});
