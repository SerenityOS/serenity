function registerInDifferentScope(registry) {
    const target = {};
    registry.register(target, {});
    eval("");
}

test("basic functionality", () => {
    expect(WeakMap.prototype.set).toHaveLength(2);

    const weakMap = new WeakMap([
        [{ a: 1 }, 1],
        [{ a: 2 }, 2],
        [{ a: 3 }, 3],
        [Symbol("foo"), "bar"],
    ]);
    expect(weakMap.set({ a: 4 }, 4)).toBe(weakMap);
    expect(weakMap.set({ a: 1 }, 2)).toBe(weakMap);
    expect(weakMap.set(Symbol("hello"), "friends")).toBe(weakMap);
});

test("invalid values", () => {
    const weakMap = new WeakMap();
    [-100, Infinity, NaN, "hello", 152n].forEach(value => {
        expect(() => {
            weakMap.set(value, value);
        }).toThrowWithMessage(TypeError, "cannot be held weakly");
    });
});

function setObjectKey(weakMap) {
    expect(weakMap.set({ e: 3 }, 1)).toBe(weakMap);
}

function setSymbolKey(weakMap) {
    expect(weakMap.set(Symbol("foo"), "bar")).toBe(weakMap);
}

test("automatic removal of garbage-collected values", () => {
    const weakMap = new WeakMap();

    setObjectKey(weakMap);
    expect(getWeakMapSize(weakMap)).toBe(1);

    gc();
    expect(getWeakMapSize(weakMap)).toBe(0);

    setSymbolKey(weakMap);
    expect(getWeakMapSize(weakMap)).toBe(1);

    gc();
    expect(getWeakMapSize(weakMap)).toBe(0);
});
