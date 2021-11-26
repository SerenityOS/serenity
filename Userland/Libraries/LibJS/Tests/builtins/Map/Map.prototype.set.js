test("basic functionality", () => {
    expect(Map.prototype.set).toHaveLength(2);

    const map = new Map([
        ["a", 0],
        ["b", 1],
        ["c", 2],
    ]);
    expect(map).toHaveSize(3);
    expect(map.set("d", 3)).toBe(map);
    expect(map).toHaveSize(4);
    expect(map.set("a", -1)).toBe(map);
    expect(map).toHaveSize(4);
});
