test("basic functionality", () => {
    expect(RegExp.prototype.toString).toHaveLength(0);

    expect(/test/g.toString()).toBe("/test/g");
});
