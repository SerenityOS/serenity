test("invalid maxByteLength throws", () => {
    expect(() => {
        new ArrayBuffer(8, { maxByteLength: 4 });
    }).toThrowWithMessage(
        RangeError,
        "Byte length of 8 is not less or equal to maxByteLength 4 for resizable ArrayBuffer"
    );

    expect(() => {
        new ArrayBuffer(8, { maxByteLength: -1 });
    }).toThrowWithMessage(RangeError, "Index must be a positive integer");
});
