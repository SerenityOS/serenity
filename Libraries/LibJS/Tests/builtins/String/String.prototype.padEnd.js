test("basic functionality", () => {
    expect(String.prototype.padEnd).toHaveLength(1);

    var s = "foo";
    expect(s.padEnd(-1)).toBe("foo");
    expect(s.padEnd(0)).toBe("foo");
    expect(s.padEnd(3)).toBe("foo");
    expect(s.padEnd(5)).toBe("foo  ");
    expect(s.padEnd(10)).toBe("foo       ");
    expect(s.padEnd("5")).toBe("foo  ");
    expect(s.padEnd([[["5"]]])).toBe("foo  ");
    expect(s.padEnd(2, "+")).toBe("foo");
    expect(s.padEnd(5, "+")).toBe("foo++");
    expect(s.padEnd(5, 1)).toBe("foo11");
    expect(s.padEnd(10, null)).toBe("foonullnul");
    expect(s.padEnd(10, "bar")).toBe("foobarbarb");
    expect(s.padEnd(10, "123456789")).toBe("foo1234567");
});
