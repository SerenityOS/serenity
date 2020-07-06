test("basic functionality", () => {
    expect(Object).toHaveLength(1);
    expect(Object.name).toBe("Object");
    expect(Object.prototype).not.toHaveProperty("length");

    expect(typeof Object()).toBe("object");
    expect(typeof new Object()).toBe("object");
});
