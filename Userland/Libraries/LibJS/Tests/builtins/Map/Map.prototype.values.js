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
    // FIXME: This test should be rewritten once we have proper iteration order
    const first = it.next();
    expect(first.done).toBeFalse();
    expect([0, 1, 2].includes(first.value)).toBeTrue();
    const second = it.next();
    expect(second.done).toBeFalse();
    expect([0, 1, 2].includes(second.value)).toBeTrue();
    const third = it.next();
    expect(third.done).toBeFalse();
    expect([0, 1, 2].includes(third.value)).toBeTrue();
    expect(it.next()).toEqual({ value: undefined, done: true });
    expect(it.next()).toEqual({ value: undefined, done: true });
    expect(it.next()).toEqual({ value: undefined, done: true });
});
