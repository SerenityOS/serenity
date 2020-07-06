test("basic functionality", () => {
    expect(String.prototype.indexOf).toHaveLength(1);

    var s = "hello friends";

    expect(s.indexOf("friends")).toBe(6);
    expect(s.indexOf("enemies")).toBe(-1);
});
