test("basic functionality", () => {
    expect(Math.asinh).toHaveLength(1);

    expect(Math.asinh(0)).toBeCloseTo(0);
    // FIXME: expect(Math.asinh(1)).toBeCloseTo(0.881373);
});
