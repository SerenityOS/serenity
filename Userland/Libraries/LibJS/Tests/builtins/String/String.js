test("constructor properties", () => {
    expect(String).toHaveLength(1);
    expect(String.name).toBe("String");
});

test("typeof", () => {
    expect(typeof String()).toBe("string");
    expect(typeof new String()).toBe("object");
});

test("length", () => {
    expect(new String().length).toBe(0);
    expect(new String("a").length).toBe(1);
    expect(new String("\u180E").length).toBe(1);
    expect(new String("\uDBFF\uDFFF").length).toBe(2);
});
