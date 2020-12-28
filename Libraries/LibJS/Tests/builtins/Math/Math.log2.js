test("basic functionality", () => {
    expect(Math.log2).toHaveLength(1);

    expect(Math.log2(3)).toBeCloseTo(1.584962500721156);
    // FIXME: not precise enough
    // expect(Math.log2(2)).toBe(1);
    // expect(Math.log2(1)).toBe(0);
    expect(Math.log2(0)).toBe(-Infinity);
    expect(Math.log2(-2)).toBe(NaN);
    // expect(Math.log2(1024)).toBe(10);
});
