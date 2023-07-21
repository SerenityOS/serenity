test("basic functionality", () => {
    expect(WeakSet.prototype.add).toHaveLength(1);

    const weakSet = new WeakSet([{ a: 1 }, { a: 2 }, { a: 3 }, Symbol("foo")]);
    expect(weakSet.add({ a: 4 })).toBe(weakSet);
    expect(weakSet.add({ a: 1 })).toBe(weakSet);
    expect(weakSet.add(Symbol("bar"))).toBe(weakSet);
});

test("invalid values", () => {
    const weakSet = new WeakSet();
    [-100, Infinity, NaN, "hello", 152n].forEach(value => {
        expect(() => {
            weakSet.add(value);
        }).toThrowWithMessage(TypeError, "cannot be held weakly");
    });
});

function addObjectItem(weakSet) {
    weakSet.add({ a: 1 });
}

function addSymbolItem(weakSet) {
    weakSet.add(Symbol("foo"));
}

test("automatic removal of garbage-collected values", () => {
    const weakSet = new WeakSet();

    addObjectItem(weakSet);
    expect(getWeakSetSize(weakSet)).toBe(1);

    gc();
    expect(getWeakSetSize(weakSet)).toBe(0);

    addSymbolItem(weakSet);
    expect(getWeakSetSize(weakSet)).toBe(1);

    gc();
    expect(getWeakSetSize(weakSet)).toBe(0);
});
