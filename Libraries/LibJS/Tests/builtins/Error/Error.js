test("basic functionality", () => {
    expect(Error).toHaveLength(1);
    expect(Error.name).toBe("Error");
});

test("name", () => {
    [Error(), Error(undefined), Error("test"), Error(42), Error(null)].forEach(error => {
        expect(error.name).toBe("Error");
    });
});

test("message", () => {
    expect(Error().message).toBe("");
    expect(Error(undefined).message).toBe("");
    expect(Error("test").message).toBe("test");
    expect(Error(42).message).toBe("42");
    expect(Error(null).message).toBe("null");
});
