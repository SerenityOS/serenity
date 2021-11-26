test("length is 1", () => {
    expect(Set.prototype.has).toHaveLength(1);
});

test("basic functionality", () => {
    var set = new Set(["hello", "friends", 1, 2, false]);

    expect(new Set().has()).toBeFalse();
    expect(new Set([undefined]).has()).toBeTrue();
    expect(set.has("hello")).toBeTrue();
    expect(set.has(1)).toBeTrue();
    expect(set.has("serenity")).toBeFalse();
});
