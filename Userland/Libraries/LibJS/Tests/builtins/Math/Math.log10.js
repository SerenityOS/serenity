test("basic functionality", () => {
    expect(Math.log10).toHaveLength(1);

    expect(Math.log10(2)).toBeCloseTo(0.3010299956639812);
    expect(Math.log10(1)).toBe(0);
    expect(Math.log10(0)).toBe(-Infinity);
    expect(Math.log10(-2)).toBe(NaN);
    expect(Math.log10(100000)).toBe(5);
});
