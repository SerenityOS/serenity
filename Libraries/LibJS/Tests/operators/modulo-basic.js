test("basic functionality", () => {
    expect(10 % 3).toBe(1);
    expect(10.5 % 2.5).toBe(0.5);
    expect(-0.99 % 0.99).toBe(-0);

    // Examples from MDN:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Arithmetic_Operators
    expect(12 % 5).toBe(2);
    expect(-1 % 2).toBe(-1);
    expect(1 % -2).toBe(1);
    expect(1 % 2).toBe(1);
    expect(2 % 3).toBe(2);
    expect(-4 % 2).toBe(-0);
    expect(5.5 % 2).toBe(1.5);
    expect(NaN % 2).toBeNaN();
});
