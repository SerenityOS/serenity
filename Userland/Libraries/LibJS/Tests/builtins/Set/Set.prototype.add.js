test("basic functionality", () => {
    expect(Set.prototype.add).toHaveLength(1);

    const set = new Set(["a", "b", "c"]);
    expect(set).toHaveSize(3);
    expect(set.add("d")).toBe(set);
    expect(set).toHaveSize(4);
    expect(set.add("a")).toBe(set);
    expect(set).toHaveSize(4);
});
