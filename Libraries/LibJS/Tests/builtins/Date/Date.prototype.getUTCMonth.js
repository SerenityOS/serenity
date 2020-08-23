test("basic functionality", () => {
    var d = new Date();
    expect(d.getUTCMonth()).toBe(d.getUTCMonth());
    expect(d.getUTCMonth()).not.toBeNaN();
    expect(d.getUTCMonth()).toBeGreaterThanOrEqual(0);
    expect(d.getUTCMonth()).toBeLessThanOrEqual(11);
});
