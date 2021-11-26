test("basic functionality", () => {
    expect(WeakMap.prototype.get).toHaveLength(1);

    var original = [
        [{ a: 1 }, 1],
        [{ a: 2 }, 2],
        [{ a: 3 }, 3],
    ];
    const weakMap = new WeakMap(original);
    expect(weakMap.get(original[0][0])).toBe(original[0][1]);
    expect(weakMap.get(null)).toBe(undefined);
});
