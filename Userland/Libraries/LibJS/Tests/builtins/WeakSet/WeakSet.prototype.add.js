test("basic functionality", () => {
    expect(WeakSet.prototype.add).toHaveLength(1);

    const weakSet = new WeakSet([{ a: 1 }, { a: 2 }, { a: 3 }]);
    expect(weakSet.add({ a: 4 })).toBe(weakSet);
    expect(weakSet.add({ a: 1 })).toBe(weakSet);
});

test("invalid values", () => {
    const weakSet = new WeakSet();
    [-100, Infinity, NaN, "hello", 152n].forEach(value => {
        expect(() => {
            weakSet.add(value);
        }).toThrowWithMessage(TypeError, "is not an object");
    });
});

test("automatic removal of garbage-collected values", () => {
    const weakSet = new WeakSet();
    const item = { a: 1 };

    expect(weakSet.add(item)).toBe(weakSet);
    expect(getWeakSetSize(weakSet)).toBe(1);

    markAsGarbage("item");
    gc();

    expect(getWeakSetSize(weakSet)).toBe(0);
});
