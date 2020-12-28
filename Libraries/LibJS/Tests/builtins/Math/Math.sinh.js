test("basic functionality", () => {
    expect(Math.sinh).toHaveLength(1);

    expect(Math.sinh(0)).toBe(0);
    expect(Math.sinh(1)).toBeCloseTo(1.1752011936438014);
});
