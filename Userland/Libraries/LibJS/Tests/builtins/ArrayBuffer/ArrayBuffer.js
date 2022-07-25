test("basic functionality", () => {
    expect(ArrayBuffer).toHaveLength(1);
    expect(ArrayBuffer.name).toBe("ArrayBuffer");
    expect(ArrayBuffer.prototype.constructor).toBe(ArrayBuffer);
    expect(new ArrayBuffer()).toBeInstanceOf(ArrayBuffer);
    expect(typeof new ArrayBuffer()).toBe("object");
});

test("ArrayBuffer constructor must be invoked with 'new'", () => {
    expect(() => {
        ArrayBuffer();
    }).toThrowWithMessage(TypeError, "ArrayBuffer constructor must be called with 'new'");
});
