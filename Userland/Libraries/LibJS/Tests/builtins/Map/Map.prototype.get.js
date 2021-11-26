test("basic functionality", () => {
    expect(Map.prototype.get).toHaveLength(1);

    const map = new Map([
        ["a", 0],
        ["b", 1],
        ["c", 2],
    ]);
    expect(map.get("a")).toBe(0);
    expect(map.get("d")).toBe(undefined);
});
