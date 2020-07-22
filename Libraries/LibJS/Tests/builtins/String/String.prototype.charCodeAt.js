test("basic functionality", () => {
    expect(String.prototype.charAt).toHaveLength(1);

    var s = "Foobar";
    expect(typeof s).toBe("string");
    expect(s).toHaveLength(6);

    expect(s.charCodeAt(0)).toBe(70);
    expect(s.charCodeAt(1)).toBe(111);
    expect(s.charCodeAt(2)).toBe(111);
    expect(s.charCodeAt(3)).toBe(98);
    expect(s.charCodeAt(4)).toBe(97);
    expect(s.charCodeAt(5)).toBe(114);
    expect(s.charCodeAt(6)).toBe(NaN);
    expect(s.charCodeAt(-1)).toBe(NaN);

    expect(s.charCodeAt()).toBe(70);
    expect(s.charCodeAt(NaN)).toBe(70);
    expect(s.charCodeAt("foo")).toBe(70);
    expect(s.charCodeAt(undefined)).toBe(70);
});
