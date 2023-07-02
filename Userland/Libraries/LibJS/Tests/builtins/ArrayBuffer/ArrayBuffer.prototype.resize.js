test("basic functionality", () => {
    const resizableBuffer = new ArrayBuffer(8, { maxByteLength: 16 });

    expect(resizableBuffer.resize(8)).toBeUndefined();
    expect(resizableBuffer.resize(0)).toBeUndefined();
    expect(resizableBuffer.resize(16)).toBeUndefined();

    expect(() => {
        resizableBuffer.resize(-1);
    }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    expect(() => {
        resizableBuffer.resize(17);
    }).toThrowWithMessage(
        RangeError,
        "Byte length of 17 is not less or equal to maxByteLength 16 for resizable ArrayBuffer"
    );
});

test("fixed length buffer resize should throw", () => {
    const staticBuffer = new ArrayBuffer(8);

    expect(() => {
        staticBuffer.resize(0);
    }).toThrowWithMessage(TypeError, "Tried to resize an ArrayBuffer that is fixed length");
});
