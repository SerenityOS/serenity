test("basic functionality", () => {
    const resizableBuffer = new ArrayBuffer(8, { maxByteLength: 16 });

    expect(resizableBuffer.resize(8)).toBeUndefined();
    expect(resizableBuffer.resize(0)).toBeUndefined();
    expect(resizableBuffer.resize(16)).toBeUndefined();

    expect(() => {
        resizableBuffer.resize(-1);
    }).toThrowWithMessage(
        RangeError,
        "Invalid byte length for ArrayBuffer -1 when the max byte length is 16"
    );
    expect(() => {
        resizableBuffer.resize(17);
    }).toThrowWithMessage(
        RangeError,
        "Invalid byte length for ArrayBuffer 17 when the max byte length is 16"
    );
});

test("un-resizable buffer should throw", () => {
    const staticBuffer = new ArrayBuffer(8);

    expect(() => {
        staticBuffer.resize(0);
    }).toThrowWithMessage(TypeError, "Tried to resize an ArrayBuffer that is not resizable");
});
