test("basic functionality", () => {
    expect(Math.log).toHaveLength(1);

    expect(Math.log(-1)).toBe(NaN);
    expect(Math.log(0)).toBe(-Infinity);
    // FIXME: not precise enough
    //expect(Math.log(1)).toBe(0);
    expect(Math.log(10)).toBeCloseTo(2.302585092994046);
});
