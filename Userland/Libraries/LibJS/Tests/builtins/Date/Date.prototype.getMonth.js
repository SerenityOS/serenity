test("basic functionality", () => {
    var d = new Date();
    expect(d.getMonth()).toBe(d.getMonth());
    expect(d.getMonth()).not.toBeNaN();
    expect(d.getMonth()).toBeGreaterThanOrEqual(0);
    expect(d.getMonth()).toBeLessThanOrEqual(11);
});
