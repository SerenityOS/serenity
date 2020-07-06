test("basic functionality", () => {
    expect(Math.log1p).toHaveLength(1);

    expect(Math.log1p(-2)).toBeNaN();
    expect(Math.log1p(-1)).toBe(-Infinity);
    // FIXME: expect(Math.log1p(0)).toBe(0);
    // FIXME: expect(Math.log1p(1)).toBeCloseTo(0.693147);
});
