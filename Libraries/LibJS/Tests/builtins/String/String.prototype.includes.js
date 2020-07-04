test("basic functionality", () => {
    expect(String.prototype.includes).toHaveLength(1);

    expect("hello friends".includes("hello")).toBe(true);
    expect("hello friends".includes("hello", 100)).toBe(false);
    expect("hello friends".includes("hello", -10)).toBe(true);
    expect("hello friends".includes("friends", 6)).toBe(true);
    expect("hello friends".includes("hello", 6)).toBe(false);
    expect("hello friends false".includes(false)).toBe(true);
    expect("hello 10 friends".includes(10)).toBe(true);
    expect("hello friends undefined".includes()).toBe(true);
});
