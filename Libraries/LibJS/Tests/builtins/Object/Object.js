test("basic functionality", () => {
    expect(Object).toHaveLength(1);
    expect(Object.name).toBe("Object");
    expect(Object.prototype.length).toBe(undefined);

    expect(typeof Object()).toBe("object");
    expect(typeof new Object()).toBe("object");
});
