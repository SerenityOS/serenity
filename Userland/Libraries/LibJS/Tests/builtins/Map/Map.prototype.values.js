test("length", () => {
    expect(Map.prototype.values.length).toBe(0);
});

test("basic functionality", () => {
    const original = [
        ["a", 0],
        ["b", 1],
        ["c", 2],
    ];
    const a = new Map(original);
    const it = a.values();
    expect(it.next()).toEqual({ value: 0, done: false });
    expect(it.next()).toEqual({ value: 1, done: false });
    expect(it.next()).toEqual({ value: 2, done: false });
    expect(it.next()).toEqual({ value: undefined, done: true });
    expect(it.next()).toEqual({ value: undefined, done: true });
    expect(it.next()).toEqual({ value: undefined, done: true });
});
