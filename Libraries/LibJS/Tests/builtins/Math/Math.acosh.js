test("basic functionality", () => {
    expect(Math.acosh).toHaveLength(1);

    expect(Math.acosh(-1)).toBeNaN();
    expect(Math.acosh(0)).toBeNaN();
    expect(Math.acosh(0.5)).toBeNaN();
    expect(Math.acosh(1)).toBeCloseTo(0);
    // FIXME: expect(Math.acosh(2)).toBeCloseTo(1.316957);
});
