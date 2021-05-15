test("basic functionality", () => {
    expect(Math.tanh).toHaveLength(1);

    expect(Math.tanh(0)).toBe(0);
    expect(Math.tanh(Infinity)).toBe(1);
    expect(Math.tanh(-Infinity)).toBe(-1);
    expect(Math.tanh(1)).toBeCloseTo(0.7615941559557649);
});
