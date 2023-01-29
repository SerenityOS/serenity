test("basic functionality", () => {
    expect(Set.prototype.isDisjointFrom).toHaveLength(1);

    const set1 = new Set(["a", "b"]);
    const set2 = new Set(["c"]);
    expect(set1.isDisjointFrom(set1)).toBeFalse();
    expect(set1.isDisjointFrom(set2)).toBeTrue();
    expect(set2.isDisjointFrom(set1)).toBeTrue();
});
