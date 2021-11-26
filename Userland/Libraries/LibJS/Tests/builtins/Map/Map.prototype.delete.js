test("basic functionality", () => {
    expect(Map.prototype.delete).toHaveLength(1);

    const map = new Map([
        ["a", 0],
        ["b", 1],
        ["c", 2],
    ]);
    expect(map).toHaveSize(3);
    expect(map.delete("b")).toBeTrue();
    expect(map).toHaveSize(2);
    expect(map.delete("b")).toBeFalse();
    expect(map).toHaveSize(2);
});
