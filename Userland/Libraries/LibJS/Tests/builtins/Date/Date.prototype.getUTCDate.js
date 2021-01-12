test("basic functionality", () => {
    let d = new Date();
    expect(d.getUTCDate()).toBe(d.getUTCDate());
    expect(d.getUTCDate()).not.toBeNaN();
    expect(d.getUTCDate()).toBeGreaterThanOrEqual(1);
    expect(d.getUTCDate()).toBeLessThanOrEqual(31);
});
