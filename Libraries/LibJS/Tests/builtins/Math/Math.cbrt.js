test("basic functionality", () => {
    expect(Math.cbrt).toHaveLength(1);

    expect(Math.cbrt(NaN)).toBeNaN();
    // FIXME: expect(Math.cbrt(-1)).toBe(-1);
    expect(Math.cbrt(-0)).toBe(-0);
    // FIXME: expect(Math.cbrt(-Infinity)).toBe(-Infinity);
    // FIXME: expect(Math.cbrt(1)).toBe(1);
    // FIXME: expect(Math.cbrt(Infinity)).toBe(Infinity);
    // FIXME: expect(Math.cbrt(null)).toBe(0);
    // FIXME: expect(Math.cbrt(2)).toBeCloseTo(1.259921));
});
