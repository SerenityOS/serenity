test("basic functionality", () => {
    expect(WeakMap.prototype.set).toHaveLength(2);

    const weakMap = new WeakMap([
        [{ a: 1 }, 1],
        [{ a: 2 }, 2],
        [{ a: 3 }, 3],
    ]);
    expect(weakMap.set({ a: 4 }, 4)).toBe(weakMap);
    expect(weakMap.set({ a: 1 }, 2)).toBe(weakMap);
});

test("invalid values", () => {
    const weakMap = new WeakMap();
    [-100, Infinity, NaN, "hello", 152n].forEach(value => {
        expect(() => {
            weakMap.set(value, value);
        }).toThrowWithMessage(TypeError, "is not an object");
    });
});

test("automatic removal of garbage-collected values", () => {
    const weakMap = new WeakMap();
    const key = { e: 3 };

    expect(weakMap.set(key, 1)).toBe(weakMap);
    expect(getWeakMapSize(weakMap)).toBe(1);

    markAsGarbage("key");
    gc();

    expect(getWeakMapSize(weakMap)).toBe(0);
});
