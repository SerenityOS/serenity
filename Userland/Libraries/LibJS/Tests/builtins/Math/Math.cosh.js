test("basic functionality", () => {
    expect(Math.cosh).toHaveLength(1);

    expect(Math.cosh(0)).toBe(1);
    expect(Math.cosh(1)).toBeCloseTo(1.5430806348152437);
    expect(Math.cosh(-1)).toBeCloseTo(1.5430806348152437);
});
