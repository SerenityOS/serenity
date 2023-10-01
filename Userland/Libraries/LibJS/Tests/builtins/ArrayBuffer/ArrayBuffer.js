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

test("ArrayBuffer size limit", () => {
    expect(() => {
        new ArrayBuffer(2 ** 53);
    }).toThrowWithMessage(RangeError, "Invalid array buffer length");
});

test("ArrayBuffer is detached after transfer()", () => {
    const ab = new ArrayBuffer(16);
    expect(ab.detached).toBe(false);
    const transferred = ab.transfer();
    expect(ab.detached).toBe(true);
    expect(transferred.detached).toBe(false);
});
