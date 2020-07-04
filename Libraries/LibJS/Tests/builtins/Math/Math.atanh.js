test("basic functionality", () => {
    expect(Math.atanh).toHaveLength(1);

    expect(Math.atanh(-2)).toBeNaN();
    expect(Math.atanh(2)).toBeNaN();
    expect(Math.atanh(-1)).toBe(-Infinity);
    // FIXME: expect(Math.atanh(0)).toBe(0);
    expect(Math.atanh(0.5)).toBeCloseTo(0.549306);
    // FIXME: expect(Math.atanh(1)).toBe(Infinity);
});
