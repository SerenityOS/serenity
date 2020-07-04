test("constructor properties", () => {
    expect(String).toHaveLength(1);
    expect(String.name).toBe("String");
});

test("typeof", () => {
    expect(typeof String()).toBe("string");
    expect(typeof new String()).toBe("object");
});
